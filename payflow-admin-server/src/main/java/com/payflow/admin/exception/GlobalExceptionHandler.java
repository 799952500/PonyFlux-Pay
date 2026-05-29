package com.payflow.admin.exception;

import com.payflow.common.exception.BizException;
import com.payflow.common.web.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.redis.RedisConnectionFailureException;
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
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @deprecated 迁移期兼容别名，请改用 {@link R}。
     */
    @Deprecated
    public static class ApiResponse<T> extends R<T> {
        public ApiResponse(int code, String message, T data) {
            setCode(code);
            setMessage(message);
            setData(data);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        String firstMessage = "参数校验失败";
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
            if (firstMessage.equals("参数校验失败") && error.getDefaultMessage() != null) {
                firstMessage = error.getDefaultMessage();
            }
        }
        log.warn("参数校验失败: {}", errors);
        return R.<Object>builder().code(400).message(firstMessage).data(errors).build();
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public R<Object> handleRedisDown(RedisConnectionFailureException ex) {
        log.warn("Redis 连接失败: {}", ex.getMessage());
        return R.bizError(503, "Redis 服务不可用，请启动 redis-server（端口 6379）");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Object> handleIllegalArg(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return R.bizError(400, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Object> handleIllegalState(IllegalStateException ex) {
        log.warn("非法状态: {}", ex.getMessage());
        return R.bizError(400, ex.getMessage());
    }

    @ExceptionHandler(ResourceDependencyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<Object> handleResourceDependency(ResourceDependencyException ex) {
        log.warn("删除被关联阻断: {}", ex.getMessage());
        Map<String, Object> data = new HashMap<>();
        if (ex.getResult() != null) {
            data.put("blocked", ex.getResult().isBlocked());
            data.put("summary", ex.getResult().getSummary());
            data.put("refs", ex.getResult().getRefs());
        }
        return R.<Object>builder()
                .code(ResourceDependencyException.CODE)
                .message(ex.getMessage())
                .data(data)
                .build();
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Object>> handleBizException(BizException ex, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getCode() == 6001 || ex.getCode() == 6002) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getCode() == 6006) {
            status = HttpStatus.CONFLICT;
        } else if (ex.getCode() == 6101) {
            status = HttpStatus.FORBIDDEN;
        }
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        String message = ex.getCode() == 6101 ? "无权访问该资源" : ex.getMessage();
        return ResponseEntity.status(status).body(R.bizError(ex.getCode(), message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Object> handleNoResourceFound(NoResourceFoundException ex) {
        return R.bizError(404, "接口不存在: " + ex.getResourcePath());
    }

    @ExceptionHandler(NestedServletException.class)
    public ResponseEntity<R<Object>> handleNestedServlet(NestedServletException ex) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(ex);
        if (cause instanceof NoResourceFoundException nrf) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(R.bizError(404, "接口不存在: " + nrf.getResourcePath()));
        }
        log.error("NestedServletException: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.bizError(500, "服务器内部错误"));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Object> handleRuntime(RuntimeException ex) {
        log.error("运行时异常: {}", ex.getMessage(), ex);
        return R.bizError(500, "服务器内部错误");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Object> handleGeneral(Exception ex) {
        log.error("系统异常: {}", ex.getMessage(), ex);
        return R.bizError(500, "服务器内部错误");
    }
}
