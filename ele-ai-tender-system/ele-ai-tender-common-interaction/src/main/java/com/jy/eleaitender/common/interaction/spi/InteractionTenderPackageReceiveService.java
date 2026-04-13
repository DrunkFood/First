package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.TenderPackageCallbackRequest;

/**
 * 电子标招标文件接收SPI
 */
public interface InteractionTenderPackageReceiveService {

    void receive(TenderPackageCallbackRequest request);
}
