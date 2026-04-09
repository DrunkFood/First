package com.jy.eletender.tenderdocument.controller;

import com.jy.eletender.common.response.Result;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleCopyRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentScoreTypeSaveRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEvaluationRulesPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentRuleResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentScoreTypeResponse;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.tenderdocument.service.ITenderDocumentRuleService;
import com.jy.eletender.tenderdocument.support.CurrentExternalUserResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 评审规则控制器。
 * 面向评审项配置页提供规则树读写、规则复制等编制能力。
 */
@RestController
@RequireLogin
@RequestMapping("/api/tender-documents")
public class TenderDocumentRuleController {

    private final ITenderDocumentRuleService tenderDocumentRuleService;
    private final CurrentExternalUserResolver currentExternalUserResolver;

    public TenderDocumentRuleController(ITenderDocumentRuleService tenderDocumentRuleService,
                                        CurrentExternalUserResolver currentExternalUserResolver) {
        this.tenderDocumentRuleService = tenderDocumentRuleService;
        this.currentExternalUserResolver = currentExternalUserResolver;
    }

    /**
     * 获取评审规则页面概览。
     * 该接口只返回页面初始化所需的标段列表、评标办法和节点分类，不返回具体树数据。
     */
    @GetMapping("/evaluation-rules")
    public Result<TenderDocumentEvaluationRulesPageResponse> getRulePage(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentRuleService.getRulePage(tenderDocumentId, userContext));
    }

    /**
     * 保存某个标段、某个节点分类下的评审规则树。
     * tenderId 和 nodeCategory 固定放在查询参数中，便于前端按页面维度组织接口调用。
     */
    @PutMapping("/evaluation-rules/item")
    public Result<Void> saveRuleTree(@RequestParam Long tenderDocumentId,
                                     @RequestParam String tenderId,
                                     @RequestParam String nodeCategory,
                                     @Valid @RequestBody TenderDocumentRuleSaveRequest request) {
        // 对外参数放在 query 中，对内统一回填到请求对象，避免 service 层再分散读取。
        request.setTenderId(tenderId);
        request.setNodeCategory(nodeCategory);
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentRuleService.saveRuleTree(tenderDocumentId, tenderId, request, userContext);
        return Result.success();
    }

    /**
     * 获取某个标段、某个节点分类下的评审规则树。
     * 返回值直接使用前端可渲染的树形结构，后端内部仍然按平表存储。
     */
    @GetMapping("/evaluation-rules/item")
    public Result<TenderDocumentRuleResponse> getRuleTree(@RequestParam Long tenderDocumentId,
                                                          @RequestParam String tenderId,
                                                          @RequestParam String nodeCategory) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentRuleService.getRuleTree(tenderDocumentId, tenderId, nodeCategory, userContext));
    }

    /**
     * 复制其他标段的评审规则到当前标段。
     * 复制时按目标标段整体覆盖，前端无需逐个节点搬运。
     */
    @PostMapping("/evaluation-rules/copy")
    public Result<Void> copyRuleTree(@RequestParam Long tenderDocumentId,
                                     @Valid @RequestBody TenderDocumentRuleCopyRequest request) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentRuleService.copyRuleTree(tenderDocumentId, request, userContext);
        return Result.success();
    }

    /**
     * 查询编制单级统一分值模式。
     */
    @GetMapping("/evaluation-rules/score-type")
    public Result<TenderDocumentScoreTypeResponse> getScoreType(@RequestParam Long tenderDocumentId) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        return Result.success(tenderDocumentRuleService.getScoreType(tenderDocumentId, userContext));
    }

    /**
     * 修改编制单级统一分值模式。
     */
    @PutMapping("/evaluation-rules/score-type")
    public Result<Void> updateScoreType(@RequestParam Long tenderDocumentId,
                                        @RequestBody TenderDocumentScoreTypeSaveRequest request) {
        TenderDocumentUserContext userContext = currentExternalUserResolver.resolve();
        tenderDocumentRuleService.updateScoreType(tenderDocumentId, request, userContext);
        return Result.success();
    }
}
