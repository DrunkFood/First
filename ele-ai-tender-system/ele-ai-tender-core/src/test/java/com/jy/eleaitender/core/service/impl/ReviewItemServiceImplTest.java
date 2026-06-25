package com.jy.eleaitender.core.service.impl;

import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import com.jy.eleaitender.core.service.IProjectService;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewItemServiceImplTest {

    @InjectMocks
    private ReviewItemServiceImpl reviewItemService;

    @Mock
    private TbProjectReviewItemMapper reviewItemMapper;

    @Mock
    private IProjectService projectService;

    @Test
    void replaceAllRebuildsItemsAndRemapsTemporaryParentIds() {
        Long projectId = 100L;
        TbProject project = new TbProject();
        project.setId(projectId);
        when(projectService.getById(projectId)).thenReturn(project);

        TbProjectReviewItem existing = new TbProjectReviewItem();
        existing.setId(9L);
        existing.setProjectId(projectId);
        when(reviewItemMapper.selectByProjectId(projectId)).thenReturn(List.of(existing));

        AtomicLong generatedId = new AtomicLong(101L);
        doAnswer(invocation -> {
            TbProjectReviewItem item = invocation.getArgument(0);
            item.setId(generatedId.getAndIncrement());
            return 1;
        }).when(reviewItemMapper).insert(any(TbProjectReviewItem.class));

        TbProjectReviewItem root = new TbProjectReviewItem();
        root.setId(-1L);
        root.setProjectId(projectId);
        root.setLevel(1);
        root.setItemName("技术标评审");
        root.setReviewType("TECHNICAL");
        root.setSortOrder(0);

        TbProjectReviewItem child = new TbProjectReviewItem();
        child.setId(-2L);
        child.setProjectId(projectId);
        child.setParentId(-1L);
        child.setLevel(2);
        child.setItemName("技术方案");
        child.setReviewType("TECHNICAL");
        child.setSortOrder(1);

        reviewItemService.replaceAll(projectId, List.of(child, root));

        verify(reviewItemMapper).deleteById(9L);
        ArgumentCaptor<TbProjectReviewItem> captor = ArgumentCaptor.forClass(TbProjectReviewItem.class);
        verify(reviewItemMapper, times(2)).insert(captor.capture());

        List<TbProjectReviewItem> inserted = captor.getAllValues();
        assertThat(inserted).extracting(TbProjectReviewItem::getItemName)
                .containsExactly("技术标评审", "技术方案");
        assertThat(inserted.get(0).getParentId()).isNull();
        assertThat(inserted.get(0).getLevel()).isEqualTo(1);
        assertThat(inserted.get(1).getParentId()).isEqualTo(101L);
        assertThat(inserted.get(1).getLevel()).isEqualTo(2);
        assertThat(inserted).allSatisfy(item -> assertThat(item.getProjectId()).isEqualTo(projectId));
    }
}
