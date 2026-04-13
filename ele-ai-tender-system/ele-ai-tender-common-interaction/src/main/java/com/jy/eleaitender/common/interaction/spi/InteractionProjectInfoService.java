package com.jy.eleaitender.common.interaction.spi;

import com.jy.eleaitender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eleaitender.common.interaction.dto.ProjectBasicInfoResponse;

/**
 * 项目基本信息查询SPI
 */
public interface InteractionProjectInfoService {

    ProjectBasicInfoResponse queryProjectBasicInfo(ProjectBasicInfoQueryRequest request);
}
