package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eleaitender.common.interaction.dto.BidRecordSchemeResponse;

/**
 * 标录方案查询SPI
 */
public interface InteractionBidRecordSchemeService {

    BidRecordSchemeResponse queryBidRecordScheme(BidRecordSchemeQueryRequest request);
}
