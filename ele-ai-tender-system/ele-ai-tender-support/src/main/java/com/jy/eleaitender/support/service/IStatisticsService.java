package com.jy.eleaitender.support.service;

import com.jy.eleaitender.support.vo.StatisticsOverviewVO;

/**
 * 统计分析服务接口
 */
public interface IStatisticsService {

    /**
     * 获取统计概览数据
     *
     * @return 概览 VO
     */
    StatisticsOverviewVO getOverview();
}
