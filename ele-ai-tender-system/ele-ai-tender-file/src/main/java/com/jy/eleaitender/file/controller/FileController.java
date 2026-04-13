package com.jy.eleaitender.file.controller;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.FileException;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.dto.response.FileUploadResponse;
import com.jy.eleaitender.common.entity.file.FileInfo;
import com.jy.eleaitender.file.service.IFileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件服务")
public class FileController {

    @Autowired
    private IFileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<FileUploadResponse> upload(
            @Parameter(description = "文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型", required = true) @RequestParam("bizType") String bizType) {
        FileUploadResponse response = fileStorageService.upload(file, bizType);
        return Result.success(response);
    }

    @GetMapping("/download/{fileId}")
    @Operation(summary = "下载文件")
    public ResponseEntity<Resource> download(
            @Parameter(description = "文件ID", required = true) @PathVariable("fileId") Long fileId) {
        FileInfo fileInfo = fileStorageService.getById(fileId);
        if (fileInfo == null) {
            throw new FileException(ResponseCode.FILE_NOT_FOUND);
        }

        String filePath = fileStorageService.getFilePath(fileId);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileException(ResponseCode.FILE_NOT_FOUND);
        }

        Resource resource = new FileSystemResource(file);

        String encodedFileName = URLEncoder.encode(fileInfo.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + encodedFileName + "\"")
                .body(resource);
    }

    @GetMapping("/info/{fileId}")
    @Operation(summary = "获取文件信息")
    public Result<FileInfo> info(
            @Parameter(description = "文件ID", required = true) @PathVariable("fileId") Long fileId) {
        FileInfo fileInfo = fileStorageService.getById(fileId);
        if (fileInfo == null) {
            return Result.fail(ResponseCode.FILE_NOT_FOUND);
        }
        return Result.success(fileInfo);
    }

    @DeleteMapping("/delete/{fileId}")
    @Operation(summary = "删除文件")
    public Result<Boolean> delete(
            @Parameter(description = "文件ID", required = true) @PathVariable("fileId") Long fileId) {
        boolean result = fileStorageService.delete(fileId);
        return Result.success(result);
    }
}
