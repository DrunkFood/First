package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.ai.AiModelConfig;
import com.jy.eleaitender.common.entity.support.SupMessage;
import com.jy.eleaitender.common.entity.support.SupPolicyFile;
import com.jy.eleaitender.common.entity.support.SysOperationLog;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.support.mapper.*;
import com.jy.eleaitender.support.service.IStatisticsService;
import com.jy.eleaitender.support.vo.StatisticsOverviewVO;
import com.jy.eleaitender.support.vo.StatisticsOverviewVO.DailyStatItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 统计分析服务实现
 * 第一阶段: 基于支撑中心已有数据做简单 COUNT 统计
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private TemplateConfigMapper templateMapper;

    @Autowired
    private KnowledgeConfigMapper knowledgeMapper;

    @Autowired
    private ModelConfigMapper modelConfigMapper;

    @Autowired
    private SysOperationLogMapper operationLogMapper;

    @Autowired
    private PolicyFileMapper policyFileMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public StatisticsOverviewVO getOverview() {
        StatisticsOverviewVO vo = new StatisticsOverviewVO();

        // 基础计数
        vo.setUserCount(userMapper.selectCount(null));
        vo.setTemplateCount(templateMapper.selectCount(null));
        vo.setKnowledgeCount(knowledgeMapper.selectCount(null));
        vo.setModelConfigCount(modelConfigMapper.selectCount(null));
        vo.setOperationLogCount(operationLogMapper.selectCount(null));
        vo.setPolicyFileCount(policyFileMapper.selectCount(null));

        // 今日操作次数
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);

        LambdaQueryWrapper<SysOperationLog> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(SysOperationLog::getCreateTime, todayStart.getTime());
        vo.setTodayOperationCount(operationLogMapper.selectCount(todayWrapper));

        // 当前用户未读消息数
        Long userId = SecurityContextHolder.getUserId();
        if (userId != null) {
            LambdaQueryWrapper<SupMessage> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(SupMessage::getUserId, userId)
                      .eq(SupMessage::getIsRead, 0);
            vo.setUnreadMessageCount(messageMapper.selectCount(msgWrapper));
        }

        // 近7天每日操作统计
        vo.setDailyOperations(getLast7DaysOperations());

        return vo;
    }

    /**
     * 查询近7天每日操作数量
     */
    private List<DailyStatItem> getLast7DaysOperations() {
        List<DailyStatItem> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 6; i >= 0; i--) {
            Calendar dayStart = Calendar.getInstance();
            dayStart.add(Calendar.DAY_OF_MONTH, -i);
            dayStart.set(Calendar.HOUR_OF_DAY, 0);
            dayStart.set(Calendar.MINUTE, 0);
            dayStart.set(Calendar.SECOND, 0);
            dayStart.set(Calendar.MILLISECOND, 0);

            Calendar dayEnd = (Calendar) dayStart.clone();
            dayEnd.add(Calendar.DAY_OF_MONTH, 1);

            LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(SysOperationLog::getCreateTime, dayStart.getTime())
                   .lt(SysOperationLog::getCreateTime, dayEnd.getTime());

            DailyStatItem item = new DailyStatItem();
            item.setDate(sdf.format(dayStart.getTime()));
            item.setCount(operationLogMapper.selectCount(wrapper));
            result.add(item);
        }
        return result;
    }
}
