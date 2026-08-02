package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.support.SupModelRouteRule;
import com.jy.eleaitender.support.vo.ModelRouteRuleVO;

import java.util.List;

/**
 * 模型路由规则服务接口
 */
public interface IModelRouteRuleService extends IService<SupModelRouteRule> {

    /**
     * 分页查询路由规则
     *
     * @param pageNum       页码
     * @param pageSize      每页条数
     * @param usageScenario 使用场景（可选）
     * @return 分页结果（带模型名称）
     */
    Page<ModelRouteRuleVO> getPage(Integer pageNum, Integer pageSize, String usageScenario);

    /**
     * 根据场景获取生效的路由规则（按优先级排序）
     *
     * @param usageScenario 使用场景
     * @return 路由规则列表
     */
    List<SupModelRouteRule> getActiveRules(String usageScenario);

    /**
     * 创建路由规则
     *
     * @param rule 路由规则
     * @return 创建后的实体
     */
    SupModelRouteRule create(SupModelRouteRule rule);

    /**
     * 更新路由规则
     *
     * @param rule 路由规则
     */
    void update(SupModelRouteRule rule);

    /**
     * 删除路由规则
     *
     * @param id 主键ID
     */
    void deleteById(Long id);

    /**
     * 启用/停用路由规则
     *
     * @param id       主键ID
     * @param isActive 0-停用, 1-启用
     */
    void setActive(Long id, Integer isActive);
}
