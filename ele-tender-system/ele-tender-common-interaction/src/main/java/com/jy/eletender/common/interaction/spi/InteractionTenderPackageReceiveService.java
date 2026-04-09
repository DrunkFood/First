package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest;

/**
 * 电子标招标文件接收SPI
 */
public interface InteractionTenderPackageReceiveService {

    void receive(TenderPackageCallbackRequest request);
}
