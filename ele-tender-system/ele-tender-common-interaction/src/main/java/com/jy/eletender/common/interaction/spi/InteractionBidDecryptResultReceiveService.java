package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.BidDecryptResultCallbackRequest;

/**
 * 投标文件解密结果回调接收SPI
 */
public interface InteractionBidDecryptResultReceiveService {

    void onDecryptResult(BidDecryptResultCallbackRequest request);
}
