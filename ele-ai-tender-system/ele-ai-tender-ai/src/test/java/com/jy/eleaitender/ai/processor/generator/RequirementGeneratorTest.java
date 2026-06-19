package com.jy.eleaitender.ai.processor.generator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.dto.ai.RequirementGenerateParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequirementGeneratorTest {

    @Mock
    private ModelRouter modelRouter;

    @Mock
    private AiCallRecorder aiCallRecorder;

    @Mock
    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RequirementGenerator generator;

    @BeforeEach
    void setUp() {
        GenerateResultParser resultParser = new GenerateResultParser();
        ReflectionTestUtils.setField(resultParser, "objectMapper", objectMapper);

        generator = new RequirementGenerator();
        ReflectionTestUtils.setField(generator, "modelRouter", modelRouter);
        ReflectionTestUtils.setField(generator, "resultParser", resultParser);
        ReflectionTestUtils.setField(generator, "aiCallRecorder", aiCallRecorder);
        ReflectionTestUtils.setField(generator, "objectMapper", objectMapper);
    }

    @Test
    void generateShouldBuildRequirementContentThroughOutlineChapterAndReviewCalls() throws Exception {
        AiTask task = buildTask();
        ListAppender<ILoggingEvent> logAppender = attachRequirementGenerationLogAppender();
        when(modelRouter.route(AiTaskType.REQUIREMENT_GENERATE)).thenReturn(chatClient);
        when(aiCallRecorder.buildUserPromptWithFiles(anyString(), eq(List.of("101"))))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class) + "\nREFERENCE_FILE_CONTENT");
        when(aiCallRecorder.callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REQUIREMENT_OUTLINE_GENERATE),
                anyString(), eq("GENERATION"), eq(9001L), eq(2L), isNull()))
                .thenReturn("""
                        {
                          "projectOverview": "办公楼装修工程",
                          "chapters": [
                            {
                              "chapterKey": "technical",
                              "chapterTitle": "技术要求",
                              "corePoints": "施工范围、材料标准、质量验收",
                              "estimatedWords": 6000
                            }
                          ]
                        }
                        """);
        when(aiCallRecorder.callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REQUIREMENT_CHAPTER_GENERATE),
                anyString(), eq("GENERATION"), eq(9001L), eq(2L), isNull()))
                .thenReturn("""
                        ### 1. 施工范围
                        1.1 投标人应完成办公楼装修施工。
                        """);
        when(aiCallRecorder.callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REQUIREMENT_REVIEW),
                anyString(), eq("GENERATION"), eq(9001L), eq(2L), isNull()))
                .thenReturn("{\"revisions\":[]}");

        String result;
        try {
            result = generator.generate(task);
        } finally {
            detachRequirementGenerationLogAppender(logAppender);
        }

        JsonNode root = objectMapper.readTree(result);
        assertThat(root.path("content").asText())
                .contains("## 技术要求")
                .contains("投标人应完成办公楼装修施工");

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiCallRecorder).buildUserPromptWithFiles(promptCaptor.capture(), eq(List.of("101")));
        assertThat(promptCaptor.getValue())
                .contains("办公楼装修")
                .contains("工程")
                .contains("限额以下")
                .contains("500000");

        verify(aiCallRecorder).callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REQUIREMENT_OUTLINE_GENERATE),
                anyString(), eq("GENERATION"), eq(9001L), eq(2L), isNull());
        ArgumentCaptor<String> chapterPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiCallRecorder).callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REQUIREMENT_CHAPTER_GENERATE),
                chapterPromptCaptor.capture(), eq("GENERATION"), eq(9001L), eq(2L), isNull());
        assertThat(chapterPromptCaptor.getValue())
                .contains("预估字数上限：5000字")
                .contains("请在该字数上限内编写本章节的详细内容");
        verify(aiCallRecorder).callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REQUIREMENT_REVIEW),
                anyString(), eq("GENERATION"), eq(9001L), eq(2L), isNull());

        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(logMessages)
                .allMatch(message -> message.startsWith("taskId=9001 "))
                .noneMatch(message -> message.contains("callId="))
                .noneMatch(message -> message.contains("event=REQ_GEN_AI_BEGIN"))
                .noneMatch(message -> message.contains("event=REQ_GEN_OUTLINE_RESULT"))
                .noneMatch(message -> message.contains("rawResponse"))
                .noneMatch(message -> message.contains("responseParams="));
        String outlineRequestLog = logMessages.stream()
                .filter(message -> message.contains("event=REQ_GEN_AI_REQUEST")
                        && message.contains("stageCode=OUTLINE_GENERATE"))
                .findFirst()
                .orElseThrow();
        assertThat(outlineRequestLog)
                .contains("requestParams=")
                .contains("5000字以内")
                .contains("章节数量5-6个")
                .endsWith("}");
        String outlineResponseLog = logMessages.stream()
                .filter(message -> message.contains("event=REQ_GEN_AI_RESPONSE")
                        && message.contains("stageCode=OUTLINE_GENERATE"))
                .findFirst()
                .orElseThrow();
        assertThat(outlineResponseLog)
                .contains("responseLength=")
                .contains("outlineResult=")
                .contains("技术要求")
                .endsWith("}");
        assertThat(logMessages)
                .anyMatch(message -> message.contains("event=REQ_GEN_AI_REQUEST")
                        && message.contains("stageCode=OUTLINE_GENERATE")
                        && message.contains("\"systemPrompt\"")
                        && message.contains("\"userPrompt\""))
                .anyMatch(message -> message.contains("event=REQ_GEN_AI_RESPONSE")
                        && message.contains("stageCode=CHAPTER_CONTENT_GENERATE")
                        && message.contains("chapterNo=1")
                        && message.contains("responseLength="))
                .anyMatch(message -> message.contains("event=REQ_GEN_TASK_END")
                        && message.contains("totalDurationMs="));
    }

    private AiTask buildTask() throws Exception {
        RequirementGenerateParams params = new RequirementGenerateParams();
        params.setRequirementName("办公楼装修");
        params.setProjectType("ENGINEERING");
        params.setProjectCategory("LIMITED_BELOW");
        params.setBudget("500000");
        params.setDescription("办公楼室内装修改造。");

        AiTask task = new AiTask();
        task.setId(9001L);
        task.setCreateId(2L);
        task.setTaskType(AiTaskType.REQUIREMENT_GENERATE.getCode());
        task.setFileIds("101");
        task.setRequestParams(objectMapper.writeValueAsString(params));
        return task;
    }

    private ListAppender<ILoggingEvent> attachRequirementGenerationLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger("AI_REQUIREMENT_GENERATION_LOG");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachRequirementGenerationLogAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger("AI_REQUIREMENT_GENERATION_LOG");
        logger.detachAppender(appender);
    }
}
