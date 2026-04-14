package com.jy.eleaitender.support.controller;

import com.jy.eleaitender.common.entity.support.SysParameter;
import com.jy.eleaitender.common.logging.OperationLog;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.ISysParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统参数控制器
 */
@RestController
@RequestMapping("/api/v1/sys-params")
@Tag(name = "系统参数管理")
public class SysParameterController {

    @Autowired
    private ISysParameterService sysParameterService;

    @GetMapping
    @Operation(summary = "按分组查询系统参数")
    @RequireLogin
    public Result<List<SysParameter>> list(@RequestParam(required = false) String paramGroup) {
        return Result.success(sysParameterService.listByGroup(paramGroup));
    }

    @GetMapping("/value")
    @Operation(summary = "根据key获取参数值")
    @RequireLogin
    public Result<String> getValueByKey(@RequestParam String paramKey) {
        return Result.success(sysParameterService.getValueByKey(paramKey));
    }

    @PutMapping
    @Operation(summary = "批量更新系统参数")
    @RequireLogin
    @OperationLog("更新系统参数")
    public Result<Void> batchUpdate(@RequestBody Map<String, String> params) {
        sysParameterService.batchUpdate(params);
        return Result.success();
    }

    @PutMapping("/{paramKey}")
    @Operation(summary = "更新单个系统参数")
    @RequireLogin
    @OperationLog("更新系统参数")
    public Result<Void> updateByKey(@PathVariable String paramKey, @RequestBody Map<String, String> body) {
        sysParameterService.updateByKey(paramKey, body.get("paramValue"));
        return Result.success();
    }
}
