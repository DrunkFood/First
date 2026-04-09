package com.jy.eletender.common.interaction.spi;

import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;

/**
 * 项目基本信息查询SPI
 */
public interface InteractionProjectInfoService {

    ProjectBasicInfoResponse queryProjectBasicInfo(ProjectBasicInfoQueryRequest request);
}
