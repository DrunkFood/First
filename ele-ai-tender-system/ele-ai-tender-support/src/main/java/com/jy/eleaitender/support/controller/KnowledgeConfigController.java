package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IKnowledgeConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge-configs")
@Tag(name = "知识库管理")
public class KnowledgeConfigController {

    @Autowired
    private IKnowledgeConfigService knowledgeConfigService;

    @GetMapping
    @Operation(summary = "查询知识库列表")
    @RequireLogin
    public Result<Page<AiKnowledgeDocument>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String docCategory,
            @RequestParam(required = false) String status) {
        return Result.success(knowledgeConfigService.getPage(pageNum, pageSize, docCategory, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识文档详情")
    @RequireLogin
    public Result<AiKnowledgeDocument> getById(@PathVariable Long id) {
        return Result.success(knowledgeConfigService.getById(id));
    }

    @PostMapping
    @Operation(summary = "上传知识文档")
    @RequireLogin
    public Result<AiKnowledgeDocument> create(@RequestBody AiKnowledgeDocument document) {
        return Result.success(knowledgeConfigService.create(document));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识文档")
    @RequireLogin
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeConfigService.deleteById(id);
        return Result.success();
    }
}
