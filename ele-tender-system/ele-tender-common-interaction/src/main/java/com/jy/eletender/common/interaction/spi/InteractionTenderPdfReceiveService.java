package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest;

/**
 * 招标文件PDF接收SPI
 */
public interface InteractionTenderPdfReceiveService {

    void receive(TenderPdfCallbackRequest request);
}
