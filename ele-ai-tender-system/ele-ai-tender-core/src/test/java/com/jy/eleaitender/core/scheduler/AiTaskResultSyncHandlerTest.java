package com.jy.eleaitender.core.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiTaskResultSyncHandler 评审项结果解析单测，聚焦权重模式下
 * 一级分类节点 weight 字段是否被正确读取持久化。
 * parseReviewItemsFromResult 仅依赖 objectMapper + final parentMap，无需 DB。
 */
class AiTaskResultSyncHandlerTest {

    @Test
    void parseReviewItemsFromResult_shouldReadWeightOnLevel1Node() {
        String json = """
                {"reviewItems":[
                  {"name":"技术标评审","level":1,"weight":60,"children":[
                    {"name":"施工方案","level":2,"score":100,"subjectivity":"SUBJECTIVE","isRequired":false,"children":[]}
                  ]}
                ]}
                """;

        AiTaskResultSyncHandler handler = new AiTaskResultSyncHandler();
        ReflectionTestUtils.setField(handler, "objectMapper", new ObjectMapper());

        List<TbProjectReviewItem> items = handler.parseReviewItemsFromResult(json, 1L);

        // 一级分类节点应携带权重%
        TbProjectReviewItem level1 = items.stream().filter(i -> i.getLevel() == 1).findFirst().orElseThrow();
        assertThat(level1.getWeight()).isEqualByComparingTo("60");
        assertThat(level1.getReviewType()).isEqualTo("TECHNICAL");

        // 叶子节点存类型内分值，weight 应为 null
        TbProjectReviewItem leaf = items.stream().filter(i -> i.getLevel() == 2).findFirst().orElseThrow();
        assertThat(leaf.getScore()).isEqualByComparingTo("100");
        assertThat(leaf.getWeight()).isNull();
    }
}
