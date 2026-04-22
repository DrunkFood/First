package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.entity.core.TbProjectTemplate;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.service.IProjectTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 项目模板管理控制器
 */
@RestController
@RequestMapping("/api/v1/project-templates")
@Tag(name = "项目模板管理")
public class ProjectTemplateController {

    @Autowired
    private IProjectTemplateService projectTemplateService;

    @PostMapping("/bind")
    @RequireLogin
    @Operation(summary = "项目绑定模板")
    public Result<TbProjectTemplate> bind(@RequestBody Map<String, Long> params) {
        Long projectId = params.get("projectId");
        Long supTemplateId = params.get("supTemplateId");
        return Result.success(projectTemplateService.bindTemplate(projectId, supTemplateId));
    }

    @GetMapping("/project/{projectId}")
    @RequireLogin
    @Operation(summary = "获取项目模板")
    public Result<TbProjectTemplate> getByProject(@PathVariable Long projectId) {
        return Result.success(projectTemplateService.getByProjectId(projectId));
    }
}
