package com.jy.eleaitender.ai.processor.generator;

import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.client.InternalFileServiceClient;
import com.jy.eleaitender.common.dto.FillData;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.MapUtils.getLong;
import static org.apache.commons.collections4.MapUtils.getString;

/**
 * 文档集成器
 * 通过File服务的poi-tl模板引擎根据项目数据生成Word文档
 */
@Slf4j
@Component
public class DocumentIntegration {

    @Autowired
    private GenerateResultParser resultParser;

    @Autowired
    private InternalFileServiceClient fileServiceClient;

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    /**
     * 执行文档集成
     *
     * @param task AI任务（requestParams包含项目数据、templateFileId、projectName）
     * @return 生成的文件ID字符串
     */
    public String integration(AiTask task) {
        log.info("开始文档集成: taskId={}", task.getId());

        Map<String, Object> params = resultParser.parseParams(task.getRequestParams());

        Long templateFileId = getLong(params, "templateFileId");
        String projectName = getString(params, "projectName");

        // TODO 通过AI模型匹配占位符和数据key，生成符合Word模板占位符名称的填充数据，列表数据无法正确匹配
        //WordStructureVO fileStructure = fileServiceClient.getFileStructure(templateFileId);
        //Map<String, Object> data = matchPlaceholders(fileStructure.getPlaceholders(), fillData, task);

        // 提取 FillData 列表
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fillDataRaw = (List<Map<String, Object>>) params.get("fillDataList");
        List<FillData> fillDataList = deserializeFillDataList(fillDataRaw);

        // 调用File服务生成Word文档（新接口：List<FillData>）
        Long generatedFileId = fileServiceClient.generateDocument(templateFileId, fillDataList, projectName + ".docx");

        log.info("文档集成完成: taskId={}, generatedFileId={}", task.getId(), generatedFileId);
        return String.valueOf(generatedFileId);
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 将 JSON 反序列化后的 Map 列表转为 FillData 列表
     * Jackson 反序列化 List<FillData> 时 value 会变成 LinkedHashMap，需按 type 转为具体类型
     */
    private List<FillData> deserializeFillDataList(List<Map<String, Object>> rawList) {
        if (rawList == null) return List.of();
        return rawList.stream().map(raw -> {
            try {
                FillData fd = MAPPER.convertValue(raw, FillData.class);
                if (fd.getValue() != null && fd.getType() != null) {
                    fd.setValue(switch (fd.getType()) {
                        case TABLE -> MAPPER.convertValue(fd.getValue(), com.jy.eleaitender.common.dto.TableData.class);
                        case IMAGE -> MAPPER.convertValue(fd.getValue(), com.jy.eleaitender.common.dto.ImageData.class);
                        case TEXT, MARKDOWN -> fd.getValue() instanceof String s ? s : String.valueOf(fd.getValue());
                    });
                }
                return fd;
            } catch (Exception e) {
                log.warn("FillData 反序列化失败: {}", raw, e);
                return null;
            }
        }).filter(java.util.Objects::nonNull).toList();
    }

    /**
     * 通过AI模型将Word模板占位符与数据字段进行语义匹配
     * 解决占位符名（如"项目名称"）与数据key（如"projectName"）不匹配的问题
     *
     * @param targetPlaceholders Word模板占位符列表（{{变量名}}格式）
     * @param sourceData         源数据（key为英文字段名）
     * @param task               AI任务（用于AI调用记录）
     * @return 匹配后的数据，key为模板占位符名，value为对应数据值
     */
    private Map<String, Object> matchPlaceholders(List<String> targetPlaceholders,
                                                  Map<String, Object> sourceData,
                                                  AiTask task) {
        if (targetPlaceholders == null || targetPlaceholders.isEmpty() || sourceData.isEmpty()) {
            log.warn("占位符或数据为空，跳过AI匹配");
            return sourceData;
        }

        try {
            // 提取占位符名称（去掉{{、}}、#等poi-tl标记）
            List<String> placeholderNames = targetPlaceholders.stream()
                    .map(p -> p.replace("{{", "").replace("}}", "").replace("#", "").trim())
                    .distinct()
                    .toList();

            // 构建数据字段描述，附带值预览帮助AI理解语义
            String dataFieldsDesc = sourceData.entrySet().stream()
                    .map(e -> formatDataField(e.getKey(), e.getValue()))
                    .collect(Collectors.joining("\n"));

            String placeholdersStr = String.join(", ", placeholderNames);
            String userPrompt = PromptBuilder.buildPlaceholderMatch(placeholdersStr, dataFieldsDesc);

            // 路由到合适的模型
            ChatClient client = modelRouter.route(AiTaskType.DOCUMENT_INTEGRATION);

            // 同步调用并记录响应
            String aiOutput = aiCallRecorder.callAndRecord(client, SystemPromptTemplates.PLACEHOLDER_MATCH,
                    userPrompt, "OPTIMIZATION", task.getId(), task.getCreateId(), task.getFileIdList());

            // 解析AI返回的映射JSON
            String mappingJson = resultParser.extractJson(aiOutput);
            Map<String, String> mapping = resultParser.parseParams(mappingJson)
                    .entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.valueOf(e.getValue())
                    ));

            log.info("AI占位符匹配结果: {}", mapping);

            // 基于映射重组数据：key使用模板占位符名，value取源数据对应值
            Map<String, Object> mappedData = new LinkedHashMap<>(sourceData);
            for (String placeholder : placeholderNames) {
                String sourceKey = mapping.getOrDefault(placeholder, "");
                if (!sourceKey.isEmpty() && sourceData.containsKey(sourceKey)) {
                    mappedData.put(placeholder, sourceData.get(sourceKey));
                }
            }

            return mappedData;
        } catch (Exception e) {
            log.error("AI占位符匹配失败，回退到原始数据: taskId={}", task.getId(), e);
            return sourceData;
        }
    }

    private String formatDataField(String key, Object value) {
        if (value instanceof List) {
            return key + ": [列表数据]";
        }
        if (value == null) {
            return key + ": (空)";
        }
        String str = value.toString();
        String preview = str.length() > 60 ? str.substring(0, 60) + "..." : str;
        return key + ": " + preview;
    }
}
