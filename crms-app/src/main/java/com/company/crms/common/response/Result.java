package com.company.crms.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应封装。
 *
 * <pre>
 * { "code": "0", "message": "OK", "data": ..., "traceId": "..." }
 * </pre>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String CODE_SUCCESS = "0";

    private String code;
    private String message;
    private T data;
    private String traceId;

    private Result(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = MDC.get("traceId");
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, "OK", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_SUCCESS, "OK", data);
    }

    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return CODE_SUCCESS.equals(this.code);
    }
}
