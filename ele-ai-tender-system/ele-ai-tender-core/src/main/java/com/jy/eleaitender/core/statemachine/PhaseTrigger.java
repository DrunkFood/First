package com.jy.eleaitender.core.statemachine;

import com.jy.eleaitender.common.entity.core.AiProject;

/**
 * 阶段触发器接口
 * 每个编制阶段可注册一个触发器，控制阶段进入/离开/完成校验
 */
public interface PhaseTrigger {

    /**
     * 进入阶段时执行
     */
    default void onEnter(AiProject project) {
    }

    /**
     * 离开阶段时执行（完成当前阶段）
     */
    default void onExit(AiProject project) {
    }

    /**
     * 检查当前阶段是否可以完成（是否满足推进条件）
     */
    boolean canComplete(AiProject project);

    /**
     * 不能完成时的提示信息
     */
    default String getIncompleteMessage() {
        return "当前阶段尚未完成";
    }
}
