package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.core.service.IReviewItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评审项控制器
 */
@RestController
@RequestMapping("/api/v1/review-items")
@Tag(name = "评审项管理")
public class ReviewItemController {

    @Autowired
    private IReviewItemService reviewItemService;

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建评审项")
    public Result<TbProjectReviewItem> create(@RequestBody TbProjectReviewItem reviewItem) {
        return Result.success(reviewItemService.create(reviewItem));
    }

    @GetMapping("/{projectId}")
    @RequireLogin
    @Operation(summary = "获取项目评审项树")
    public Result<List<TbProjectReviewItem>> getTree(@PathVariable Long projectId) {
        return Result.success(reviewItemService.getTreeByProjectId(projectId));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新评审项")
    public Result<Void> update(@PathVariable Long id, @RequestBody TbProjectReviewItem reviewItem) {
        reviewItemService.update(id, reviewItem);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除评审项")
    public Result<Void> deleteById(@PathVariable Long id) {
        reviewItemService.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{projectId}/generate")
    @RequireLogin
    @Operation(summary = "提交AI生成评审项")
    public Result<AiTask> submitGenerate(@PathVariable Long projectId,
                                         @RequestBody Map<String, Object> params) {
        return Result.success(reviewItemService.submitGenerate(projectId, params));
    }

    @PostMapping("/batch")
    @RequireLogin
    @Operation(summary = "批量创建评审项")
    public Result<Void> batchCreate(@RequestBody List<TbProjectReviewItem> items) {
        reviewItemService.batchCreate(items);
        return Result.success();
    }

    @PutMapping("/batch")
    @RequireLogin
    @Operation(summary = "批量更新评审项")
    public Result<Void> batchUpdate(@RequestBody List<TbProjectReviewItem> items) {
        reviewItemService.batchUpdate(items);
        return Result.success();
    }
}
