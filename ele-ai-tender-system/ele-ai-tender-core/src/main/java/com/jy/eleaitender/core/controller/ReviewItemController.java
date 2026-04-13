package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.entity.AiReviewItem;
import com.jy.eleaitender.core.service.IReviewItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public Result<AiReviewItem> create(@RequestBody AiReviewItem reviewItem) {
        return Result.success(reviewItemService.create(reviewItem));
    }

    @GetMapping("/{projectId}")
    @RequireLogin
    @Operation(summary = "获取项目评审项树")
    public Result<List<AiReviewItem>> getTree(@PathVariable Long projectId) {
        return Result.success(reviewItemService.getTreeByProjectId(projectId));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新评审项")
    public Result<Void> update(@PathVariable Long id, @RequestBody AiReviewItem reviewItem) {
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
}
