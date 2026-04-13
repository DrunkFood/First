package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.BidDecryptResultCallbackRequest;

/**
 * 投标文件解密结果回调接收SPI
 */
public interface InteractionBidDecryptResultReceiveService {

    void onDecryptResult(BidDecryptResultCallbackRequest request);
}
