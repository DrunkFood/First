package com.jy.eleaitender.core.engine;

import com.jy.eleaitender.common.dto.ReviewConfig;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.ScoreMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentDataAssembler 评审汇总表构建单测，聚焦权重模式/分值模式类别标签差异。
 * toReviewSummaryList 为纯函数（仅依赖静态 typeLabels + 入参），无需注入 mapper。
 */
class DocumentDataAssemblerTest {

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
}
