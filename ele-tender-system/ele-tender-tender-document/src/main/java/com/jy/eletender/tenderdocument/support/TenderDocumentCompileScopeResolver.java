package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;

/**
 * 根据同步结果解析编制粒度。
 */
public final class TenderDocumentCompileScopeResolver {

    private TenderDocumentCompileScopeResolver() {
    }

    /**
     * 根据项目类型推导编制粒度。
     * 公开招标按项目编制，邀请招标按标段编制。
     */
    public static String resolve(InteractionProjectType projectType) {
        if (projectType == null) {
            throw new IllegalArgumentException("基本信息同步结果缺少projectType");
        }
        if (InteractionProjectType.INVITE == projectType) {
            return TenderDocumentScopeType.TENDER.name();
        }
        if (InteractionProjectType.PUBLIC == projectType) {
            return TenderDocumentScopeType.PROJECT.name();
        }
        throw new IllegalArgumentException("无法根据projectType推导compileScope: " + projectType.name());
    }
}
