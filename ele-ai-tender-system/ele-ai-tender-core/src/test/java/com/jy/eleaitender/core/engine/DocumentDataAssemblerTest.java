package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.dto.FillData;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentDataAssemblerTest {

    @InjectMocks
    private DocumentDataAssembler assembler;

    @Mock
    private TbProjectMapper projectMapper;

    @Mock
    private TbProjectReviewItemMapper reviewItemMapper;

    @Test
    void assembleIncludesComplianceRootWithoutChildren() {
        Long projectId = 100L;
        TbProject project = new TbProject();
        project.setId(projectId);
        project.setProjectName("测试项目");
        when(projectMapper.selectById(projectId)).thenReturn(project);

        TbProjectReviewItem complianceItem = new TbProjectReviewItem();
        complianceItem.setId(1L);
        complianceItem.setProjectId(projectId);
        complianceItem.setLevel(1);
        complianceItem.setItemName("资格条件");
        complianceItem.setItemContent("符合要求");
        complianceItem.setReviewType("COMPLIANCE");
        complianceItem.setSortOrder(0);
        when(reviewItemMapper.selectByProjectId(projectId)).thenReturn(List.of(complianceItem));

        List<FillData> fillDataList = assembler.assemble(projectId);

        String complianceMarkdown = fillDataList.stream()
                .filter(fd -> "complianceItems".equals(fd.getKey()))
                .findFirst()
                .map(fd -> (String) fd.getValue())
                .orElse("");
        assertThat(complianceMarkdown).contains("资格条件: 符合要求");
    }
}
