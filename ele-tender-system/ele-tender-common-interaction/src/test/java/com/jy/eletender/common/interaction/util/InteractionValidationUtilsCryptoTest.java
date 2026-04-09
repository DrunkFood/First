package com.jy.eletender.common.interaction.util;

import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentResultCallbackRequest;
import com.jy.eletender.common.interaction.exception.InteractionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class InteractionValidationUtilsCryptoTest {

    @Test
    void shouldValidateBidDocumentPushRequest() {
        BidDocumentPushRequest request = new BidDocumentPushRequest();
        request.setFileId(101L);
        request.setFileSha256("abc123");

        InteractionValidationUtils.validateBidDocumentPushRequest(request);
    }

    @Test
    void shouldRejectBidDocumentPushRequestWhenSha256Missing() {
        BidDocumentPushRequest request = new BidDocumentPushRequest();
        request.setFileId(101L);

        assertThrows(InteractionException.class, () -> InteractionValidationUtils.validateBidDocumentPushRequest(request));
    }

    @Test
    void shouldValidateBidDecryptSubmitRequest() {
        BidDecryptSubmitRequest request = new BidDecryptSubmitRequest();
        request.setProjectId("P1");
        request.setTenderId("T1");
        request.setBidRecordId("B1");
        request.setBidderPwdStr("pwd");
        request.setFileSha256("sha");

        InteractionValidationUtils.validateBidDecryptSubmitRequest(request);
    }

    @Test
    void shouldValidateBidDocumentResultCallbackRequest() {
        BidDocumentResultCallbackRequest request = new BidDocumentResultCallbackRequest();
        request.setFileId(101L);
        request.setUploadResult("SUCCESS");

        InteractionValidationUtils.validateBidDocumentResultCallbackRequest(request);
    }

    @Test
    void shouldValidateBidDecryptResultCallbackRequest() {
        BidDecryptResultCallbackRequest request = new BidDecryptResultCallbackRequest();
        request.setBidRecordId("B1");
        request.setProjectId("P1");
        request.setTenderId("T1");
        request.setStatus("SUCCESS");
        request.setBidRecordData("{}");

        InteractionValidationUtils.validateBidDecryptResultCallbackRequest(request);
    }
}
