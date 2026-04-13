package com.jy.eleaitender.common.exception;

import com.jy.eleaitender.common.enums.ResponseCode;
import lombok.Getter;

/**
 * 文件异常
 */
@Getter
public class FileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public FileException(String message) {
        super(message);
        this.code = ResponseCode.FILE_UPLOAD_ERROR.getCode();
    }

    public FileException(int code, String message) {
        super(message);
        this.code = code;
    }

    public FileException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }

    public FileException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }
}
