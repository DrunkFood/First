package com.jy.eleaitender.core.service.impl;

import com.jy.eleaitender.common.dto.ai.AiTaskParams;
import com.jy.eleaitender.common.dto.ai.DetectionParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.core.dto.request.DetectionSubmitRequest;
import com.jy.eleaitender.core.mapper.TbDetectionRecordMapper;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DetectionServiceImplTest {

    @InjectMocks
    private DetectionServiceImpl detectionService;

    @Mock
    private TbProjectMapper projectMapper;

    @Mock
    private TbProjectReviewItemMapper reviewItemMapper;

    @Mock
    private TbDetectionRecordMapper detectionRecordMapper;

    @Mock
    private IAiTaskService aiTaskService;

    @Test
    void submitUsesBusinessContentInsteadOfGeneratedDocumentFile() {
        Long projectId = 100L;
        TbProject project = new TbProject();
        project.setId(projectId);
        project.setStatus(ProjectStatus.IN_PROGRESS.getCode());
        project.setGeneratedFileId(999L);
        project.setRequirementContent("Requirement body");

        TbProjectReviewItem reviewItem = new TbProjectReviewItem();
        reviewItem.setLevel(2);
        reviewItem.setReviewType("TECHNICAL");
        reviewItem.setItemName("Technical score");
        reviewItem.setItemContent("Review standard");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(reviewItemMapper.selectByProjectId(projectId)).thenReturn(List.of(reviewItem));

        AtomicLong taskId = new AtomicLong(1L);
        when(aiTaskService.createTask(any(AiTaskType.class), anyLong(), anyLong(), anyString(),
                any(AiTaskParams.class), anyString())).thenAnswer(invocation -> {
            AiTask task = new AiTask();
            task.setId(taskId.getAndIncrement());
            return task;
        });

        detectionService.submit(projectId, null);

        ArgumentCaptor<AiTaskType> taskTypeCaptor = ArgumentCaptor.forClass(AiTaskType.class);
        ArgumentCaptor<AiTaskParams> paramsCaptor = ArgumentCaptor.forClass(AiTaskParams.class);
        verify(aiTaskService, times(3)).createTask(taskTypeCaptor.capture(), anyLong(), anyLong(), anyString(),
                paramsCaptor.capture(), anyString());

        assertThat(taskTypeCaptor.getAllValues())
                .doesNotContain(AiTaskType.DETECTION_POLICY_REVIEW);
        assertThat(paramsCaptor.getAllValues())
                .hasSize(3)
                .allSatisfy(taskParams -> {
                    DetectionParams params = (DetectionParams) taskParams;
                    assertThat(params.getContentFileId()).isNull();
                    assertThat(params.getContent())
                            .contains("Requirement body")
                            .contains("Technical score")
                            .contains("Review standard")
                            .doesNotContain("999");
                });
    }

    @Test
    void submitIncludesPolicyReviewWhenPolicyFilesSelected() {
        Long projectId = 100L;
        TbProject project = new TbProject();
        project.setId(projectId);
        project.setStatus(ProjectStatus.IN_PROGRESS.getCode());
        project.setRequirementContent("Requirement body");

        when(projectMapper.selectById(projectId)).thenReturn(project);
        when(reviewItemMapper.selectByProjectId(projectId)).thenReturn(List.of());

        AtomicLong taskId = new AtomicLong(1L);
        when(aiTaskService.createTask(any(AiTaskType.class), anyLong(), anyLong(), anyString(),
                any(AiTaskParams.class), anyString())).thenAnswer(invocation -> {
            AiTask task = new AiTask();
            task.setId(taskId.getAndIncrement());
            return task;
        });

        DetectionSubmitRequest request = new DetectionSubmitRequest();
        request.setPolicyFileIds(List.of(11L, 22L));

        var taskIds = detectionService.submit(projectId, request);

        ArgumentCaptor<AiTaskType> taskTypeCaptor = ArgumentCaptor.forClass(AiTaskType.class);
        ArgumentCaptor<String> fileIdsCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiTaskService, times(4)).createTask(taskTypeCaptor.capture(), anyLong(), anyLong(), anyString(),
                any(AiTaskParams.class), fileIdsCaptor.capture());

        assertThat(taskTypeCaptor.getAllValues()).contains(AiTaskType.DETECTION_POLICY_REVIEW);
        assertThat(fileIdsCaptor.getAllValues()).allMatch("11,22"::equals);
        assertThat(taskIds).containsKey("POLICY_REVIEW");
    }
}
