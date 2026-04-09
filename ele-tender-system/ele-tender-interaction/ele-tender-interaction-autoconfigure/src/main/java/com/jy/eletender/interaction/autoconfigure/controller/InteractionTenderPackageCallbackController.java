package com.jy.eletender.interaction.autoconfigure.controller;

import com.jy.eletender.common.interaction.constant.InteractionApiPaths;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest;
import com.jy.eletender.common.interaction.spi.InteractionEventLogger;
import com.jy.eletender.common.interaction.spi.InteractionTenderPackageReceiveService;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 电子标招标文件回传控制器
 */
@RestController
public class InteractionTenderPackageCallbackController {

    private final InteractionTenderPackageReceiveService tenderPackageReceiveService;
    private final InteractionEventLogger eventLogger;

    public InteractionTenderPackageCallbackController(InteractionTenderPackageReceiveService tenderPackageReceiveService,
                                                      InteractionEventLogger eventLogger) {
        this.tenderPackageReceiveService = tenderPackageReceiveService;
        this.eventLogger = eventLogger;
    }

    /**
     * 接收电子标招标文件数据包回传。
     * 与 PDF 回传独立留痕，便于业务系统按文件类型分别处理与重试。
     */
    @PostMapping(value = InteractionApiPaths.CALLBACK_TENDER_PACKAGE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<Void> receiveTenderPackage(@RequestBody TenderPackageCallbackRequest request) {
        Throwable error = null;
        InteractionResult<Void> result = null;
        try {
            InteractionValidationUtils.validateTenderPackageCallbackRequest(request);
            tenderPackageReceiveService.receive(request);
            result = InteractionResult.success();
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("callbacks/tender-package", request, result, error);
            }
        }
    }
}
