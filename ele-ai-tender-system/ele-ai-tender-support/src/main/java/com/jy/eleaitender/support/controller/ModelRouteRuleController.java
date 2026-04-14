package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupModelRouteRule;
import com.jy.eleaitender.common.logging.OperationLog;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IModelRouteRuleService;
import com.jy.eleaitender.support.vo.ModelRouteRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型路由规则控制器
 */
@RestController
@RequestMapping("/api/v1/model-routes")
@Tag(name = "模型路由管理")
public class ModelRouteRuleController {

    @Autowired
    private IModelRouteRuleService routeRuleService;

    @GetMapping
    @Operation(summary = "分页查询路由规则")
    @RequireLogin
    public Result<Page<ModelRouteRuleVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String usageScenario) {
        return Result.success(routeRuleService.getPage(pageNum, pageSize, usageScenario));
    }

    @GetMapping("/active")
    @Operation(summary = "获取指定场景的生效路由规则")
    @RequireLogin
    public Result<List<SupModelRouteRule>> activeRules(@RequestParam String usageScenario) {
        return Result.success(routeRuleService.getActiveRules(usageScenario));
    }

    @PostMapping
    @Operation(summary = "创建路由规则")
    @RequireLogin
    @OperationLog("创建模型路由规则")
    public Result<SupModelRouteRule> create(@RequestBody SupModelRouteRule rule) {
        return Result.success(routeRuleService.create(rule));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新路由规则")
    @RequireLogin
    @OperationLog("更新模型路由规则")
    public Result<Void> update(@PathVariable Long id, @RequestBody SupModelRouteRule rule) {
        rule.setId(id);
        routeRuleService.update(rule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除路由规则")
    @RequireLogin
    @OperationLog("删除模型路由规则")
    public Result<Void> delete(@PathVariable Long id) {
        routeRuleService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "启用/停用路由规则")
    @RequireLogin
    @OperationLog("变更路由规则状态")
    public Result<Void> setActive(@PathVariable Long id, @RequestParam Integer isActive) {
        routeRuleService.setActive(id, isActive);
        return Result.success();
    }
}
