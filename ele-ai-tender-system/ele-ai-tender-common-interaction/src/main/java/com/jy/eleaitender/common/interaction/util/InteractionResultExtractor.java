package com.jy.eleaitender.common.interaction.util;

import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.enums.InteractionResponseCode;
import com.jy.eleaitender.common.interaction.exception.InteractionException;

/**
 * 交互结果提取工具 — 从 InteractionResult 中提取 data，失败时抛出 InteractionException。
 */
public final class InteractionResultExtractor {

    private InteractionResultExtractor() {
    }

    public static <T> T extractData(InteractionResult<T> result, String defaultMessage) {
        if (result == null) {
            throw new InteractionException(InteractionResponseCode.FAIL, defaultMessage);
        }
        if (!result.isSuccess() || result.getData() == null) {
            throw new InteractionException(result.getCode(), result.getMessage());
        }
        return result.getData();
    }
}
