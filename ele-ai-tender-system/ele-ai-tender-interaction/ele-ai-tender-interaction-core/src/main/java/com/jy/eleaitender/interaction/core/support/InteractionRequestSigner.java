package com.jy.eleaitender.interaction.core.support;

import com.jy.eleaitender.common.interaction.constant.InteractionHeaderConstants;
import com.jy.eleaitender.common.interaction.util.InteractionSignatureUtil;
import com.jy.eleaitender.interaction.core.properties.EleTenderInteractionProperties;
import org.springframework.http.HttpHeaders;

/**
 * 交互请求签名器
 */
public class InteractionRequestSigner {

    private final EleTenderInteractionProperties properties;

    public InteractionRequestSigner(EleTenderInteractionProperties properties) {
        this.properties = properties;
    }

    public HttpHeaders sign(HttpHeaders headers) {
        long timestamp = System.currentTimeMillis();
        headers.set(InteractionHeaderConstants.APP_KEY, properties.getAppKey());
        headers.set(InteractionHeaderConstants.TIMESTAMP, String.valueOf(timestamp));
        headers.set(InteractionHeaderConstants.SIGNATURE,
                InteractionSignatureUtil.generateSignature(properties.getAppKey(), timestamp, properties.getAppSecret()));
        return headers;
    }
}
