package com.jy.eleaitender.file.handler;

import com.jy.eleaitender.common.exception.AuthException;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.exception.FileException;
import com.jy.eleaitender.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleAuthException(AuthException e) {
        log.error("认证异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(FileException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleFileException(FileException e) {
        log.error("文件异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("文件大小超出限制: {}", e.getMessage());
        return Result.fail(5004, "文件大小超出限制");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(buildUnhandledExceptionMessage(e));
    }

    private String buildUnhandledExceptionMessage(Exception e) {
        String detail = e.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            detail = e.getClass().getSimpleName();
        }
        return "系统异常: " + detail;
    }
}
