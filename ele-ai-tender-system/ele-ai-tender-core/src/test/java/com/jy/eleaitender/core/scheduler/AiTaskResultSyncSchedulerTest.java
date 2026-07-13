package com.jy.eleaitender.core.scheduler;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.core.mapper.AiTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiTaskResultSyncSchedulerTest {

    @InjectMocks
    private AiTaskResultSyncScheduler scheduler;

    @Mock
    private AiTaskMapper aiTaskMapper;

    @Mock
    private AiTaskResultSyncHandler syncHandler;

    @Test
    void syncCompletedTasksSkipsTaskWhenSyncClaimFails() {
        AiTask task = new AiTask();
        task.setId(100L);
        task.setTaskType("REVIEW_ITEM_GENERATE");
        when(aiTaskMapper.selectUnsyncedTasks(20)).thenReturn(List.of(task));
        when(aiTaskMapper.markSyncing(100L)).thenReturn(0);

        scheduler.syncCompletedTasks();

        verify(syncHandler, never()).sync(any(AiTask.class));
        verify(aiTaskMapper, never()).markSuccessSynced(100L);
        verify(aiTaskMapper, never()).markFailedSynced(100L, anyString());
    }

    @Test
    void syncCompletedTasksMarksSuccessOnlyAfterSyncClaimSucceeds() {
        AiTask task = new AiTask();
        task.setId(100L);
        task.setTaskType("REVIEW_ITEM_GENERATE");
        when(aiTaskMapper.selectUnsyncedTasks(20)).thenReturn(List.of(task));
        when(aiTaskMapper.markSyncing(100L)).thenReturn(1);

        scheduler.syncCompletedTasks();

        verify(syncHandler).sync(task);
        verify(aiTaskMapper).markSuccessSynced(100L);
    }
}
