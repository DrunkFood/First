package com.jy.eleaitender.ai.processor.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.prompt.PromptBuilder;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.dto.ai.OutlineResult;
import com.jy.eleaitender.common.dto.ai.RequirementGenerateParams;
import com.jy.eleaitender.common.dto.ai.RequirementOutline;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ProjectType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 需求生成器（Agent三步式编排）
 * <p>
 * Step 1: 生成大纲 → AI输出JSON大纲结构
 * Step 2: 分章生成 → 并行AI调用，每章独立生成详细内容
 * Step 3: 审查修订 → AI输出修订列表，代码层执行精确替换
 */
@Slf4j
@Component
public class RequirementGenerator {

    /**
     * 修订原文最小字符数，过短的原文容易误替换
     */
    private static final int MIN_REVISION_ORIGINAL_LENGTH = 20;

    /**
     * 分章并行生成时，每章交错启动的间隔（毫秒），避免瞬间并发触发API速率限制
     */
    private static final long CHAPTER_STAGGER_INTERVAL_MS = 2000L;

    @Autowired
    private ModelRouter modelRouter;

    @Autowired
    private GenerateResultParser resultParser;

    @Autowired
    private AiCallRecorder aiCallRecorder;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 虚拟线程执行器（JDK21），类级别共享
     */
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 执行需求生成（三步式Agent编排）
     *
     * @param task AI任务（requestParams包含项目信息）
     * @return JSON结果字符串 {"content": "Markdown需求内容"}
     */
    public String generate(AiTask task) {
        log.info("开始需求生成(Agent三步式): taskId={}", task.getId());

        RequirementGenerateParams params = resultParser.parseParams(task.getRequestParams(), RequirementGenerateParams.class);
        ChatClient client = modelRouter.route(AiTaskType.REQUIREMENT_GENERATE);

        // Step 1: 生成大纲（参考文件内容在此步骤使用）
        OutlineResult outline = generateOutline(params, client, task);
        log.info("Step1大纲生成完成: taskId={}, 章节数={}", task.getId(), outline.getChapters().size());

        // Step 2: 分章并行生成（不传fileIdList，避免参考文件内容重复注入）
        List<String> chapterContents = generateChapters(outline, client, task);
        log.info("Step2分章生成完成: taskId={}, 成功章节数={}", task.getId(), chapterContents.size());

        // Step 3: 拼接全文
        String fullContent = assembleFullContent(outline, chapterContents);
        log.info("全文拼接完成: taskId={}, 总字符数={}", task.getId(), fullContent.length());

        // Step 4: 审查与局部修订（不传fileIdList，减少token消耗）
        String finalContent = reviewAndRefine(fullContent, params, client, task);
        log.info("需求生成完成: taskId={}, 最终字符数={}", task.getId(), finalContent.length());

        return resultParser.toJsonResult("content", finalContent);
    }

    // ==================== Step 1: 生成大纲 ====================

    private OutlineResult generateOutline(RequirementGenerateParams params,
                                          ChatClient client, AiTask task) {
        String userPrompt = PromptBuilder.buildOutline(
                params.getRequirementName(),
                params.getProjectType(),
                params.getProjectCategory(),
                params.getBudget(),
                params.getDescription());

        String aiOutput = aiCallRecorder.callAndRecord(
                client, SystemPromptTemplates.REQUIREMENT_OUTLINE_GENERATE,
                userPrompt, "GENERATION", task.getId(), task.getCreateId(), task.getFileIdList());

        // 解析大纲JSON
        String json = resultParser.extractJson(aiOutput);
        OutlineResult outline = resultParser.parseParams(json, OutlineResult.class);
        if (outline == null || outline.getChapters() == null || outline.getChapters().isEmpty()) {
            log.warn("大纲解析失败或为空，使用降级模板: taskId={}", task.getId());
            outline = buildFallbackOutline(params);
        }
        return outline;
    }

    /**
     * 降级大纲：大纲生成失败时使用预设章节模板
     */
    private OutlineResult buildFallbackOutline(RequirementGenerateParams params) {
        OutlineResult result = new OutlineResult();
        String desc = params.getDescription();
        // projectOverview 设计用途是50-100字摘要，过长时截断
        if (desc != null && desc.length() > 100) {
            int end = desc.indexOf('。', 80);
            result.setProjectOverview(end > 0 ? desc.substring(0, end + 1) : desc.substring(0, 100));
        } else {
            result.setProjectOverview(desc != null ? desc : "");
        }

        List<RequirementOutline> chapters = new ArrayList<>();
        String projectType = params.getProjectType();

        if (ProjectType.SERVICE.getCode().equals(projectType)) {
            chapters.add(new RequirementOutline("project_overview", "项目概况与采购范围",
                    "项目背景、采购目标、采购范围、预算及资金来源", 2000));
            chapters.add(new RequirementOutline("service_scope", "服务范围与内容",
                    "服务内容描述、服务边界、服务成果交付物", 2000));
            chapters.add(new RequirementOutline("service_requirements", "服务要求",
                    "服务标准、服务质量、服务流程、服务响应时间", 2000));
            chapters.add(new RequirementOutline("personnel", "人员配备要求",
                    "项目团队结构、关键岗位资质要求、人员数量与分工", 1500));
            chapters.add(new RequirementOutline("commercial", "商务要求",
                    "报价要求、付款方式、合同期限、违约责任", 1500));
            chapters.add(new RequirementOutline("acceptance", "验收与考核",
                    "验收标准、验收流程、考核指标与奖惩", 1500));
            chapters.add(new RequirementOutline("other", "其他要求",
                    "保密要求、知识产权、争议解决", 1000));
        } else if (ProjectType.GOODS.getCode().equals(projectType)) {
            chapters.add(new RequirementOutline("project_overview", "项目概况与采购范围",
                    "项目背景、采购目标、采购范围、预算及资金来源", 2000));
            chapters.add(new RequirementOutline("procurement_list", "采购清单",
                    "采购设备/货物清单、数量、规格要求概要", 2000));
            chapters.add(new RequirementOutline("technical_specs", "技术规格与参数要求",
                    "核心技术指标、性能参数、配置要求、兼容性要求", 3000));
            chapters.add(new RequirementOutline("quality", "质量标准与检验",
                    "质量标准、检验方法、不合格品处理", 1500));
            chapters.add(new RequirementOutline("commercial", "商务要求",
                    "报价要求、付款方式、交货条件、违约责任", 1500));
            chapters.add(new RequirementOutline("delivery", "交货与验收",
                    "交货时间、交货地点、验收标准与流程", 1500));
            chapters.add(new RequirementOutline("after_sale", "售后服务",
                    "质保期限、售后服务内容、响应时间、培训要求", 1500));
            chapters.add(new RequirementOutline("other", "其他要求",
                    "包装运输、保密要求、知识产权", 1000));
        } else {
            // 工程类默认（projectType为null时也走此分支）
            if (projectType == null) {
                log.warn("projectType为null，降级使用工程类章节模板");
            }
            chapters.add(new RequirementOutline("project_overview", "项目概况与采购范围",
                    "项目背景、建设目标、工程范围、预算及资金来源", 2000));
            chapters.add(new RequirementOutline("scope", "采购范围",
                    "工程内容、工作范围、界限划分", 2000));
            chapters.add(new RequirementOutline("technical_specs", "技术规格与参数要求",
                    "设计标准、技术参数、材料要求、施工工艺", 3000));
            chapters.add(new RequirementOutline("construction", "施工要求",
                    "施工组织、安全文明施工、环保要求、进度要求", 2000));
            chapters.add(new RequirementOutline("quality", "质量验收",
                    "质量标准、验收规范、检测方法", 1500));
            chapters.add(new RequirementOutline("commercial", "商务要求",
                    "报价要求、付款方式、合同工期、违约责任", 1500));
            chapters.add(new RequirementOutline("service", "服务要求",
                    "项目管理、协调配合、保修责任", 1500));
            chapters.add(new RequirementOutline("other", "其他要求",
                    "风险分担、保密要求、争议解决", 1000));
        }

        result.setChapters(chapters);
        return result;
    }

    // ==================== Step 2: 分章并行生成 ====================

    private List<String> generateChapters(OutlineResult outline,
                                          ChatClient client, AiTask task) {
        List<RequirementOutline> chapters = outline.getChapters();
        if (chapters == null || chapters.isEmpty()) {
            return List.of();
        }

        String projectOverview = outline.getProjectOverview() != null ? outline.getProjectOverview() : "";
        // 将章节列表格式化为大纲目录文本
        String outlineDirectory = chapters.stream()
                .map(ch -> ch.getChapterTitle() + "（" + ch.getCorePoints() + "）")
                .collect(Collectors.joining("\n"));

        // 交错并行生成所有章节：每章延迟启动，避免同时提交触发API速率限制
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            RequirementOutline chapter = chapters.get(i);
            int chapterIndex = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                // 首章立即启动，后续章节按序延迟，错开API请求
                if (chapterIndex > 0) {
                    try {
                        Thread.sleep(chapterIndex * CHAPTER_STAGGER_INTERVAL_MS);
                    } catch (InterruptedException ignored) {
                    }
                }
                return generateSingleChapter(chapter, projectOverview, outlineDirectory,
                        client, task, chapterIndex);
            }, virtualThreadExecutor));
        }

        // 等待所有章节完成，超时20分钟
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(20, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            log.error("分章生成等待超时: taskId={}, 取消未完成章节", task.getId());
            futures.forEach(f -> f.cancel(true));
        } catch (Exception e) {
            log.error("分章生成异常: taskId={}, 取消未完成章节", task.getId(), e);
            futures.forEach(f -> f.cancel(true));
        }

        // 按顺序收集结果
        List<String> contents = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            try {
                String content = future.getNow(null);
                contents.add(content != null ? content : "> ⚠️ 本章节内容生成失败，请手动补充");
            } catch (Exception e) {
                contents.add("> ⚠️ 本章节内容生成失败，请手动补充");
            }
        }

        return contents;
    }

    /**
     * 生成单个章节
     */
    private String generateSingleChapter(RequirementOutline chapter,
                                         String projectOverview,
                                         String outlineDirectory,
                                         ChatClient client, AiTask task,
                                         int chapterIndex) {
        String userPrompt = PromptBuilder.buildChapter(
                projectOverview,
                outlineDirectory,
                chapter.getChapterTitle(),
                chapter.getCorePoints(),
                chapter.getEstimatedWords());

        try {
            String aiOutput = aiCallRecorder.callAndRecord(
                    client, SystemPromptTemplates.REQUIREMENT_CHAPTER_GENERATE,
                    userPrompt, "GENERATION", task.getId(), task.getCreateId(), null);
            String content = resultParser.extractMarkdown(aiOutput);
            if (content != null && !content.isBlank()) {
                log.info("章节[{}]生成成功: {}, 字符数={}", chapterIndex, chapter.getChapterTitle(), content.length());
                return content;
            } else {
                log.error("章节[{}]生成结果为空或空白: {}", chapterIndex, chapter.getChapterTitle());
            }
        } catch (Exception e) {
            log.error("章节[{}]生成失败: {} - {}", chapterIndex, chapter.getChapterTitle(), e.getMessage());
        }

        return "> ⚠️ 本章节内容生成失败，请手动补充";
    }

    // ==================== 拼接全文 ====================

    private String assembleFullContent(OutlineResult outline, List<String> chapterContents) {
        StringBuilder sb = new StringBuilder();
        List<RequirementOutline> chapters = outline.getChapters();

        for (int i = 0; i < chapters.size(); i++) {
            if (i > 0) {
                sb.append("\n\n---\n\n");
            }
            sb.append("## ").append(chapters.get(i).getChapterTitle()).append("\n\n");
            sb.append(chapterContents.get(i));
        }

        return sb.toString();
    }

    // ==================== Step 3: 审查与局部修订 ====================

    private String reviewAndRefine(String fullContent,
                                   RequirementGenerateParams params,
                                   ChatClient client, AiTask task) {
        String userPrompt = PromptBuilder.buildReview(
                params.getRequirementName(),
                params.getProjectType(),
                params.getProjectCategory(),
                params.getBudget(),
                fullContent);

        try {
            // 审查调用不传fileIdList，减少token消耗
            String aiOutput = aiCallRecorder.callAndRecord(
                    client, SystemPromptTemplates.REQUIREMENT_REVIEW,
                    userPrompt, "GENERATION", task.getId(), task.getCreateId(), null);

            return applyRevisions(fullContent, aiOutput);
        } catch (Exception e) {
            log.warn("审查修订失败，使用原始内容: taskId={}, error={}", task.getId(), e.getMessage());
            return fullContent;
        }
    }

    /**
     * 解析修订列表并逐项精确替换
     * <p>
     * 防护措施：
     * 1. 原文长度 < MIN_REVISION_ORIGINAL_LENGTH 的修订项跳过（避免短片段误替换）
     * 2. 原文在全文中匹配 >1 次时跳过（避免批量误替换）
     * 3. 按原文在全文中的位置从后向前替换（避免偏移量漂移）
     */
    private String applyRevisions(String fullContent, String aiOutput) {
        try {
            String json = resultParser.extractJson(aiOutput);
            JsonNode root = objectMapper.readTree(json);
            JsonNode revisionsNode = root.get("revisions");

            if (revisionsNode == null || !revisionsNode.isArray() || revisionsNode.isEmpty()) {
                log.info("审查结果无修订项");
                return fullContent;
            }

            // 收集有效修订项及其位置
            List<RevisionEntry> validRevisions = new ArrayList<>();
            int applied = 0;
            int skipped = 0;

            for (JsonNode revision : revisionsNode) {
                String original = revision.path("original").asText("");
                String revised = revision.path("revised").asText("");

                if (original.isEmpty() || revised.isEmpty()) {
                    skipped++;
                    continue;
                }

                // 原文太短，跳过避免误替换
                if (original.length() < MIN_REVISION_ORIGINAL_LENGTH) {
                    skipped++;
                    log.warn("修订项原文过短({}字符)，跳过: [{}]",
                            original.length(),
                            original.length() > 30 ? original.substring(0, 30) + "..." : original);
                    continue;
                }

                // 检查匹配次数
                int matchCount = countMatches(fullContent, original);
                if (matchCount == 0) {
                    skipped++;
                    log.warn("修订项原文未匹配，跳过: original前30字=[{}]",
                            original.length() > 30 ? original.substring(0, 30) + "..." : original);
                } else if (matchCount > 1) {
                    skipped++;
                    log.warn("修订项原文匹配多处({}处)，跳过避免误替换: original前30字=[{}]",
                            matchCount, original.length() > 30 ? original.substring(0, 30) + "..." : original);
                } else {
                    int pos = fullContent.indexOf(original);
                    validRevisions.add(new RevisionEntry(pos, original, revised));
                }
            }

            // 按位置从后向前替换，避免偏移量漂移
            validRevisions.sort((a, b) -> Integer.compare(b.position, a.position));
            StringBuilder result = new StringBuilder(fullContent);
            for (RevisionEntry entry : validRevisions) {
                result.replace(entry.position, entry.position + entry.original.length(), entry.revised);
                applied++;
            }

            log.info("审查修订完成: 应用{}项, 跳过{}项", applied, skipped);
            return result.toString();
        } catch (Exception e) {
            log.warn("修订JSON解析失败，使用原始内容: {}", e.getMessage());
            return fullContent;
        }
    }

    /**
     * 计算文本中子串出现的次数
     */
    private int countMatches(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    // ==================== 内部类 ====================

    /**
     * 修订条目（用于从后向前替换）
     */
    private record RevisionEntry(int position, String original, String revised) {
    }

}
