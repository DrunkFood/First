package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiReviewItem;
import com.jy.eleaitender.core.mapper.AiReviewItemMapper;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 评审项设置阶段触发器
 * 校验项目下已存在评审项
 */
@Component
public class ReviewItemTrigger implements PhaseTrigger {

    @Autowired
    private AiReviewItemMapper reviewItemMapper;

    @Override
    public boolean canComplete(AiProject project) {
        List<AiReviewItem> items = reviewItemMapper.selectByProjectId(project.getId());
        return items != null && !items.isEmpty();
    }

    @Override
    public String getIncompleteMessage() {
        return "请先设置评审项";
    }
}
