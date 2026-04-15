package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.support.SupMessage;
import com.jy.eleaitender.common.entity.support.SysOperationLog;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.support.mapper.*;
import com.jy.eleaitender.support.service.IStatisticsService;
import com.jy.eleaitender.support.vo.StatisticsOverviewVO;
import com.jy.eleaitender.support.vo.StatisticsOverviewVO.DailyStatItem;
import com.jy.eleaitender.support.vo.StatisticsOverviewVO.StatusDistItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析服务实现
 * 基于支撑中心及核心业务已有数据做 COUNT 统计
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService {

    /** 项目状态名称映射 */
    private static final Map<String, String> PROJECT_STATUS_NAMES = Map.of(
            "DRAFT", "草稿",
            "IN_PROGRESS", "编制中",
            "PENDING_DETECTION", "待检测",
            "DETECTING", "检测中",
            "DETECTION_PASSED", "检测通过",
            "DETECTION_FAILED", "检测未通过",
            "PUBLISHED", "已发布",
            "ARCHIVED", "已归档",
            "CANCELLED", "已取消"
    );

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysAccessSystemMapper accessSystemMapper;

    @Autowired
    private SysMainVersionMapper mainVersionMapper;

    @Autowired
    private AiProjectMapper projectMapper;

    @Autowired
    private AiRequirementMapper requirementMapper;

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

        // 支撑中心基础计数
        vo.setUserCount(userMapper.selectCount(null));
        vo.setRoleCount(roleMapper.selectCount(null));
        vo.setAccessSystemCount(accessSystemMapper.selectCount(null));
        vo.setVersionCount(mainVersionMapper.selectCount(null));

        // 核心业务计数
        vo.setProjectCount(projectMapper.selectCount(null));
        vo.setRequirementCount(requirementMapper.selectCount(null));
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

        // 今日新建项目数
        LambdaQueryWrapper<AiProject> todayProjectWrapper = new LambdaQueryWrapper<>();
        todayProjectWrapper.ge(AiProject::getCreateTime, todayStart.getTime());
        vo.setTodayProjectCount(projectMapper.selectCount(todayProjectWrapper));

        // 当前用户未读消息数
        Long userId = SecurityContextHolder.getUserId();
        if (userId != null) {
            LambdaQueryWrapper<SupMessage> msgWrapper = new LambdaQueryWrapper<>();
            msgWrapper.eq(SupMessage::getUserId, userId)
                      .eq(SupMessage::getIsRead, 0);
            vo.setUnreadMessageCount(messageMapper.selectCount(msgWrapper));
        }

        // 近7天每日操作统计（单次查询优化）
        vo.setDailyOperations(getLast7DaysOperations(todayStart));

        // 项目状态分布（同时用于计算待办事项）
        List<StatusDistItem> statusDist = getProjectStatusDist();
        vo.setProjectStatusDist(statusDist);

        // 从状态分布中提取待办事项计数
        Map<String, Long> statusCountMap = statusDist.stream()
                .collect(Collectors.toMap(StatusDistItem::getStatus, StatusDistItem::getCount, Long::sum));
        vo.setPendingDetectionCount(statusCountMap.getOrDefault("PENDING_DETECTION", 0L));
        vo.setDetectionFailedCount(statusCountMap.getOrDefault("DETECTION_FAILED", 0L));
        vo.setInProgressCount(statusCountMap.getOrDefault("IN_PROGRESS", 0L));

        return vo;
    }

    /**
     * 查询近7天每日操作数量
     * 优化：单次查询获取7天数据，内存按日期分组
     */
    private List<DailyStatItem> getLast7DaysOperations(Calendar todayStart) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 计算7天前的起始时间
        Calendar sevenDaysAgo = (Calendar) todayStart.clone();
        sevenDaysAgo.add(Calendar.DAY_OF_MONTH, -6);

        // 单次查询7天内所有操作日志的创建时间
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SysOperationLog::getCreateTime, sevenDaysAgo.getTime())
               .select(SysOperationLog::getCreateTime);
        List<SysOperationLog> logs = operationLogMapper.selectList(wrapper);

        // 按日期分组计数
        Map<String, Long> countByDate = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> sdf.format(log.getCreateTime()),
                        Collectors.counting()
                ));

        // 填充7天数据（无数据的日期补0）
        List<DailyStatItem> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            Calendar day = Calendar.getInstance();
            day.add(Calendar.DAY_OF_MONTH, -i);
            String dateStr = sdf.format(day.getTime());

            DailyStatItem item = new DailyStatItem();
            item.setDate(dateStr);
            item.setCount(countByDate.getOrDefault(dateStr, 0L));
            result.add(item);
        }
        return result;
    }

    /**
     * 查询项目状态分布
     */
    private List<StatusDistItem> getProjectStatusDist() {
        // 查询所有非删除项目的状态
        LambdaQueryWrapper<AiProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AiProject::getStatus);
        List<AiProject> projects = projectMapper.selectList(wrapper);

        // 按状态分组计数
        Map<String, Long> countByStatus = projects.stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(
                        AiProject::getStatus,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        // 转换为VO列表
        List<StatusDistItem> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : countByStatus.entrySet()) {
            StatusDistItem item = new StatusDistItem();
            item.setStatus(entry.getKey());
            item.setStatusName(PROJECT_STATUS_NAMES.getOrDefault(entry.getKey(), entry.getKey()));
            item.setCount(entry.getValue());
            result.add(item);
        }
        return result;
    }
}
