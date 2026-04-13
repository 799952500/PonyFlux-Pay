package com.payflow.cashier.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应结果包装类
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "R", description = "统一响应结果")
public class R<T> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码")
    private int code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "数据体")
    private T data;

    // ==================== 成功响应 ====================

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return R.<T>builder()
                .code(0)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> R<T> ok(T data, String message) {
        return R.<T>builder()
                .code(0)
                .message(message)
                .data(data)
                .build();
    }

    // ==================== 业务错误（6xxx）====================

    public static <T> R<T> bizError(int code, String message) {
        return R.<T>builder().code(code).message(message).build();
    }

    public static <T> R<T> orderNotFound(String orderId) {
        return bizError(6001, "订单不存在: " + orderId);
    }

    public static <T> R<T> orderExpired(String orderId) {
        return bizError(6002, "订单已过期: " + orderId);
    }

    public static <T> R<T> orderStatusError(String message) {
        return bizError(6003, "订单状态异常: " + message);
    }

    public static <T> R<T> duplicateOrder(String merchantOrderNo) {
        return bizError(6006, "商户订单号重复: " + merchantOrderNo);
    }

    // ==================== 客户端错误（4xxx）====================

    public static <T> R<T> badRequest(String message) {
        return R.<T>builder().code(4000).message(message).build();
    }

    public static <T> R<T> paramInvalid(String message) {
        return R.<T>builder().code(4001).message("参数错误: " + message).build();
    }

    public static <T> R<T> unauthorized(String message) {
        return R.<T>builder().code(4010).message("未授权: " + message).build();
    }

    public static <T> R<T> unauthorized() {
        return R.<T>builder().code(4010).message("未授权: token无效或已过期").build();
    }

    // ==================== 服务端错误（5xxx）====================

    public static <T> R<T> serverError(String message) {
        return R.<T>builder().code(5000).message("服务器错误: " + message).build();
    }
}
