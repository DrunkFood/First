package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.BidDocumentResultCallbackRequest;

/**
 * 投标文件预存结果回调接收SPI
 */
public interface InteractionBidDocumentResultReceiveService {

    void onBidDocumentResult(BidDocumentResultCallbackRequest request);
}
