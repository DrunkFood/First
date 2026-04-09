package com.jy.eletender.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoEnumsTest {

    @Test
    void shouldExposeDecryptArtifactLifecycleStatuses() {
        assertThat(DecryptArtifactStatus.values())
                .containsExactly(
                        DecryptArtifactStatus.PENDING,
                        DecryptArtifactStatus.PROCESSING,
                        DecryptArtifactStatus.SUCCESS,
                        DecryptArtifactStatus.FAILED
                );
    }

    @Test
    void shouldExposeDecryptRequestLifecycleStatuses() {
        assertThat(DecryptRequestStatus.values())
                .containsExactly(
                        DecryptRequestStatus.PENDING,
                        DecryptRequestStatus.PROCESSING,
                        DecryptRequestStatus.SUCCESS,
                        DecryptRequestStatus.FAILED
                );
    }

    @Test
    void shouldExposeCallbackStatuses() {
        assertThat(CallbackStatus.values())
                .containsExactly(
                        CallbackStatus.NOT_CALLED,
                        CallbackStatus.SUCCESS,
                        CallbackStatus.FAILED
                );
    }

    @Test
    void shouldExposeBidDocumentUploadStatuses() {
        assertThat(BidDocumentUploadStatus.values())
                .containsExactly(
                        BidDocumentUploadStatus.SUCCESS,
                        BidDocumentUploadStatus.FAILED,
                        BidDocumentUploadStatus.PROCESSING
                );
    }
}
