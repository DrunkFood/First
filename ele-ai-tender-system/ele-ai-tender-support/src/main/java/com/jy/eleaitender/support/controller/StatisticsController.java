package com.jy.eleaitender.support.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IStatisticsService;
import com.jy.eleaitender.support.vo.StatisticsOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计分析控制器
 */
@RestController
@RequestMapping("/api/v1/statistics")
@Tag(name = "统计分析")
public class StatisticsController {

    @Autowired
    private IStatisticsService statisticsService;

    @GetMapping("/overview")
    @Operation(summary = "获取统计概览数据")
    @RequireLogin
    public Result<StatisticsOverviewVO> overview() {
        return Result.success(statisticsService.getOverview());
    }
}
