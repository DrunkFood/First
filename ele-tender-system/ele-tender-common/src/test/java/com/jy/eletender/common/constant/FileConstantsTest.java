package com.jy.eletender.common.constant;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileConstantsTest {

    @ParameterizedTest
    @ValueSource(strings = {".HzctZbs", ".HzctTbs", ".CustomSuffix", ".A", ".abc123"})
    void validateDocumentSuffix_shouldAcceptValidSuffixes(String suffix) {
        assertThat(FileConstants.validateDocumentSuffix(suffix)).isEqualTo(suffix);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void validateDocumentSuffix_shouldReturnNullForBlank(String suffix) {
        assertThat(FileConstants.validateDocumentSuffix(suffix)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "HzctZbs",
            "..",
            "../etc",
            ".Hzct/Zbs",
            ".Hzct\\Zbs",
            ".Hzct Zbs",
            ".Hzct.Zbs",
            ".",
            ".a_b",
            ".a-b",
            ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    })
    void validateDocumentSuffix_shouldRejectInvalidSuffixes(String suffix) {
        assertThatThrownBy(() -> FileConstants.validateDocumentSuffix(suffix))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件后缀格式不合法");
    }
}
