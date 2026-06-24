package com.jy.eleaitender.interaction.autoconfigure.web;

import com.jy.eleaitender.common.interaction.constant.InteractionHeaderConstants;
import com.jy.eleaitender.common.interaction.enums.InteractionResponseCode;
import com.jy.eleaitender.common.interaction.exception.InteractionException;
import com.jy.eleaitender.common.interaction.util.InteractionSignatureUtil;
import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 交互接口签名校验拦截器
 */
public class InteractionSignatureInterceptor implements HandlerInterceptor {

    private final EleAiTenderInteractionProperties properties;

    public InteractionSignatureInterceptor(EleAiTenderInteractionProperties properties) {
        this.properties = properties;
    }

    @Override
    /**
     * 对业务系统暴露的交互接口统一做签名校验，避免每个 controller 重复处理请求头和验签逻辑。
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String appKey = request.getHeader(InteractionHeaderConstants.APP_KEY);
        String timestampText = request.getHeader(InteractionHeaderConstants.TIMESTAMP);
        String signature = request.getHeader(InteractionHeaderConstants.SIGNATURE);
        if (isBlank(appKey) || isBlank(timestampText) || isBlank(signature)) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "交互请求头参数不完整");
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampText);
        } catch (NumberFormatException e) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "时间戳格式错误", e);
        }
        // 只有 appKey 匹配当前 starter 配置且签名正确，才允许进入业务 SPI。
        if (!equals(properties.getAppKey(), appKey)
                || !InteractionSignatureUtil.verifySignature(appKey, timestamp, properties.getAppSecret(), signature)) {
            throw new InteractionException(InteractionResponseCode.SIGNATURE_ERROR);
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean equals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
