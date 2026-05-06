package com.payflow.recon.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应（与收银台 R 结构一致）。
 *
 * @author PayFlow Team
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

    public static <T> R<T> ok(T data) {
        return R.<T>builder().code(0).message("success").data(data).build();
    }

    public static <T> R<T> bizError(int code, String message) {
        return R.<T>builder().code(code).message(message).build();
    }

    public static <T> R<T> serverError(String message) {
        return R.<T>builder().code(5000).message("服务器错误: " + message).build();
    }
}
