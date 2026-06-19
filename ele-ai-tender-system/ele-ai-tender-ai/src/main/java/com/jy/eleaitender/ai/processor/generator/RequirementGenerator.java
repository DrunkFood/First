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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static final Logger requirementGenerationLog = LoggerFactory.getLogger("AI_REQUIREMENT_GENERATION_LOG");

    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final String STAGE_OUTLINE_GENERATE = "OUTLINE_GENERATE";

    private static final String STAGE_CHAPTER_CONTENT_GENERATE = "CHAPTER_CONTENT_GENERATE";

    private static final String STAGE_FULL_CONTENT_REVIEW = "FULL_CONTENT_REVIEW";

    private static final String STAGE_ASSEMBLE_CONTENT = "ASSEMBLE_CONTENT";

    private static final String STAGE_REVIEW_APPLY = "REVIEW_APPLY";

    private static final String MODEL_SCENE_GENERATION = "GENERATION";

    private static final int MAX_REQUIREMENT_TOTAL_WORDS = 5000;

    private static final int DEFAULT_CHAPTER_ESTIMATED_WORDS = 800;

    private static final int MIN_CHAPTER_ESTIMATED_WORDS = 300;

    /**
     * 修订原文最小字符数，过短的原文容易误替换
     */
    private static final int MIN_REVISION_ORIGINAL_LENGTH = 20;

    /**
     * 分章生成最大并发数，避免瞬间并发触发API速率限制
     */
    private static final int CHAPTER_GENERATION_CONCURRENCY = 3;

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
        String taskStartTime = Timing.currentTime();
        long totalStart = Timing.now();
        long outlineMs = -1L;
        long chaptersMs = -1L;
        long assembleMs = -1L;
        long reviewMs = -1L;
        int chapterCount = 0;
        int contentLength = 0;
        boolean success = false;
        log.info("开始需求生成(Agent三步式): taskId={}", task.getId());
        logTaskStart(task, taskStartTime);

        try {
            RequirementGenerateParams params = resultParser.parseParams(task.getRequestParams(), RequirementGenerateParams.class);
            ChatClient client = modelRouter.route(AiTaskType.REQUIREMENT_GENERATE);

            // Step 1: 生成大纲（参考文件内容在此步骤使用）
            long outlineStart = Timing.now();
            OutlineResult outline = generateOutline(params, client, task);
            outlineMs = Timing.msSince(outlineStart);
            chapterCount = outline.getChapters().size();
            log.info("Step1大纲生成完成: taskId={}, 章节数={}", task.getId(), chapterCount);

            // Step 2: 分章并行生成（不传fileIdList，避免参考文件内容重复注入）
            long chaptersStart = Timing.now();
            List<String> chapterContents = generateChapters(outline, client, task);
            chaptersMs = Timing.msSince(chaptersStart);
            log.info("Step2分章生成完成: taskId={}, 成功章节数={}", task.getId(), chapterContents.size());

            // Step 3: 拼接全文
            String assembleStartTime = Timing.currentTime();
            long assembleStart = Timing.now();
            String fullContent = assembleFullContent(outline, chapterContents);
            assembleMs = Timing.msSince(assembleStart);
            Map<String, Object> assembleParams = new LinkedHashMap<>();
            assembleParams.put("contentLength", fullContent.length());
            logStepEnd(task, STAGE_ASSEMBLE_CONTENT, "拼接全文", null, null,
                    "success", assembleStartTime, Timing.currentTime(), assembleMs,
                    assembleParams);
            log.info("Step3全文拼接完成: taskId={}, 总字符数={}", task.getId(), fullContent.length());

            // Step 4: 审查与局部修订（不传fileIdList，减少token消耗）
            long reviewStart = Timing.now();
            String finalContent = reviewAndRefine(fullContent, params, client, task);
            reviewMs = Timing.msSince(reviewStart);
            contentLength = finalContent.length();
            log.info("Step4需求生成完成: taskId={}, 最终字符数={}", task.getId(), contentLength);

            String result = resultParser.toJsonResult("content", finalContent);
            success = true;
            return result;
        } finally {
            requirementGenerationLog.info("taskId={} event=REQ_GEN_TASK_END taskType={} status={} startTime={} endTime={} totalDurationMs={} outlineDurationMs={} chaptersDurationMs={} assembleDurationMs={} reviewDurationMs={} chapterCount={} contentLength={}",
                    task.getId(), task.getTaskType(), success ? "success" : "error", taskStartTime, Timing.currentTime(),
                    Timing.msSince(totalStart), outlineMs, chaptersMs, assembleMs, reviewMs,
                    chapterCount, contentLength);
        }
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

        String systemPrompt = SystemPromptTemplates.REQUIREMENT_OUTLINE_GENERATE;
        String resolvedUserPrompt = aiCallRecorder.buildUserPromptWithFiles(userPrompt, task.getFileIdList());

        AiCallLogContext aiCall = beginAiCall(task, STAGE_OUTLINE_GENERATE, "生成大纲", null, null);
        logAiRequest(task, aiCall, systemPrompt, resolvedUserPrompt, task.getFileIdList());
        AiCallLogResult aiCallResult;
        boolean aiSuccess = false;
        String aiOutput = null;
        String errorMessage = null;
        try {
            aiOutput = aiCallRecorder.callAndRecord(
                    client, systemPrompt,
                    resolvedUserPrompt, MODEL_SCENE_GENERATION, task.getId(), task.getCreateId(), null);
            aiSuccess = true;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            throw e;
        } finally {
            aiCallResult = finishAiCall(task, aiCall, aiSuccess, aiOutput, errorMessage);
        }

        // 解析大纲JSON
        String json = resultParser.extractJson(aiOutput);
        OutlineResult outline = resultParser.parseParams(json, OutlineResult.class);
        if (outline == null || outline.getChapters() == null || outline.getChapters().isEmpty()) {
            log.warn("大纲解析失败或为空，使用降级模板: taskId={}", task.getId());
            outline = buildFallbackOutline(params);
        }
        normalizeOutlineEstimatedWords(task.getId(), outline);
        return outline;
    }

    private void normalizeOutlineEstimatedWords(Long taskId, OutlineResult outline) {
        if (outline == null || outline.getChapters() == null || outline.getChapters().isEmpty()) {
            return;
        }

        List<RequirementOutline> chapters = outline.getChapters();
        int defaultWords = Math.max(1, MAX_REQUIREMENT_TOTAL_WORDS / chapters.size());
        int totalWords = 0;
        for (RequirementOutline chapter : chapters) {
            if (chapter.getEstimatedWords() <= 0) {
                chapter.setEstimatedWords(defaultWords);
            }
            totalWords += chapter.getEstimatedWords();
        }

        if (totalWords <= MAX_REQUIREMENT_TOTAL_WORDS) {
            return;
        }

        int minWords = Math.min(MIN_CHAPTER_ESTIMATED_WORDS, defaultWords);
        int remainingWords = MAX_REQUIREMENT_TOTAL_WORDS;
        for (int i = 0; i < chapters.size(); i++) {
            RequirementOutline chapter = chapters.get(i);
            int remainingChapters = chapters.size() - i;
            int scaledWords;
            if (remainingChapters == 1) {
                scaledWords = remainingWords;
            } else {
                int reservedWords = minWords * (remainingChapters - 1);
                scaledWords = (int) Math.floor(chapter.getEstimatedWords() * (double) MAX_REQUIREMENT_TOTAL_WORDS / totalWords);
                scaledWords = Math.max(minWords, scaledWords);
                scaledWords = Math.min(scaledWords, remainingWords - reservedWords);
            }
            chapter.setEstimatedWords(Math.max(1, scaledWords));
            remainingWords -= chapter.getEstimatedWords();
        }

        log.info("大纲预估总字数超过{}，已压缩到{}以内: taskId={}, originalTotal={}",
                MAX_REQUIREMENT_TOTAL_WORDS, MAX_REQUIREMENT_TOTAL_WORDS, taskId, totalWords);
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
                    "项目背景、采购目标、采购范围、预算及资金来源", 900));
            chapters.add(new RequirementOutline("service_scope", "服务范围与内容",
                    "服务内容描述、服务边界、服务成果交付物", 1100));
            chapters.add(new RequirementOutline("service_requirements", "服务要求",
                    "服务标准、人员配备、服务质量、服务流程、服务响应时间", 1100));
            chapters.add(new RequirementOutline("commercial", "商务要求",
                    "报价要求、付款方式、合同期限、违约责任", 900));
            chapters.add(new RequirementOutline("acceptance_other", "验收与其他要求",
                    "验收标准、验收流程、考核指标、保密要求、知识产权、争议解决", 1000));
        } else if (ProjectType.GOODS.getCode().equals(projectType)) {
            chapters.add(new RequirementOutline("project_overview", "项目概况与采购范围",
                    "项目背景、采购目标、采购范围、预算及资金来源", 700));
            chapters.add(new RequirementOutline("procurement_list", "采购清单",
                    "采购设备/货物清单、数量、规格要求概要", 800));
            chapters.add(new RequirementOutline("technical_specs", "技术规格与参数要求",
                    "核心技术指标、性能参数、配置要求、兼容性要求", 1200));
            chapters.add(new RequirementOutline("quality_delivery", "质量标准与交货验收",
                    "质量标准、检验方法、交货时间、交货地点、验收标准与流程", 900));
            chapters.add(new RequirementOutline("commercial", "商务要求",
                    "报价要求、付款方式、交货条件、违约责任", 700));
            chapters.add(new RequirementOutline("after_sale_other", "售后服务与其他要求",
                    "质保期限、售后服务内容、响应时间、培训要求、包装运输、保密要求、知识产权", 700));
        } else {
            // 工程类默认（projectType为null时也走此分支）
            if (projectType == null) {
                log.warn("projectType为null，降级使用工程类章节模板");
            }
            chapters.add(new RequirementOutline("project_overview_scope", "项目概况与采购范围",
                    "项目背景、建设目标、工程范围、界限划分、预算及资金来源", 800));
            chapters.add(new RequirementOutline("technical_specs", "技术规格与参数要求",
                    "设计标准、技术参数、材料要求、施工工艺", 1200));
            chapters.add(new RequirementOutline("construction", "施工要求",
                    "施工组织、安全文明施工、环保要求、进度要求", 900));
            chapters.add(new RequirementOutline("quality", "质量验收",
                    "质量标准、验收规范、检测方法", 800));
            chapters.add(new RequirementOutline("commercial", "商务要求",
                    "报价要求、付款方式、合同工期、违约责任", 700));
            chapters.add(new RequirementOutline("service_other", "服务与其他要求",
                    "项目管理、协调配合、保修责任、风险分担、保密要求、争议解决", 600));
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

        // 限流并行生成所有章节：最多同时发起固定数量的章节AI请求，避免触发API速率限制
        Semaphore chapterSemaphore = new Semaphore(CHAPTER_GENERATION_CONCURRENCY);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            RequirementOutline chapter = chapters.get(i);
            int chapterIndex = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                String chapterStartTime = Timing.currentTime();
                long chapterStart = Timing.now();
                long delayMs = 0;
                long delayStart = Timing.now();
                boolean acquired = false;
                try {
                    chapterSemaphore.acquire();
                    acquired = true;
                    delayMs = Timing.msSince(delayStart);
                    return generateSingleChapter(chapter, projectOverview, outlineDirectory,
                            client, task, chapterIndex, delayMs, chapterStartTime, chapterStart);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("章节[{}]等待并发许可被中断: taskId={}, title={}",
                            chapterIndex, task.getId(), chapter.getChapterTitle());
                    return "> ⚠️ 本章节内容生成失败，请手动补充";
                } finally {
                    if (acquired) {
                        chapterSemaphore.release();
                    }
                }
            }, virtualThreadExecutor));
        }

        // 等待所有章节完成，超时20分钟
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.MINUTES);
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
                                         int chapterIndex,
                                         long delayMs,
                                         String chapterStartTime,
                                         long chapterStart) {
        String userPrompt = PromptBuilder.buildChapter(
                projectOverview,
                outlineDirectory,
                chapter.getChapterTitle(),
                chapter.getCorePoints(),
                chapter.getEstimatedWords());

        String systemPrompt = SystemPromptTemplates.REQUIREMENT_CHAPTER_GENERATE;
        AiCallLogContext aiCall = beginAiCall(task, STAGE_CHAPTER_CONTENT_GENERATE, "生成章节正文",
                chapterIndex, chapter.getChapterTitle());
        logAiRequest(task, aiCall, systemPrompt, userPrompt, List.of());
        AiCallLogResult aiCallResult = null;

        try {
            boolean aiSuccess = false;
            String aiOutput = null;
            String errorMessage = null;
            try {
                aiOutput = aiCallRecorder.callAndRecord(
                        client, systemPrompt,
                        userPrompt, MODEL_SCENE_GENERATION, task.getId(), task.getCreateId(), null);
                aiSuccess = true;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                throw e;
            } finally {
                aiCallResult = finishAiCall(task, aiCall, aiSuccess, aiOutput, errorMessage);
            }
            long aiMs = aiCallResult.durationMs();
            String content = resultParser.extractMarkdown(aiOutput);
            if (content != null && !content.isBlank()) {
                log.info("taskId={}, 章节[{}]生成成功: {}, 字符数={}", task.getId(), chapterIndex, chapter.getChapterTitle(), content.length());
                logChapterStepEnd(task, aiCall, "success", chapterStartTime, Timing.currentTime(),
                        Timing.msSince(chapterStart), delayMs, aiMs, content.length());
                return content;
            } else {
                logChapterStepEnd(task, aiCall, "empty", chapterStartTime, Timing.currentTime(),
                        Timing.msSince(chapterStart), delayMs, aiMs, 0);
                log.error("taskId={}, 章节[{}]生成结果为空: {}", task.getId(), chapterIndex, chapter.getChapterTitle());
            }
        } catch (Exception e) {
            long aiMs = aiCallResult != null ? aiCallResult.durationMs() : Timing.msSince(aiCall.startNanos());
            log.error("taskId={}, 章节[{}]生成失败: {} - {}", task.getId(), chapterIndex, chapter.getChapterTitle(), e.getMessage(), e);
            logChapterStepEnd(task, aiCall, "error", chapterStartTime, Timing.currentTime(),
                    Timing.msSince(chapterStart), delayMs, aiMs, 0);
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

        String systemPrompt = SystemPromptTemplates.REQUIREMENT_REVIEW;
        AiCallLogContext aiCall = beginAiCall(task, STAGE_FULL_CONTENT_REVIEW, "全文审查校验", null, null);
        logAiRequest(task, aiCall, systemPrompt, userPrompt, List.of());

        try {
            // 审查调用不传fileIdList，减少token消耗
            AiCallLogResult aiCallResult;
            boolean aiSuccess = false;
            String aiOutput = null;
            String errorMessage = null;
            try {
                aiOutput = aiCallRecorder.callAndRecord(
                        client, systemPrompt,
                        userPrompt, MODEL_SCENE_GENERATION, task.getId(), task.getCreateId(), null);
                aiSuccess = true;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                throw e;
            } finally {
                aiCallResult = finishAiCall(task, aiCall, aiSuccess, aiOutput, errorMessage);
            }

            String applyStartTime = Timing.currentTime();
            long applyStart = Timing.now();
            String refinedContent = applyRevisions(fullContent, aiOutput);
            Map<String, Object> applyParams = new LinkedHashMap<>();
            applyParams.put("inputLength", fullContent.length());
            applyParams.put("outputLength", refinedContent.length());
            logStepEnd(task, STAGE_REVIEW_APPLY, "应用审查修订", null, null,
                    "success", applyStartTime, Timing.currentTime(), Timing.msSince(applyStart),
                    applyParams);
            return refinedContent;
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
                    log.warn("修订项原文过短({}字符)，跳过: [{}]", original.length(), original);
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

    private AiCallLogContext beginAiCall(AiTask task,
                                         String stageCode,
                                         String stageName,
                                         Integer chapterIndex,
                                         String chapterTitle) {
        String callTime = Timing.currentTime();
        AiCallLogContext context = new AiCallLogContext(stageCode, stageName,
                chapterIndex, safeLogValue(chapterTitle), callTime, Timing.now());
        return context;
    }

    private AiCallLogResult finishAiCall(AiTask task,
                                         AiCallLogContext context,
                                         boolean success,
                                         String aiOutput,
                                         String errorMessage) {
        String returnTime = Timing.currentTime();
        long durationMs = Timing.msSince(context.startNanos());
        String status = success ? "success" : "error";
        int responseLength = aiOutput == null ? 0 : aiOutput.length();
        AiCallLogResult result = new AiCallLogResult(returnTime, durationMs, status, responseLength);

        if (errorMessage != null && !errorMessage.isBlank()) {
            requirementGenerationLog.info("taskId={} event=REQ_GEN_AI_RESPONSE stageCode={} stageName={} chapterNo={} chapterTitle=\"{}\" status={} startTime={} endTime={} durationMs={} responseLength={} errorMessage=\"{}\"",
                    task.getId(), context.stageCode(), context.stageName(), chapterNo(context),
                    context.chapterTitle(), result.status(), context.callTime(), result.returnTime(),
                    result.durationMs(), result.responseLength(), safeLogValue(errorMessage));
            return result;
        }

        if (STAGE_OUTLINE_GENERATE.equals(context.stageCode()) && aiOutput != null) {
            requirementGenerationLog.info("taskId={} event=REQ_GEN_AI_RESPONSE stageCode={} stageName={} chapterNo={} chapterTitle=\"{}\" status={} startTime={} endTime={} durationMs={} responseLength={} outlineResult={}",
                    task.getId(), context.stageCode(), context.stageName(), chapterNo(context),
                    context.chapterTitle(), result.status(), context.callTime(), result.returnTime(),
                    result.durationMs(), result.responseLength(), safeLogValue(aiOutput));
            return result;
        }

        requirementGenerationLog.info("taskId={} event=REQ_GEN_AI_RESPONSE stageCode={} stageName={} chapterNo={} chapterTitle=\"{}\" status={} startTime={} endTime={} durationMs={} responseLength={}",
                task.getId(), context.stageCode(), context.stageName(), chapterNo(context),
                context.chapterTitle(), result.status(), context.callTime(), result.returnTime(),
                result.durationMs(), result.responseLength());
        return result;
    }

    private void logTaskStart(AiTask task, String startTime) {
        requirementGenerationLog.info("taskId={} event=REQ_GEN_TASK_START taskType={} startTime={} requestParams={}",
                task.getId(), task.getTaskType(), startTime, rawJsonLog(task.getRequestParams()));
    }

    private void logAiRequest(AiTask task,
                              AiCallLogContext context,
                              String systemPrompt,
                              String userPrompt,
                              List<String> fileIds) {
        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("modelScene", MODEL_SCENE_GENERATION);
        requestParams.put("taskType", task.getTaskType());
        requestParams.put("createId", task.getCreateId());
        requestParams.put("fileIds", fileIds != null ? fileIds : List.of());
        requestParams.put("systemPrompt", systemPrompt != null ? systemPrompt : "");
        requestParams.put("userPrompt", userPrompt != null ? userPrompt : "");

        int requestLength = (systemPrompt == null ? 0 : systemPrompt.length())
                + (userPrompt == null ? 0 : userPrompt.length());
        requirementGenerationLog.info("taskId={} event=REQ_GEN_AI_REQUEST stageCode={} stageName={} chapterNo={} chapterTitle=\"{}\" startTime={} requestLength={} requestParams={}",
                task.getId(), context.stageCode(), context.stageName(), chapterNo(context),
                context.chapterTitle(), context.callTime(), requestLength, toJsonLog(requestParams));
    }

    private void logChapterStepEnd(AiTask task,
                                   AiCallLogContext context,
                                   String status,
                                   String startTime,
                                   String endTime,
                                   long durationMs,
                                   long delayMs,
                                   long aiMs,
                                   int contentLength) {
        requirementGenerationLog.info("taskId={} event=REQ_GEN_STEP_END stageCode={} stageName={} chapterNo={} chapterTitle=\"{}\" status={} startTime={} endTime={} durationMs={} delayDurationMs={} aiDurationMs={} contentLength={}",
                task.getId(), context.stageCode(), context.stageName(), chapterNo(context),
                context.chapterTitle(), status, startTime, endTime, durationMs, delayMs, aiMs, contentLength);
    }

    private void logStepEnd(AiTask task,
                            String stageCode,
                            String stageName,
                            Integer chapterIndex,
                            String chapterTitle,
                            String status,
                            String startTime,
                            String endTime,
                            long durationMs,
                            Map<String, Object> extraParams) {
        requirementGenerationLog.info("taskId={} event=REQ_GEN_STEP_END stageCode={} stageName={} chapterNo={} chapterTitle=\"{}\" status={} startTime={} endTime={} durationMs={} extraParams={}",
                task.getId(), stageCode, stageName, chapterIndex == null ? "-" : chapterIndex + 1,
                safeLogValue(chapterTitle), status, startTime, endTime, durationMs,
                toJsonLog(extraParams != null ? extraParams : Map.of()));
    }

    private Object chapterNo(AiCallLogContext context) {
        if (context.chapterIndex() == null) {
            return "-";
        }
        return context.chapterIndex() + 1;
    }

    private String rawJsonLog(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(rawJson));
        } catch (Exception ignored) {
            return toJsonLog(rawJson);
        }
    }

    private String toJsonLog(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "\"" + safeLogValue(String.valueOf(value)) + "\"";
        }
    }

    private String safeLogValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private record AiCallLogContext(String stageCode,
                                    String stageName,
                                    Integer chapterIndex,
                                    String chapterTitle,
                                    String callTime,
                                    long startNanos) {
    }

    private record AiCallLogResult(String returnTime,
                                   long durationMs,
                                   String status,
                                   int responseLength) {
    }

    private static final class Timing {
        private Timing() {
        }

        private static String currentTime() {
            return LocalDateTime.now().format(LOG_TIME_FORMATTER);
        }

        private static long now() {
            return System.nanoTime();
        }

        private static long msSince(long startNanos) {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        }
    }

}
