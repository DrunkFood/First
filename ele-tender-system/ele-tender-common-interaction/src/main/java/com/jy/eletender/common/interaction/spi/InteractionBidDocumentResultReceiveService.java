package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.BidDocumentResultCallbackRequest;

/**
 * 投标文件预存结果回调接收SPI
 */
public interface InteractionBidDocumentResultReceiveService {

    void onBidDocumentResult(BidDocumentResultCallbackRequest request);
}
