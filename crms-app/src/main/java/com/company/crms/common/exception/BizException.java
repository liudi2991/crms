package com.company.crms.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。
 *
 * <p>HTTP 状态保持 200，业务错误码通过 {@code code} 字段返回。
 */
@Getter
public class BizException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String code;

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }
}
