package com.jy.eleaitender.common.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectCategoryTest {

    @Test
    void shouldExposeCurrentProjectCategoryCodesAndLabels() {
        assertThat(Arrays.stream(ProjectCategory.values()).map(ProjectCategory::getCode).toList())
                .containsExactly("SMALL_TRADE", "GOVERNMENT_PROCUREMENT", "COMPREHENSIVE_TRADE");

        assertThat(ProjectCategory.fromCode("SMALL_TRADE").getLabel()).isEqualTo("小额交易");
        assertThat(ProjectCategory.fromCode("GOVERNMENT_PROCUREMENT").getLabel()).isEqualTo("政府采购");
        assertThat(ProjectCategory.fromCode("COMPREHENSIVE_TRADE").getLabel()).isEqualTo("综合交易");
    }

}
