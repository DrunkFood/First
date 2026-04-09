package com.jy.eletender.interaction.autoconfigure.controller;

import com.jy.eletender.common.interaction.constant.InteractionApiPaths;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;
import com.jy.eletender.common.interaction.spi.InteractionEventLogger;
import com.jy.eletender.common.interaction.spi.InteractionProjectInfoService;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 项目基本信息查询控制器
 */
@RestController
public class InteractionProjectInfoController {

    private final InteractionProjectInfoService projectInfoService;
    private final InteractionEventLogger eventLogger;

    public InteractionProjectInfoController(InteractionProjectInfoService projectInfoService,
                                            InteractionEventLogger eventLogger) {
        this.projectInfoService = projectInfoService;
        this.eventLogger = eventLogger;
    }

    /**
     * 查询项目基本信息。
     * TenderDocument 入口和页面同步都会依赖这条能力，因此协议字段必须保持稳定。
     */
    @PostMapping(value = InteractionApiPaths.PROJECT_BASIC_INFO,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InteractionResult<ProjectBasicInfoResponse> queryProjectBasicInfo(@RequestBody ProjectBasicInfoQueryRequest request) {
        Throwable error = null;
        InteractionResult<ProjectBasicInfoResponse> result = null;
        try {
            InteractionValidationUtils.validateProjectBasicInfoQueryRequest(request);
            result = InteractionResult.success(projectInfoService.queryProjectBasicInfo(request));
            return result;
        } catch (RuntimeException e) {
            error = e;
            throw e;
        } finally {
            if (eventLogger != null) {
                eventLogger.logInbound("projects/basic-info", request, result, error);
            }
        }
    }
}
