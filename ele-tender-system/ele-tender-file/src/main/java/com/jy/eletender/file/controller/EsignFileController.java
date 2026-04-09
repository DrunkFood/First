package com.jy.eletender.file.controller;

import com.jy.eletender.common.dto.response.FileUploadResponse;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.file.service.IFileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file/esign")
@Tag(name = "文件服务")
public class EsignFileController {

    private static final String ESIGN_BIZ_TYPE = "esign";

    private final IFileStorageService fileStorageService;

    public EsignFileController(IFileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @RequireLogin
    @PostMapping("/upload")
    @Operation(
            summary = "Esign 上传文件",
            parameters = {
                    @Parameter(name = "token", in = ParameterIn.QUERY, required = true, description = "Esign 前端通过请求参数传递的 JWT")
            }
    )
    public Result<FileUploadResponse> upload(
            @Parameter(description = "文件", required = true) @RequestParam("file") MultipartFile file) {
        return Result.success(fileStorageService.upload(file, ESIGN_BIZ_TYPE));
    }
}
