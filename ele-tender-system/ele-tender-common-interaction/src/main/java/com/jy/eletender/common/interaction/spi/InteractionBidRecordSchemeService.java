package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeResponse;

/**
 * 标录方案查询SPI
 */
public interface InteractionBidRecordSchemeService {

    BidRecordSchemeResponse queryBidRecordScheme(BidRecordSchemeQueryRequest request);
}
