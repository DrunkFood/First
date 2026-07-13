package com.jy.eleaitender.interaction.autoconfigure.web;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 交互 MVC 配置
 */
public class InteractionAiWebMvcConfigurer implements WebMvcConfigurer {

    private final InteractionAiSignatureInterceptor signatureInterceptor;

    public InteractionAiWebMvcConfigurer(InteractionAiSignatureInterceptor signatureInterceptor) {
        this.signatureInterceptor = signatureInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signatureInterceptor)
                .addPathPatterns(InteractionApiPaths.BASE + "/**");
    }
}
