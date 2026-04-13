package com.jy.eleaitender.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.ai.dto.request.KnowledgeDocumentRequest;
import com.jy.eleaitender.ai.service.IKnowledgeDocumentService;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge/documents")
@Tag(name = "知识库管理")
public class KnowledgeController {

    @Autowired
    private IKnowledgeDocumentService knowledgeDocumentService;

    @GetMapping
    @Operation(summary = "查询知识文档列表")
    @RequireLogin
    public Result<Page<AiKnowledgeDocument>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String docCategory,
            @RequestParam(required = false) String status) {
        return Result.success(knowledgeDocumentService.getPage(pageNum, pageSize, docCategory, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识文档详情")
    @RequireLogin
    public Result<AiKnowledgeDocument> getById(@PathVariable Long id) {
        return Result.success(knowledgeDocumentService.getById(id));
    }

    @PostMapping
    @Operation(summary = "上传知识文档")
    @RequireLogin
    public Result<AiKnowledgeDocument> create(@RequestBody @jakarta.validation.Valid KnowledgeDocumentRequest request) {
        return Result.success(knowledgeDocumentService.create(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识文档")
    @RequireLogin
    public Result<Void> deleteById(@PathVariable Long id) {
        knowledgeDocumentService.deleteById(id);
        return Result.success();
    }
}
