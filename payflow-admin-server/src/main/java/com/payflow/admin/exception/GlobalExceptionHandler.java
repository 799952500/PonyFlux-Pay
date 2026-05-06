package com.payflow.admin.exception;

import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 全局异常处理器。
 *
 * <p>安全策略：运行时异常和通用异常不向客户端泄露内部堆栈或消息文本，
 * 仅返回「服务器内部错误」。业务异常（{@link BizException}）允许透传 message，
 * 因为业务异常由开发者主动抛出，内容可控。</p>
 *
 * <p>所有异常详细信息均通过 SLF4J 记录到日志，便于排查。</p>
 *
 * @author Lucas
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 统一响应结构
     *
     * @param <T> 数据体泛型
     */
    public static class ApiResponse<T> {

        /** 状态码 */
        public int code;

        /** 响应消息 */
        public String message;

        /** 数据体 */
        public T data;

        /**
         * 构造统一响应
         *
         * @param code    状态码
         * @param message 响应消息
         * @param data    数据体
         */
        public ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }

    /**
     * 处理参数校验异常，返回字段级错误详情。
     *
     * @param ex 校验异常
     * @return 字段错误映射
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("参数校验失败: {}", errors);
        return new ApiResponse<>(400, "参数校验失败", errors);
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex 非法参数异常
     * @return 400 响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return new ApiResponse<>(400, ex.getMessage(), null);
    }

    /**
     * 处理非法状态异常。
     *
     * @param ex 非法状态异常
     * @return 400 响应
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleIllegalState(IllegalStateException ex) {
        log.warn("非法状态: {}", ex.getMessage());
        return new ApiResponse<>(400, ex.getMessage(), null);
    }

    /**
     * 处理业务异常（BizException），允许透传 message（内容由开发者控制）。
     *
     * @param ex       业务异常
     * @param response HTTP 响应
     * @return 业务异常响应
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Object>> handleBizException(BizException ex, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getCode() == 6001 || ex.getCode() == 6002) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getCode() == 6006) {
            status = HttpStatus.CONFLICT;
        }
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(ex.getCode(), ex.getMessage(), null));
    }

    /**
     * 未匹配到 Controller 时 Spring 可能直接抛出；返回 404 与统一 JSON，避免被包装成「静态资源不存在」的 500。
     *
     * @param ex 资源未找到异常
     * @return 404 响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleNoResourceFound(NoResourceFoundException ex) {
        return new ApiResponse<>(404, "接口不存在: " + ex.getResourcePath(), null);
    }

    /**
     * DispatcherServlet 常将底层异常包装为 NestedServletException，需单独解析。
     *
     * @param ex 嵌套 Servlet 异常
     * @return 对应 HTTP 状态的响应
     */
    @ExceptionHandler(NestedServletException.class)
    public ResponseEntity<ApiResponse<Object>> handleNestedServlet(NestedServletException ex) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (cause instanceof NoResourceFoundException nrf) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, "接口不存在: " + nrf.getResourcePath(), null));
        }
        // 脱敏：不返回内部异常消息
        log.error("NestedServletException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "服务器内部错误", null));
    }

    /**
     * 处理运行时异常，脱敏后返回统一消息。
     * <p>详细异常信息仅记录日志，不暴露给客户端。</p>
     *
     * @param ex 运行时异常
     * @return 脱敏后的 500 响应
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleRuntime(RuntimeException ex) {
        log.error("运行时异常: {}", ex.getMessage(), ex);
        return new ApiResponse<>(500, "服务器内部错误", null);
    }

    /**
     * 处理通用异常，脱敏后返回统一消息。
     * <p>详细异常信息仅记录日志，不暴露给客户端。</p>
     *
     * @param ex 通用异常
     * @return 脱敏后的 500 响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGeneral(Exception ex) {
        log.error("系统异常: {}", ex.getMessage(), ex);
        return new ApiResponse<>(500, "服务器内部错误", null);
    }
}
