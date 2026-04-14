package com.jy.eleaitender.ai.controller;

import com.jy.eleaitender.ai.dto.request.MatchRequest;
import com.jy.eleaitender.ai.dto.response.MatchResultVO;
import com.jy.eleaitender.ai.service.IDocumentMatchService;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文档匹配控制器
 */
@Tag(name = "文档匹配", description = "历史需求匹配")
@RestController
@RequestMapping("/api/v1/ai/match")
public class DocumentMatchController {

    @Autowired
    private IDocumentMatchService documentMatchService;

    @Operation(summary = "自动匹配历史需求")
    @PostMapping("/auto")
    @RequireLogin
    public Result<List<MatchResultVO>> autoMatch(@RequestBody @Valid MatchRequest request) {
        return Result.success(documentMatchService.autoMatch(request));
    }

    @Operation(summary = "手动选择匹配（返回候选列表）")
    @PostMapping("/manual")
    @RequireLogin
    public Result<List<MatchResultVO>> manualMatch(@RequestBody @Valid MatchRequest request) {
        return Result.success(documentMatchService.manualMatch(request));
    }
}
