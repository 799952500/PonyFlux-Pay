package com.payflow.admin.kit;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端统一 API 响应体（{@link Map#of} 不允许 null 值）。
 */
public final class AdminApiResponseKit {

    private AdminApiResponseKit() {
    }

    public static Map<String, Object> success(Object data) {
        return body(0, "success", data);
    }

    public static Map<String, Object> error(int code, String message) {
        return body(code, message, null);
    }

    public static Map<String, Object> body(int code, String message, Object data) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("code", code);
        map.put("message", message);
        map.put("data", data);
        return map;
    }
}
