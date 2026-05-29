package com.payflow.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应体（admin / cashier / recon 共用）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return R.<T>builder().code(0).message("success").data(data).build();
    }

    public static <T> R<T> bizError(int code, String message) {
        return R.<T>builder().code(code).message(message).build();
    }

    public static <T> R<T> unauthorized() {
        return R.<T>builder().code(4010).message("未授权: token无效或已过期").build();
    }

    public static <T> R<T> unauthorized(String message) {
        return R.<T>builder().code(4010).message("未授权: " + message).build();
    }

    public static <T> R<T> badRequest(String message) {
        return R.<T>builder().code(4000).message(message).build();
    }

    public static <T> R<T> paramInvalid(String message) {
        return R.<T>builder().code(4001).message("参数错误: " + message).build();
    }

    public static <T> R<T> serverError(String message) {
        return R.<T>builder().code(5000).message("服务器错误: " + message).build();
    }
}
