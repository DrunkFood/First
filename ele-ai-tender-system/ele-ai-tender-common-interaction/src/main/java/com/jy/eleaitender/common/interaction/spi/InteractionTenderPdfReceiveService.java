package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.TenderPdfCallbackRequest;

/**
 * 招标文件PDF接收SPI
 */
public interface InteractionTenderPdfReceiveService {

    void receive(TenderPdfCallbackRequest request);
}
