package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysAccessLog;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.security.annotation.RequirePermission;
import com.jy.eleaitender.support.service.IAccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 访问日志控制器
 */
@RestController
@RequestMapping("/api/access-logs")
@Tag(name = "访问日志管理")
public class AccessLogController {

    @Autowired
    private IAccessLogService accessLogService;

    @GetMapping
    @RequireLogin
    @RequirePermission("access-log:view")
    @Operation(summary = "分页查询访问日志")
    /**
     * 提供给管理端的访问日志分页查询接口。
     */
    public Result<Page<SysAccessLog>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String tenderId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        return Result.success(accessLogService.getAccessLogPage(
                pageNum,
                pageSize,
                traceId,
                serviceName,
                statusCode,
                bizType,
                bizId,
                projectId,
                tenderId,
                startTime,
                endTime));
    }
}
