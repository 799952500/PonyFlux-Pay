package com.payflow.cashier.exception;

import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author PayFlow Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("参数校验失败: {}", errors);
        return R.<Map<String, String>>builder()
                .code(4001)
                .message("参数校验失败")
                .data(errors)
                .build();
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e, HttpServletResponse response) throws Exception {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getCode() == 6001 || e.getCode() == 6002) {
            status = HttpStatus.NOT_FOUND;
        } else if (e.getCode() == 6006) {
            status = HttpStatus.CONFLICT;
        }
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        response.setStatus(status.value());
        return R.<Void>builder()
                .code(e.getCode())
                .message(e.getMessage())
                .build();
    }

    /**
     * 运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return R.serverError(e.getMessage());
    }

    /**
     * 通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return R.serverError("系统内部错误");
    }
}
