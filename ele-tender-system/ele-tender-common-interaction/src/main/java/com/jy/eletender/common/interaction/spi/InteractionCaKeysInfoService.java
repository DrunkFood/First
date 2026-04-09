package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.CaKeysInfoResponse;

/**
 * 开标 CA 锁信息查询 SPI。
 */
public interface InteractionCaKeysInfoService {

    CaKeysInfoResponse queryCaKeysInfo(CaKeysInfoQueryRequest request);
}
