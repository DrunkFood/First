package com.jy.eleaitender.interaction.autoconfigure.controller;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.dto.TenderPdfCallbackRequest;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.common.interaction.spi.InteractionTenderPdfReceiveService;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 招标文件PDF回传控制器
 */
@RestController
public class InteractionTenderPdfCallbackController {

    private final InteractionTenderPdfReceiveService tenderPdfReceiveService;
    private final InteractionEventLogger eventLogger;

    public InteractionTenderPdfCallbackController(InteractionTenderPdfReceiveService tenderPdfReceiveService,
                                                  InteractionEventLogger eventLogger) {
        this.tenderPdfReceiveService = tenderPdfReceiveService;
        this.eventLogger = eventLogger;
    }

    /**
     * 接收签章后的招标文件 PDF 回传。
     * 文件本体由业务系统再按 fileId 向文件服务拉取，这里只处理业务接收和结果记录。
     */
    @PostMapping(value = InteractionApiPaths.CALLBACK_TENDER_PDF,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<Void> receiveTenderPdf(@RequestBody TenderPdfCallbackRequest request) {
        Throwable error = null;
        InteractionResult<Void> result = null;
        try {
            InteractionValidationUtils.validateTenderPdfCallbackRequest(request);
            tenderPdfReceiveService.receive(request);
            result = InteractionResult.success();
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("callbacks/tender-pdf", request, result, error);
            }
        }
    }
}
