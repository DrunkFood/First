package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupPolicyFile;
import com.jy.eleaitender.common.logging.OperationLog;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IPolicyFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 政策文件控制器
 */
@RestController
@RequestMapping("/api/v1/policy-files")
@Tag(name = "政策文件审查库")
public class PolicyFileController {

    @Autowired
    private IPolicyFileService policyFileService;

    @GetMapping
    @Operation(summary = "分页查询政策文件")
    @RequireLogin
    public Result<Page<SupPolicyFile>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String fileCategory,
            @RequestParam(required = false) String applicableCategory) {
        return Result.success(policyFileService.getPage(pageNum, pageSize, fileName, fileCategory, applicableCategory));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取政策文件详情")
    @RequireLogin
    public Result<SupPolicyFile> getById(@PathVariable Long id) {
        return Result.success(policyFileService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建政策文件记录")
    @RequireLogin
    @OperationLog("上传政策文件")
    public Result<SupPolicyFile> create(@RequestBody SupPolicyFile policyFile) {
        return Result.success(policyFileService.create(policyFile));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新政策文件信息")
    @RequireLogin
    @OperationLog("更新政策文件")
    public Result<Void> update(@PathVariable Long id, @RequestBody SupPolicyFile policyFile) {
        policyFile.setId(id);
        policyFileService.update(policyFile);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除政策文件")
    @RequireLogin
    @OperationLog("删除政策文件")
    public Result<Void> delete(@PathVariable Long id) {
        policyFileService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用政策文件")
    @RequireLogin
    @OperationLog("变更政策文件状态")
    public Result<Void> setStatus(@PathVariable Long id, @RequestParam Integer status) {
        policyFileService.setStatus(id, status);
        return Result.success();
    }
}
