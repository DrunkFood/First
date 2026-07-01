package com.jy.eleaitender.core.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.common.enums.ReviewType;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiTaskResultSyncHandlerTest {

    @Test
    void parseReviewItemsFromResultShouldReadWeightOnLevel1Node() {
        String json = """
                {"reviewItems":[
                  {"name":"Technical Review","level":1,"weight":60,"children":[
                    {"name":"Implementation Plan","level":2,"score":100,"subjectivity":"SUBJECTIVE","isRequired":false,"children":[]}
                  ]}
                ]}
                """;

        AiTaskResultSyncHandler handler = new AiTaskResultSyncHandler();
        ReflectionTestUtils.setField(handler, "objectMapper", new ObjectMapper());

        List<TbProjectReviewItem> items = handler.parseReviewItemsFromResult(json, 1L);

        TbProjectReviewItem level1 = items.stream().filter(i -> i.getLevel() == 1).findFirst().orElseThrow();
        assertThat(level1.getWeight()).isEqualByComparingTo("60");

        TbProjectReviewItem leaf = items.stream().filter(i -> i.getLevel() == 2).findFirst().orElseThrow();
        assertThat(leaf.getScore()).isEqualByComparingTo("100");
        assertThat(leaf.getWeight()).isNull();
    }

    @Test
    void syncReviewItemsShouldUseManualItemsWhenGenerateStandardIsDisabled() throws Exception {
        String aiResult = """
                {"reviewItems":[
                  {"name":"Compliance Review","level":1,"children":[
                    {"name":"AI Item","content":"AI Standard","level":2,"score":30,"children":[]}
                  ]}
                ]}
                """;
        String reviewConfig = """
                {"scoreMode":"SCORE","reviewTypes":[
                  {"reviewType":"COMPLIANCE","enabled":true,"generateStandard":false,"distinguishSubjectivity":true,
                   "manualItems":[
                     {"itemName":"Manual Review Item","itemContent":"Manual Standard","score":20,"subjectivity":"SUBJECTIVE",
                      "children":[{"itemName":"Manual Child Item","itemContent":"Manual Child Standard","score":5,"subjectivity":"OBJECTIVE"}]}
                   ]}
                ]}
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        AiTask task = new AiTask();
        task.setId(200L);
        task.setTaskType("REVIEW_ITEM_GENERATE");
        task.setStatus("COMPLETED");
        task.setBizId(100L);
        task.setResult(aiResult);
        task.setRequestParams("{\"reviewConfig\":" + objectMapper.valueToTree(reviewConfig) + "}");

        TbProjectReviewItemMapper reviewItemMapper = mock(TbProjectReviewItemMapper.class);
        when(reviewItemMapper.selectByProjectId(100L)).thenReturn(List.of());
        AtomicLong generatedId = new AtomicLong(1000L);
        doAnswer(invocation -> {
            TbProjectReviewItem item = invocation.getArgument(0);
            item.setId(generatedId.getAndIncrement());
            return 1;
        }).when(reviewItemMapper).insert(any(TbProjectReviewItem.class));

        AiTaskResultSyncHandler handler = new AiTaskResultSyncHandler();
        ReflectionTestUtils.setField(handler, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(handler, "reviewItemMapper", reviewItemMapper);

        handler.sync(task);

        ArgumentCaptor<TbProjectReviewItem> captor = ArgumentCaptor.forClass(TbProjectReviewItem.class);
        verify(reviewItemMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        List<TbProjectReviewItem> inserted = captor.getAllValues();

        assertThat(inserted).extracting(TbProjectReviewItem::getItemName)
                .containsExactly("Compliance Review", "Manual Review Item", "Manual Child Item");
        assertThat(inserted).extracting(TbProjectReviewItem::getItemName)
                .doesNotContain("AI Item");
        assertThat(inserted.get(1).getParentId()).isEqualTo(1000L);
        assertThat(inserted.get(2).getParentId()).isEqualTo(1001L);
        assertThat(inserted.get(1).getScore()).isEqualByComparingTo("20");
        assertThat(inserted.get(2).getSubjectivity()).isEqualTo("OBJECTIVE");
    }

    @Test
    void syncReviewItemsShouldScaleAiScoresToRemainingScoreWhenManualItemsUseScoreMode() throws Exception {
        String aiResult = """
                {"reviewItems":[
                  {"name":"%s","level":1,"children":[
                    {"name":"AI Technical Item","content":"AI Technical Standard","level":2,"score":100,"children":[]}
                  ]},
                  {"name":"%s","level":1,"children":[
                    {"name":"AI Commercial Item","content":"AI Commercial Standard","level":2,"score":100,"children":[]}
                  ]}
                ]}
                """.formatted(ReviewType.TECHNICAL.getLabel(), ReviewType.COMMERCIAL.getLabel());
        String reviewConfig = """
                {"scoreMode":"SCORE","reviewTypes":[
                  {"reviewType":"TECHNICAL","enabled":true,"generateStandard":false,
                   "manualItems":[{"itemName":"Manual Technical Item","itemContent":"Manual Standard","score":30}]},
                  {"reviewType":"COMMERCIAL","enabled":true,"generateStandard":true}
                ]}
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        AiTask task = new AiTask();
        task.setId(201L);
        task.setTaskType("REVIEW_ITEM_GENERATE");
        task.setStatus("COMPLETED");
        task.setBizId(101L);
        task.setResult(aiResult);
        task.setRequestParams("{\"reviewConfig\":" + objectMapper.valueToTree(reviewConfig) + "}");

        TbProjectReviewItemMapper reviewItemMapper = mock(TbProjectReviewItemMapper.class);
        when(reviewItemMapper.selectByProjectId(101L)).thenReturn(List.of());
        AtomicLong generatedId = new AtomicLong(2000L);
        doAnswer(invocation -> {
            TbProjectReviewItem item = invocation.getArgument(0);
            item.setId(generatedId.getAndIncrement());
            return 1;
        }).when(reviewItemMapper).insert(any(TbProjectReviewItem.class));

        AiTaskResultSyncHandler handler = new AiTaskResultSyncHandler();
        ReflectionTestUtils.setField(handler, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(handler, "reviewItemMapper", reviewItemMapper);

        handler.sync(task);

        ArgumentCaptor<TbProjectReviewItem> captor = ArgumentCaptor.forClass(TbProjectReviewItem.class);
        verify(reviewItemMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        List<TbProjectReviewItem> inserted = captor.getAllValues();

        TbProjectReviewItem manualItem = inserted.stream()
                .filter(item -> "Manual Technical Item".equals(item.getItemName()))
                .findFirst().orElseThrow();
        TbProjectReviewItem aiItem = inserted.stream()
                .filter(item -> "AI Commercial Item".equals(item.getItemName()))
                .findFirst().orElseThrow();

        assertThat(inserted).extracting(TbProjectReviewItem::getItemName)
                .doesNotContain("AI Technical Item");
        assertThat(manualItem.getScore()).isEqualByComparingTo("30");
        assertThat(aiItem.getScore()).isEqualByComparingTo("70");
    }
}
