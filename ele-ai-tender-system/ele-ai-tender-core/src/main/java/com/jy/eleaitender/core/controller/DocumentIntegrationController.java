package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.response.DocumentPreviewVO;
import com.jy.eleaitender.core.service.IDocumentIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 文档集成控制器
 */
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "文档集成")
public class DocumentIntegrationController {

    @Autowired
    private IDocumentIntegrationService documentIntegrationService;

    @PostMapping("/integrate/{projectId}")
    @RequireLogin
    @Operation(summary = "执行文档集成")
    public Result<DocumentPreviewVO> integrate(@PathVariable Long projectId) {
        return Result.success(documentIntegrationService.integrate(projectId));
    }

    @GetMapping("/preview/{projectId}")
    @RequireLogin
    @Operation(summary = "获取集成预览")
    public Result<DocumentPreviewVO> getPreview(@PathVariable Long projectId) {
        return Result.success(documentIntegrationService.getPreview(projectId));
    }

    @GetMapping("/export/{projectId}")
    @RequireLogin
    @Operation(summary = "导出Word文档")
    public ResponseEntity<byte[]> exportWord(@PathVariable Long projectId) {
        byte[] docBytes = documentIntegrationService.exportWord(projectId);

        String fileName = URLEncoder.encode("招标文件.docx", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(docBytes.length)
                .body(docBytes);
    }

    @PutMapping("/edit/{projectId}")
    @RequireLogin
    @Operation(summary = "编辑集成后的文档内容")
    public Result<Void> editContent(@PathVariable Long projectId,
                                    @RequestBody Map<String, String> body) {
        String markdownContent = body.get("markdownContent");
        documentIntegrationService.editContent(projectId, markdownContent);
        return Result.success();
    }
}
