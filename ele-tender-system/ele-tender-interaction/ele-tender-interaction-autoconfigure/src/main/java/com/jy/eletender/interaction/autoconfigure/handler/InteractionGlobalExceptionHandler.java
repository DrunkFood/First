package com.jy.eletender.interaction.autoconfigure.handler;

import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.exception.InteractionException;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionBidRecordSchemeController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionCaKeysInfoController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionIdentityController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionProjectInfoController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionTenderPackageCallbackController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionTenderPdfCallbackController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 交互层全局异常处理
 */
@Slf4j
@RestControllerAdvice(basePackageClasses = {
        InteractionIdentityController.class,
        InteractionProjectInfoController.class,
        InteractionBidRecordSchemeController.class,
        InteractionCaKeysInfoController.class,
        InteractionTenderPdfCallbackController.class,
        InteractionTenderPackageCallbackController.class
})
public class InteractionGlobalExceptionHandler {

    @ExceptionHandler(InteractionException.class)
    @ResponseStatus(HttpStatus.OK)
    public InteractionResult<?> handleInteractionException(InteractionException e) {
        log.error("交互业务异常: {}", e.getMessage());
        return InteractionResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public InteractionResult<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return InteractionResult.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public InteractionResult<?> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return InteractionResult.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public InteractionResult<?> handleException(Exception e) {
        log.error("交互系统异常", e);
        return InteractionResult.fail(buildUnhandledExceptionMessage(e));
    }

    private String buildUnhandledExceptionMessage(Exception e) {
        String detail = e.getMessage();
        if (detail == null || detail.trim().isEmpty()) {
            detail = e.getClass().getSimpleName();
        }
        return "系统异常: " + detail;
    }
}
