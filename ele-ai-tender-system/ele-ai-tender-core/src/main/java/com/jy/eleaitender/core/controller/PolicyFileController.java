package com.jy.eleaitender.core.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiKnowledgeDocument;
import com.jy.eleaitender.common.entity.core.TbPolicyFile;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.request.PolicyFileRequest;
import com.jy.eleaitender.core.dto.response.KnowledgeDocumentPolicyVO;
import com.jy.eleaitender.core.dto.response.PolicyFileVO;
import com.jy.eleaitender.core.mapper.AiKnowledgeDocumentMapper;
import com.jy.eleaitender.core.service.IPolicyFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 政策文件控制器（编制中心 - 用户政策文件）
 */
@RestController
@RequestMapping("/api/v1/policy-files")
@Tag(name = "政策文件管理")
public class PolicyFileController {

    @Autowired
    private IPolicyFileService policyFileService;

    @Autowired
    private AiKnowledgeDocumentMapper knowledgeDocumentMapper;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询当前用户的政策文件")
    public Result<Page<PolicyFileVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) String applicableCategory) {
        return Result.success(policyFileService.getPage(pageNum, pageSize, fileName, fileCategory, applicableCategory));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "上传政策文件")
    public Result<TbPolicyFile> create(@RequestBody PolicyFileRequest request) {
        TbPolicyFile policyFile = new TbPolicyFile();
        BeanUtils.copyProperties(request, policyFile);
        return Result.success(policyFileService.create(policyFile));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "查看详情")
    public Result<TbPolicyFile> getById(@PathVariable Long id) {
        return Result.success(policyFileService.getById(id));
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除政策文件")
    public Result<Void> deleteById(@PathVariable Long id) {
        policyFileService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequireLogin
    @Operation(summary = "启用/禁用")
    public Result<Void> setStatus(@PathVariable Long id, @RequestParam Integer status) {
        policyFileService.setStatus(id, status);
        return Result.success();
    }

    @GetMapping("/all")
    @RequireLogin
    @Operation(summary = "获取全部可用政策文件（系统级+用户级合并）")
    public Result<List<PolicyFileVO>> getAllAvailable(
            @RequestParam(required = false) String applicableCategory) {
        return Result.success(policyFileService.getAllAvailable(applicableCategory));
    }

    @GetMapping("/knowledge-policy")
    @RequireLogin
    @Operation(summary = "获取知识库中所有政策类文档(docCategory=POLICY)")
    public Result<List<KnowledgeDocumentPolicyVO>> getKnowledgePolicyDocuments() {
        LambdaQueryWrapper<AiKnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeDocument::getDocCategory, "POLICY")
                .eq(AiKnowledgeDocument::getStatus, "ACTIVE")
                .orderByDesc(AiKnowledgeDocument::getCreateTime);
        List<AiKnowledgeDocument> docs = knowledgeDocumentMapper.selectList(wrapper);
        List<KnowledgeDocumentPolicyVO> result = new ArrayList<>(docs.size());
        for (AiKnowledgeDocument doc : docs) {
            KnowledgeDocumentPolicyVO vo = new KnowledgeDocumentPolicyVO();
            vo.setId(doc.getId());
            vo.setDocName(doc.getDocName());
            vo.setFileId(doc.getFileId());
            vo.setFileType(doc.getFileType());
            vo.setStatus(doc.getStatus());
            vo.setCreateTime(doc.getCreateTime());
            result.add(vo);
        }
        return Result.success(result);
    }
}
