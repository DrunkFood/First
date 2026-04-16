package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.AiModelConfig;
import com.jy.eleaitender.common.entity.support.SupModelRouteRule;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.ModelConfigMapper;
import com.jy.eleaitender.support.mapper.ModelRouteRuleMapper;
import com.jy.eleaitender.support.service.IModelRouteRuleService;
import com.jy.eleaitender.support.vo.ModelRouteRuleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模型路由规则服务实现
 */
@Service
public class ModelRouteRuleServiceImpl implements IModelRouteRuleService {

    @Autowired
    private ModelRouteRuleMapper routeRuleMapper;

    @Autowired
    private ModelConfigMapper modelConfigMapper;

    @Override
    public Page<ModelRouteRuleVO> getPage(Integer pageNum, Integer pageSize, String usageScenario) {
        Page<SupModelRouteRule> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SupModelRouteRule> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(usageScenario)) {
            wrapper.eq(SupModelRouteRule::getUsageScenario, usageScenario);
        }
        wrapper.orderByAsc(SupModelRouteRule::getPriority);

        Page<SupModelRouteRule> resultPage = routeRuleMapper.selectPage(page, wrapper);

        // 转换为 VO，填充模型名称
        Page<ModelRouteRuleVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        voPage.setRecords(convertToVO(resultPage.getRecords()));
        return voPage;
    }

    @Override
    public List<SupModelRouteRule> getActiveRules(String usageScenario) {
        LambdaQueryWrapper<SupModelRouteRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupModelRouteRule::getUsageScenario, usageScenario)
               .eq(SupModelRouteRule::getIsActive, 1)
               .orderByAsc(SupModelRouteRule::getPriority);
        return routeRuleMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public SupModelRouteRule create(SupModelRouteRule rule) {
        if (rule.getIsActive() == null) {
            rule.setIsActive(1);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(0);
        }
        routeRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional
    public void update(SupModelRouteRule rule) {
        routeRuleMapper.updateById(rule);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        routeRuleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setActive(Long id, Integer isActive) {
        SupModelRouteRule rule = routeRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResponseCode.MODEL_ROUTE_NOT_FOUND);
        }
        rule.setIsActive(isActive);
        routeRuleMapper.updateById(rule);
    }

    /**
     * 将实体列表转换为 VO，填充模型名称
     */
    private List<ModelRouteRuleVO> convertToVO(List<SupModelRouteRule> rules) {
        if (rules.isEmpty()) {
            return List.of();
        }

        // 收集所有模型ID
        Set<Long> modelIds = rules.stream()
                .flatMap(r -> {
                    java.util.stream.Stream.Builder<Long> builder = java.util.stream.Stream.builder();
                    if (r.getPrimaryModelId() != null) builder.add(r.getPrimaryModelId());
                    if (r.getFallbackModelId() != null) builder.add(r.getFallbackModelId());
                    return builder.build();
                })
                .collect(Collectors.toSet());

        // 批量查询模型名称
        Map<Long, String> modelNameMap = Map.of();
        if (!modelIds.isEmpty()) {
            List<AiModelConfig> models = modelConfigMapper.selectBatchIds(modelIds);
            modelNameMap = models.stream()
                    .collect(Collectors.toMap(AiModelConfig::getId, AiModelConfig::getModelName, (a, b) -> a));
        }

        Map<Long, String> finalMap = modelNameMap;
        return rules.stream().map(r -> {
            ModelRouteRuleVO vo = new ModelRouteRuleVO();
            vo.setId(r.getId());
            vo.setUsageScenario(r.getUsageScenario());
            vo.setPrimaryModelId(r.getPrimaryModelId());
            vo.setPrimaryModelName(finalMap.getOrDefault(r.getPrimaryModelId(), ""));
            vo.setFallbackModelId(r.getFallbackModelId());
            vo.setFallbackModelName(r.getFallbackModelId() != null ? finalMap.getOrDefault(r.getFallbackModelId(), "") : "");
            vo.setPriority(r.getPriority());
            vo.setIsActive(r.getIsActive());
            vo.setDescription(r.getDescription());
            return vo;
        }).collect(Collectors.toList());
    }
}
