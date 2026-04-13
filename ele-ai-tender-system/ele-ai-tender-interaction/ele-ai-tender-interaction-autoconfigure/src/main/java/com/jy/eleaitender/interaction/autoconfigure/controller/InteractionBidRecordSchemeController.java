package com.jy.eleaitender.interaction.autoconfigure.controller;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import com.jy.eleaitender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eleaitender.common.interaction.dto.BidRecordSchemeResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.spi.InteractionBidRecordSchemeService;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标录方案查询控制器
 */
@RestController
public class InteractionBidRecordSchemeController {

    private final InteractionBidRecordSchemeService bidRecordSchemeService;
    private final InteractionEventLogger eventLogger;

    public InteractionBidRecordSchemeController(InteractionBidRecordSchemeService bidRecordSchemeService,
                                                InteractionEventLogger eventLogger) {
        this.bidRecordSchemeService = bidRecordSchemeService;
        this.eventLogger = eventLogger;
    }

    /**
     * 查询开标标录方案。
     * 交互层只负责统一协议和校验，具体结构由业务系统返回并透传给编制系统。
     */
    @PostMapping(value = InteractionApiPaths.BID_RECORD_SCHEME,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<BidRecordSchemeResponse> queryBidRecordScheme(@RequestBody BidRecordSchemeQueryRequest request) {
        Throwable error = null;
        InteractionResult<BidRecordSchemeResponse> result = null;
        try {
            InteractionValidationUtils.validateBidRecordSchemeQueryRequest(request);
            result = InteractionResult.success(bidRecordSchemeService.queryBidRecordScheme(request));
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("bid-record-schemes/query", request, result, error);
            }
        }
    }
}
