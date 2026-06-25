package com.jy.eleaitender.ai.processor.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.processor.model.ModelRouter;
import com.jy.eleaitender.ai.processor.model.RoutedChatClient;
import com.jy.eleaitender.ai.processor.prompt.SystemPromptTemplates;
import com.jy.eleaitender.ai.processor.recorder.AiCallRecorder;
import com.jy.eleaitender.common.dto.ReviewConfig;
import com.jy.eleaitender.common.dto.ai.ReviewItemGenerateParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ScoreMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewItemGeneratorTest {

    @Mock
    private ModelRouter modelRouter;

    @Mock
    private AiCallRecorder aiCallRecorder;

    @Mock
    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReviewItemGenerator generator;

    @BeforeEach
    void setUp() {
        GenerateResultParser resultParser = new GenerateResultParser();
        ReflectionTestUtils.setField(resultParser, "objectMapper", objectMapper);

        generator = new ReviewItemGenerator();
        ReflectionTestUtils.setField(generator, "modelRouter", modelRouter);
        ReflectionTestUtils.setField(generator, "resultParser", resultParser);
        ReflectionTestUtils.setField(generator, "aiCallRecorder", aiCallRecorder);
        ReflectionTestUtils.setField(generator, "objectMapper", objectMapper);
    }

    @Test
    void generateShouldWrapArrayRootAsReviewItems() throws Exception {
        mockAiOutput("""
                [
                  {
                    "name": "技术评审",
                    "level": 1,
                    "children": [
                      {
                        "name": "施工方案",
                        "level": 2,
                        "content": "施工方案完整性",
                        "score": 60,
                        "subjectivity": "SUBJECTIVE",
                        "isRequired": false,
                        "children": []
                      }
                    ]
                  }
                ]
                """);

        JsonNode root = objectMapper.readTree(generator.generate(buildTask()));

        assertThat(root.path("reviewItems")).hasSize(1);
        assertThat(root.path("reviewItems").get(0).path("name").asText()).isEqualTo("技术评审");
    }

    @Test
    void generateShouldUnwrapNestedReviewItems() throws Exception {
        mockAiOutput("""
                {
                  "data": {
                    "reviewItems": [
                      {
                        "name": "商务评审",
                        "level": 1,
                        "children": []
                      }
                    ]
                  }
                }
                """);

        JsonNode root = objectMapper.readTree(generator.generate(buildTask()));

        assertThat(root.path("reviewItems")).hasSize(1);
        assertThat(root.path("reviewItems").get(0).path("name").asText()).isEqualTo("商务评审");
    }

    @Test
    void generateShouldConvertCategoryObjectToReviewItems() throws Exception {
        mockAiOutput("""
                {
                  "符合性审查": [
                    {
                      "name": "资格条件",
                      "level": 2,
                      "content": "满足资格条件",
                      "score": 0,
                      "subjectivity": "OBJECTIVE",
                      "isRequired": true,
                      "children": []
                    }
                  ],
                  "技术评审": [
                    {
                      "name": "施工方案",
                      "level": 2,
                      "content": "施工方案完整性",
                      "score": 60,
                      "subjectivity": "SUBJECTIVE",
                      "isRequired": false,
                      "children": []
                    }
                  ],
                  "商务评审": [
                    {
                      "name": "报价响应",
                      "level": 2,
                      "content": "报价合理性",
                      "score": 40,
                      "subjectivity": "OBJECTIVE",
                      "isRequired": false,
                      "children": []
                    }
                  ]
                }
                """);

        JsonNode root = objectMapper.readTree(generator.generate(buildTask()));

        assertThat(root.path("reviewItems")).hasSize(3);
        assertThat(root.path("reviewItems").get(0).path("name").asText()).isEqualTo("符合性审查");
        assertThat(root.path("reviewItems").get(1).path("children")).hasSize(1);
    }

    @Test
    void generateShouldAskAiToRepairJsonWhenInitialOutputCannotBeParsed() throws Exception {
        when(modelRouter.routeWithInfo(AiTaskType.REVIEW_ITEM_GENERATE))
                .thenReturn(new RoutedChatClient(chatClient, "glm-test"));
        when(aiCallRecorder.callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REVIEW_ITEM_GENERATE),
                anyString(), eq("GENERATION"), eq(7001L), eq(9L), isNull(), eq("glm-test")))
                .thenReturn("以下为评审项：技术方案60分，报价40分。");
        when(aiCallRecorder.callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REVIEW_ITEM_JSON_REPAIR),
                contains("以下为评审项：技术方案60分，报价40分。"), eq("GENERATION"), eq(7001L), eq(9L), isNull(), eq("glm-test")))
                .thenReturn("""
                        {
                          "reviewItems": [
                            {
                              "name": "技术评审",
                              "level": 1,
                              "children": [
                                {
                                  "name": "技术方案",
                                  "level": 2,
                                  "content": "技术方案完整性",
                                  "score": 60,
                                  "subjectivity": "SUBJECTIVE",
                                  "isRequired": false,
                                  "children": []
                                }
                              ]
                            },
                            {
                              "name": "商务评审",
                              "level": 1,
                              "children": [
                                {
                                  "name": "报价",
                                  "level": 2,
                                  "content": "报价合理性",
                                  "score": 40,
                                  "subjectivity": "OBJECTIVE",
                                  "isRequired": false,
                                  "children": []
                                }
                              ]
                            }
                          ]
                        }
                        """);

        JsonNode root = objectMapper.readTree(generator.generate(buildTask()));

        assertThat(root.path("reviewItems")).hasSize(2);
        verify(aiCallRecorder).callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REVIEW_ITEM_JSON_REPAIR),
                contains("只修复JSON格式"), eq("GENERATION"), eq(7001L), eq(9L), isNull(), eq("glm-test"));
    }

    @Test
    void generateShouldUseWeightPromptWhenScoreModeIsWeight() throws Exception {
        // 权重模式：reviewConfig.scoreMode=WEIGHT，应调用 REVIEW_ITEM_GENERATE_WEIGHT 的 system prompt
        String weightAiOutput = """
                {
                  "reviewItems": [
                    {
                      "name": "技术评审",
                      "level": 1,
                      "weight": 60,
                      "children": [
                        {
                          "name": "施工方案",
                          "level": 2,
                          "weight": 60,
                          "content": "施工方案完整性",
                          "subjectivity": "SUBJECTIVE",
                          "isRequired": false,
                          "children": []
                        }
                      ]
                    }
                  ]
                }
                """;
        when(modelRouter.route(AiTaskType.REVIEW_ITEM_GENERATE)).thenReturn(chatClient);
        when(aiCallRecorder.callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REVIEW_ITEM_GENERATE_WEIGHT),
                anyString(), eq("GENERATION"), eq(7001L), eq(9L), isNull()))
                .thenReturn(weightAiOutput);

        JsonNode root = objectMapper.readTree(generator.generate(buildWeightTask()));

        assertThat(root.path("reviewItems")).hasSize(1);
        assertThat(root.path("reviewItems").get(0).path("name").asText()).isEqualTo("技术评审");
        // 关键断言：权重模式必须以 WEIGHT system prompt 调用，且不能走 SCORE
        verify(aiCallRecorder).callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REVIEW_ITEM_GENERATE_WEIGHT),
                anyString(), eq("GENERATION"), eq(7001L), eq(9L), isNull());
    }

    private void mockAiOutput(String aiOutput) {
        when(modelRouter.routeWithInfo(AiTaskType.REVIEW_ITEM_GENERATE))
                .thenReturn(new RoutedChatClient(chatClient, "glm-test"));
        when(aiCallRecorder.callAndRecord(eq(chatClient), eq(SystemPromptTemplates.REVIEW_ITEM_GENERATE),
                anyString(), eq("GENERATION"), eq(7001L), eq(9L), isNull(), eq("glm-test")))
                .thenReturn(aiOutput);
    }

    private AiTask buildTask() throws Exception {
        ReviewItemGenerateParams params = new ReviewItemGenerateParams();
        params.setProjectName("办公楼装修");
        params.setProjectType("ENGINEERING");
        params.setProjectCategory("SMALL_TRADE");
        params.setBudget("500000");
        params.setReviewMethod("INTELLIGENT");
        params.setRequirementContent("办公楼装修采购需求。");

        AiTask task = new AiTask();
        task.setId(7001L);
        task.setCreateId(9L);
        task.setTaskType(AiTaskType.REVIEW_ITEM_GENERATE.getCode());
        task.setRequestParams(objectMapper.writeValueAsString(params));
        return task;
    }

    /**
     * 构建权重模式任务：reviewConfig.scoreMode=WEIGHT，含启用类型
     */
    private AiTask buildWeightTask() throws Exception {
        ReviewConfig config = ReviewConfig.defaultConfig();
        config.setScoreMode(ScoreMode.WEIGHT);

        ReviewItemGenerateParams params = new ReviewItemGenerateParams();
        params.setProjectName("办公楼装修");
        params.setProjectType("ENGINEERING");
        params.setProjectCategory("SMALL_TRADE");
        params.setBudget("500000");
        params.setReviewMethod("INTELLIGENT");
        params.setRequirementContent("办公楼装修采购需求。");
        params.setReviewConfig(objectMapper.writeValueAsString(config));

        AiTask task = new AiTask();
        task.setId(7001L);
        task.setCreateId(9L);
        task.setTaskType(AiTaskType.REVIEW_ITEM_GENERATE.getCode());
        task.setRequestParams(objectMapper.writeValueAsString(params));
        return task;
    }
}
