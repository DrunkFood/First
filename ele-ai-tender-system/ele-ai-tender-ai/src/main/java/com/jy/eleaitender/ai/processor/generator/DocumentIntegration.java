package com.jy.eleaitender.ai.processor.generator;

import com.jy.eleaitender.common.client.InternalFileServiceClient;
import com.jy.eleaitender.common.entity.ai.AiTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

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

        // 调用File服务生成Word文档
        Long generatedFileId = fileServiceClient.generateDocument(templateFileId, params, projectName + ".docx");

        log.info("文档集成完成: taskId={}, generatedFileId={}", task.getId(), generatedFileId);
        return String.valueOf(generatedFileId);
    }

}
