package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.dto.FillData;
import com.jy.eleaitender.common.dto.ReviewConfig;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.ScoreMode;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * DocumentDataAssembler 评审汇总表构建单测，聚焦权重模式/分值模式类别标签差异。
 * toReviewSummaryList 为纯函数（仅依赖静态 typeLabels + 入参），无需注入 mapper。
 */
@ExtendWith(MockitoExtension.class)
class DocumentDataAssemblerTest {

    @InjectMocks
    private DocumentDataAssembler assembler;

    @Mock
    private TbProjectMapper projectMapper;

    @Mock
    private TbProjectReviewItemMapper reviewItemMapper;

    @Test
    void toReviewSummaryList_weightMode_shouldShowWeightPercentInCategoryLabel() {
        TbProjectReviewItem root = reviewItem(1L, 0L, 1, "TECHNICAL", null, new BigDecimal("60"));
        TbProjectReviewItem leaf = reviewItem(2L, 1L, 2, "TECHNICAL", new BigDecimal("100"), null);
        leaf.setItemName("施工方案");
        leaf.setItemContent("完整性");

        Map<String, List<TbProjectReviewItem>> grouped = new LinkedHashMap<>();
        grouped.put("TECHNICAL", List.of(root, leaf));

        ReviewConfig config = ReviewConfig.defaultConfig();
        config.setScoreMode(ScoreMode.WEIGHT);

        DocumentDataAssembler assembler = new DocumentDataAssembler();
        List<Map<String, String>> rows = assembler.toReviewSummaryList(grouped, config);

        // 一级节点有子项时被跳过，仅叶子节点产出 1 行；权重模式下类别列显示"权重60%"
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("categoryName")).isEqualTo("技术标（权重60%）");
    }

    @Test
    void toReviewSummaryList_scoreMode_shouldShowCategoryScoreInCategoryLabel() {
        TbProjectReviewItem root = reviewItem(1L, 0L, 1, "TECHNICAL", null, null);
        TbProjectReviewItem leaf = reviewItem(2L, 1L, 2, "TECHNICAL", new BigDecimal("100"), null);

        Map<String, List<TbProjectReviewItem>> grouped = new LinkedHashMap<>();
        grouped.put("TECHNICAL", List.of(root, leaf));

        ReviewConfig config = ReviewConfig.defaultConfig(); // 默认 SCORE

        DocumentDataAssembler assembler = new DocumentDataAssembler();
        List<Map<String, String>> rows = assembler.toReviewSummaryList(grouped, config);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("categoryName")).isEqualTo("技术标（100分）");
    }

    private TbProjectReviewItem reviewItem(Long id, Long parentId, int level, String reviewType,
                                           BigDecimal score, BigDecimal weight) {
        TbProjectReviewItem item = new TbProjectReviewItem();
        item.setId(id);
        item.setParentId(parentId);
        item.setLevel(level);
        item.setReviewType(reviewType);
        item.setScore(score);
        item.setWeight(weight);
        item.setSortOrder(0);
        return item;
    }

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
