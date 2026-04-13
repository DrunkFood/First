package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eleaitender.common.interaction.dto.CaKeysInfoResponse;

/**
 * 开标 CA 锁信息查询 SPI。
 */
public interface InteractionCaKeysInfoService {

    CaKeysInfoResponse queryCaKeysInfo(CaKeysInfoQueryRequest request);
}
