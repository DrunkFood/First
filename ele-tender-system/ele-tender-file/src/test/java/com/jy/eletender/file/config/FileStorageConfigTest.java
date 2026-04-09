package com.jy.eletender.file.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageConfigTest {

    @Test
    void shouldUseEleTenderPathAndAllowTenderDocumentPackageSuffixByDefault() {
        FileStorageConfig config = new FileStorageConfig();

        assertThat(config.getBasePath()).contains("ele-tender");
        assertThat(config.getAllowedTypeList()).contains(".pdf");
        assertThat(config.getAllowedTypeList()).contains(".HzctZbs");
        assertThat(config.getAllowedTypeList()).contains(".HzctTbs");
    }
}
