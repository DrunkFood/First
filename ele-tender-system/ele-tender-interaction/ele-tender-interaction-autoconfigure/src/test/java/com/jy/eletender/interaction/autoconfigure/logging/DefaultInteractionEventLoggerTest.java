package com.jy.eletender.interaction.autoconfigure.logging;

import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class DefaultInteractionEventLoggerTest {

    @Test
    void shouldLogBizContextForInboundRequest(CapturedOutput output) {
        MDC.put("traceId", "trace-001");
        try {
            TenderPdfCallbackRequest request = new TenderPdfCallbackRequest();
            request.setBizType(Integer.valueOf(1));
            request.setBizId("BIZ-1");
            request.setProjectId("P-1");
            request.setTenderId("T-1");
            request.setFileId(Long.valueOf(1001L));
            request.setFileName("招标文件.pdf");

            new DefaultInteractionEventLogger().logInbound("callbacks/tender-pdf", request, InteractionResult.success(), null);

            assertThat(output.getOut())
                    .contains("INTERACTION IN")
                    .contains("trace-001")
                    .contains("bizType=1")
                    .contains("bizId=BIZ-1")
                    .contains("projectId=P-1")
                    .contains("tenderId=T-1")
                    .contains("fileId=1001")
                    .contains("fileName=招标文件.pdf");
        } finally {
            MDC.remove("traceId");
        }
    }
}
