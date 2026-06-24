package com.jy.eleaitender.ai.processor.checker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.processor.model.GenerateResultParser;
import com.jy.eleaitender.ai.service.FileContentService;
import com.jy.eleaitender.common.dto.ai.DetectionParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionEngineTest {

    @InjectMocks
    private DetectionEngine detectionEngine;

    @Mock
    private FileContentService fileContentService;

    @Mock
    private SensitiveWordDetector sensitiveWordDetector;

    @Mock
    private TypoDetector typoDetector;

    @Spy
    private PolicyReviewDetector policyReviewDetector = new PolicyReviewDetector();

    @Mock
    private FormatCheckDetector formatCheckDetector;

    @Mock
    private GenerateResultParser resultParser;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void policyReviewReturnsEmptyResultWhenNoPolicyFilesSelected() throws Exception {
        DetectionParams params = new DetectionParams();
        params.setContent("detection content");

        AiTask task = new AiTask();
        task.setId(10L);
        task.setTaskType(AiTaskType.DETECTION_POLICY_REVIEW.getCode());
        task.setRequestParams(objectMapper.writeValueAsString(params));
        task.setFileIds("");

        when(resultParser.parseParams(anyString(), eq(DetectionParams.class))).thenReturn(params);

        String result = detectionEngine.detect(task);

        JsonNode root = objectMapper.readTree(result);
        assertThat(root.get("score").asInt()).isEqualTo(100);
        assertThat(root.get("issues")).isNotNull();
        assertThat(root.get("issues").isArray()).isTrue();
        assertThat(root.get("issues")).isEmpty();
        verify(policyReviewDetector, never()).detect(anyString(), anyLong(), any(), any());
    }
}
