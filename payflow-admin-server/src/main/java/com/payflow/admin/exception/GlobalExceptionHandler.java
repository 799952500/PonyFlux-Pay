package com.payflow.admin.exception;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.NestedServletException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
/**
 * @author Lucas
 */
public class GlobalExceptionHandler {

    public static class ApiResponse<T> {
        public int code;
        public String message;
        public T data;

        public ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return new ApiResponse<>(400, "Validation failed", errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleIllegalArg(IllegalArgumentException ex) {
        return new ApiResponse<>(400, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleIllegalState(IllegalStateException ex) {
        return new ApiResponse<>(400, ex.getMessage(), null);
    }

    /**
     * 未匹配到 Controller 时 Spring 可能直接抛出；返回 404 与统一 JSON，避免被包装成「静态资源不存在」的 500。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleNoResourceFound(NoResourceFoundException ex) {
        return new ApiResponse<>(404, "接口不存在: " + ex.getResourcePath(), null);
    }

    /**
     * DispatcherServlet 常将底层异常包装为 NestedServletException（非 RuntimeException），需单独解析。
     */
    @ExceptionHandler(NestedServletException.class)
    public ResponseEntity<ApiResponse<Object>> handleNestedServlet(NestedServletException ex) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (cause instanceof NoResourceFoundException nrf) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, "接口不存在: " + nrf.getResourcePath(), null));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Internal server error: " + ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleRuntime(RuntimeException ex) {
        return new ApiResponse<>(500, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGeneral(Exception ex) {
        return new ApiResponse<>(500, "Internal server error: " + ex.getMessage(), null);
    }
}
