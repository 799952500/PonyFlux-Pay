package com.payflow.admin.kit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MerchantNotifyMaskKit 脱敏测试")
class MerchantNotifyMaskKitTest {

    @Test
    @DisplayName("sign 字段应脱敏")
    void masksSignField() {
        String json = "{\"orderId\":\"ORD1\",\"sign\":\"abcdef1234567890\"}";
        Object masked = MerchantNotifyMaskKit.maskRequestParams(json);
        assertInstanceOf(Map.class, masked);
        @SuppressWarnings("unchecked")
        String sign = String.valueOf(((Map<String, Object>) masked).get("sign"));
        assertTrue(sign.contains("****"));
        assertFalse(sign.contains("abcdef1234567890"));
    }
}
