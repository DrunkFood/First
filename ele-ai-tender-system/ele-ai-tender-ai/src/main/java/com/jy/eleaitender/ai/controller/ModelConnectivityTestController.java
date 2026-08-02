package com.jy.eleaitender.ai.controller;

import com.jy.eleaitender.ai.dto.response.ModelConnectivityTestResponse;
import com.jy.eleaitender.ai.service.IModelConnectivityTestService;
import com.jy.eleaitender.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型连通性测试控制器。
 */
@Tag(name = "模型连通性测试", description = "DeepSeek、智谱模型调用测试")
@RestController
@RequestMapping("/api/v1/ai/model-test")
public class ModelConnectivityTestController {

    @Autowired
    private IModelConnectivityTestService modelConnectivityTestService;

    @Operation(summary = "测试DeepSeek/OpenAI兼容模型")
    @PostMapping("/deepseek")
    public Result<ModelConnectivityTestResponse> testDeepSeek() {
        return Result.success(modelConnectivityTestService.testDeepSeek());
    }

    @Operation(summary = "测试智谱模型")
    @PostMapping("/zhipu")
    public Result<ModelConnectivityTestResponse> testZhiPu() {
        return Result.success(modelConnectivityTestService.testZhiPu());
    }
}
