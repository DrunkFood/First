package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenderDocumentCompileScopeResolverTest {

    @Test
    void shouldResolveByProjectType() {
        assertThat(TenderDocumentCompileScopeResolver.resolve(InteractionProjectType.INVITE))
                .isEqualTo(TenderDocumentScopeType.TENDER.name());
        assertThat(TenderDocumentCompileScopeResolver.resolve(InteractionProjectType.PUBLIC))
                .isEqualTo(TenderDocumentScopeType.PROJECT.name());
    }

    @Test
    void shouldRejectWhenProjectTypeMissing() {
        assertThatThrownBy(() -> TenderDocumentCompileScopeResolver.resolve((InteractionProjectType) null))
                .hasMessageContaining("projectType");
    }
}
