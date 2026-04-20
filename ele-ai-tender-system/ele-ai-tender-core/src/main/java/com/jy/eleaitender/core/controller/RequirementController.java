package com.jy.eleaitender.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiDetectionRecord;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.core.dto.response.MatchFileVO;
import com.jy.eleaitender.core.service.IRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务需求控制器
 */
@RestController
@RequestMapping("/api/v1/requirements")
@Tag(name = "业务需求管理")
public class RequirementController {

    @Autowired
    private IRequirementService requirementService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询需求列表")
    public Result<Page<AiRequirement>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String requirementName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long projectId) {
        return Result.success(requirementService.getPage(pageNum, pageSize, requirementName, status, projectId));
    }

    @GetMapping("/match-files")
    @RequireLogin
    @Operation(summary = "获取匹配文件列表")
    public Result<List<MatchFileVO>> getMatchFiles(
            @RequestParam(required = false) Long requirementId,
            @RequestParam(required = false) String keyword) {
        return Result.success(requirementService.getMatchFiles(requirementId, keyword));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取需求详情")
    public Result<AiRequirement> getById(@PathVariable Long id) {
        return Result.success(requirementService.getById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建需求")
    public Result<AiRequirement> create(@RequestBody AiRequirement requirement) {
        return Result.success(requirementService.create(requirement));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新需求")
    public Result<Void> update(@PathVariable Long id, @RequestBody AiRequirement requirement) {
        requirementService.update(id, requirement);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除需求")
    public Result<Void> deleteById(@PathVariable Long id) {
        requirementService.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/match")
    @RequireLogin
    @Operation(summary = "匹配历史模板")
    public Result<AiRequirement> match(
            @PathVariable Long id,
            @RequestParam Long matchedFileId,
            @RequestParam(defaultValue = "MANUAL_SELECT") String matchMode) {
        return Result.success(requirementService.matchTemplate(id, matchedFileId, matchMode));
    }

    @PostMapping("/{id}/generate")
    @RequireLogin
    @Operation(summary = "提交AI生成需求任务")
    public Result<AiTask> generate(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        return Result.success(requirementService.submitGenerate(id, params));
    }

    @PostMapping("/{id}/auto-save")
    @RequireLogin
    @Operation(summary = "自动保存草稿")
    public Result<Void> autoSave(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requirementService.autoSave(id, body.get("content"));
        return Result.success();
    }

    @GetMapping("/{id}/auto-save")
    @RequireLogin
    @Operation(summary = "获取自动保存内容")
    public Result<String> getAutoSave(@PathVariable Long id) {
        return Result.success(requirementService.getAutoSaveContent(id));
    }

    @DeleteMapping("/{id}/auto-save")
    @RequireLogin
    @Operation(summary = "清除自动保存内容")
    public Result<Void> clearAutoSave(@PathVariable Long id) {
        requirementService.clearAutoSave(id);
        return Result.success();
    }

    @PostMapping("/{id}/detect")
    @RequireLogin
    @Operation(summary = "提交需求检测（敏感词+错别字）")
    public Result<Map<String, Long>> detect(@PathVariable Long id) {
        return Result.success(requirementService.submitDetection(id));
    }

    @PostMapping("/{id}/detect/{recordId}/accept")
    @RequireLogin
    @Operation(summary = "接受需求检测建议（自动修正内容）")
    public Result<Void> acceptDetectionIssue(@PathVariable Long id,
                                              @PathVariable Long recordId,
                                              @RequestParam Integer issueIndex) {
        requirementService.acceptDetectionIssue(id, recordId, issueIndex);
        return Result.success();
    }

    @PostMapping("/{id}/detect/{recordId}/reject")
    @RequireLogin
    @Operation(summary = "拒绝需求检测建议")
    public Result<Void> rejectDetectionIssue(@PathVariable Long id,
                                              @PathVariable Long recordId,
                                              @RequestParam Integer issueIndex) {
        requirementService.rejectDetectionIssue(id, recordId, issueIndex);
        return Result.success();
    }

    @GetMapping("/{id}/detect/records")
    @RequireLogin
    @Operation(summary = "获取需求检测记录列表")
    public Result<List<AiDetectionRecord>> getDetectionRecords(@PathVariable Long id) {
        return Result.success(requirementService.getDetectionRecords(id));
    }

    @PostMapping("/{id}/detect/finish")
    @RequireLogin
    @Operation(summary = "完成需求检测")
    public Result<Void> finishDetection(@PathVariable Long id) {
        requirementService.finishDetection(id);
        return Result.success();
    }
}
