package com.jy.eleaitender.interaction.autoconfigure.controller;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eleaitender.common.interaction.dto.CaKeysInfoResponse;
import com.jy.eleaitender.common.interaction.spi.InteractionCaKeysInfoService;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开标 CA 锁信息查询控制器。
 */
@RestController
public class InteractionCaKeysInfoController {

    private final InteractionCaKeysInfoService caKeysService;
    private final InteractionEventLogger eventLogger;

    public InteractionCaKeysInfoController(InteractionCaKeysInfoService caKeysService,
                                             InteractionEventLogger eventLogger) {
        this.caKeysService = caKeysService;
        this.eventLogger = eventLogger;
    }

    /**
     * 查询开标时有效的 CA 锁信息。
     */
    @PostMapping(value = InteractionApiPaths.CA_KEYS_INFO,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<CaKeysInfoResponse> queryCaKeysInfo(@RequestBody CaKeysInfoQueryRequest request) {
        Throwable error = null;
        InteractionResult<CaKeysInfoResponse> result = null;
        try {
            InteractionValidationUtils.validateCaKeysInfoQueryRequest(request);
            result = InteractionResult.success(caKeysService.queryCaKeysInfo(request));
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("ca-keys/query", request, result, error);
            }
        }
    }
}
