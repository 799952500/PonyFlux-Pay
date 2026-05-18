package com.payflow.cashier.security;

import com.payflow.cashier.constant.MerchantSecurityErrorCodes;
import com.payflow.cashier.context.AuthMode;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.service.SecurityAuditService;
import com.payflow.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * merchantId 绑定校验单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantIdGuard 测试")
class MerchantIdGuardTest {

    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private MerchantIdGuard merchantIdGuard;

    @AfterEach
    void tearDown() {
        MerchantContext.clear();
    }

    @Test
    @DisplayName("请求未带 merchantId 时放行")
    void allowsWhenBodyMerchantIdAbsent() {
        MerchantContext.set("M001", AuthMode.JWT, "/api/v1/orders", "127.0.0.1");
        assertDoesNotThrow(() -> merchantIdGuard.assertMatchesContext(
                null, "POST", "/api/v1/orders", "127.0.0.1", "test"));
    }

    @Test
    @DisplayName("merchantId 与上下文一致时放行")
    void allowsWhenMerchantIdMatches() {
        MerchantContext.set("M001", AuthMode.JWT, "/api/v1/orders", "127.0.0.1");
        assertDoesNotThrow(() -> merchantIdGuard.assertMatchesContext(
                "M001", "POST", "/api/v1/orders", "127.0.0.1", "test"));
        verify(securityAuditService, never()).recordDenied(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("merchantId 不一致时拒绝并审计")
    void deniesWhenMerchantIdMismatch() {
        MerchantContext.set("M001", AuthMode.JWT, "/api/v1/orders", "127.0.0.1");
        BizException ex = assertThrows(BizException.class, () -> merchantIdGuard.assertMatchesContext(
                "M002", "POST", "/api/v1/orders", "127.0.0.1", "test"));
        assertEquals(MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH, ex.getCode());
        verify(securityAuditService).recordDenied(
                eq("M001"), eq("M002"), eq(AuthMode.JWT), eq("POST"), eq("/api/v1/orders"),
                isNull(), isNull(), eq("127.0.0.1"), eq("test"),
                eq(MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH), anyString());
    }
}
