package com.payflow.recon.exception;

import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理。
 *
 * @author PayFlow Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
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

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (e.getCode() >= 7500 && e.getCode() < 7600) {
            status = HttpStatus.BAD_REQUEST;
        }
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        response.setStatus(status.value());
        return R.<Void>builder()
                .code(e.getCode())
                .message(e.getMessage())
                .build();
    }

    @ExceptionHandler(SQLSyntaxErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleSqlSyntax(SQLSyntaxErrorException e) {
        log.error("SQL 语法/表结构异常: {}", e.getMessage(), e);
        return R.<Void>builder()
                .code(7598)
                .message("对账表未初始化或结构不匹配，请执行 admin-schema.sql / admin-alter-202605-recon.sql 或 sql/full-reseed-payflow-demo.sql")
                .build();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoResource(NoResourceFoundException e) {
        return R.<Void>builder().code(404).message("not found").build();
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleRuntime(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        return R.serverError("服务器内部错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return R.<Void>builder().code(5000).message("系统内部错误").build();
    }
}
