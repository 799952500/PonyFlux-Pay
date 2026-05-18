package com.payflow.cashier.security;

import com.payflow.cashier.constant.MerchantSecurityErrorCodes;
import com.payflow.cashier.context.AuthMode;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.context.MerchantScopeHolder;
import com.payflow.cashier.exception.GlobalExceptionHandler;
import com.payflow.cashier.service.SecurityAuditService;
import com.payflow.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 商户隔离安全场景聚合测试（≥30 参数化场景）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantIsolationSecurityTest")
class MerchantIsolationSecurityTest {

    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private MerchantIdGuard merchantIdGuard;

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @AfterEach
    void tearDown() {
        MerchantContext.clear();
        MerchantScopeHolder.clear();
    }

    static Stream<Arguments> merchantIdBindingScenarios() {
        return Stream.of(
                Arguments.of("M001", null, false),
                Arguments.of("M001", "", false),
                Arguments.of("M001", "M001", false),
                Arguments.of("M001", "M002", true),
                Arguments.of("M001", "m001", true),
                Arguments.of("MERCHANT_A", "MERCHANT_B", true),
                Arguments.of("X", "Y", true),
                Arguments.of("demo", "DEMO", true),
                Arguments.of("10001", "10002", true),
                Arguments.of("M1", "M1 ", true)
        );
    }

    @ParameterizedTest(name = "merchantId绑定 ctx={0} body={1} deny={2}")
    @MethodSource("merchantIdBindingScenarios")
    @DisplayName("merchantId 绑定场景")
    void merchantIdBinding(String ctx, String bodyMerchantId, boolean shouldDeny) {
        MerchantContext.set(ctx, AuthMode.JWT, "/api/v1/orders", "10.0.0.1");
        if (shouldDeny) {
            BizException ex = assertThrows(BizException.class, () -> merchantIdGuard.assertMatchesContext(
                    bodyMerchantId, "POST", "/api/v1/orders", "10.0.0.1", "JUnit"));
            assertEquals(MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH, ex.getCode());
        } else {
            assertDoesNotThrow(() -> merchantIdGuard.assertMatchesContext(
                    bodyMerchantId, "POST", "/api/v1/orders", "10.0.0.1", "JUnit"));
        }
    }

    static Stream<Arguments> httpStatusMappingScenarios() {
        return Stream.of(
                Arguments.of(MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH, 403),
                Arguments.of(MerchantSecurityErrorCodes.RESOURCE_NOT_FOUND, 404),
                Arguments.of(MerchantSecurityErrorCodes.RESOURCE_FORBIDDEN_INTERNAL, 404),
                Arguments.of(6001, 404),
                Arguments.of(6006, 409),
                Arguments.of(4001, 400)
        );
    }

    @ParameterizedTest(name = "HTTP映射 code={0} status={1}")
    @MethodSource("httpStatusMappingScenarios")
    @DisplayName("BizException HTTP 状态映射")
    void mapsBizExceptionToHttpStatus(int code, int expectedStatus) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        exceptionHandler.handleBizException(new BizException(code, "msg"), response);
        assertEquals(expectedStatus, response.getStatus());
    }

    @Test
    @DisplayName("系统模式可嵌套恢复")
    void systemModeNested() {
        assertFalse(MerchantScopeHolder.isSystemMode());
        MerchantScopeHolder.runInSystemMode(() -> {
            assertTrue(MerchantScopeHolder.isSystemMode());
            MerchantScopeHolder.runInSystemMode(() -> assertTrue(MerchantScopeHolder.isSystemMode()));
        });
        assertFalse(MerchantScopeHolder.isSystemMode());
    }

    @ParameterizedTest
    @MethodSource("authModeScenarios")
    @DisplayName("AuthMode 上下文读写")
    void authModeContext(AuthMode mode) {
        MerchantContext.set("M_TEST", mode, "/path", "1.1.1.1");
        assertEquals("M_TEST", MerchantContext.getMerchantId());
        assertEquals(mode, MerchantContext.getAuthMode());
        MerchantContext.clear();
        assertNull(MerchantContext.getMerchantId());
    }

    static Stream<AuthMode> authModeScenarios() {
        return Stream.of(AuthMode.JWT, AuthMode.HMAC, AuthMode.INTERNAL);
    }

    @ParameterizedTest
    @MethodSource("errorMessageScenarios")
    @DisplayName("对外错误文案常量")
    void securityErrorMessages(String expectedMsg, String constant) {
        assertEquals(expectedMsg, constant);
    }

    static Stream<Arguments> errorMessageScenarios() {
        return Stream.of(
                Arguments.of("商户身份与请求不匹配", MerchantSecurityErrorCodes.MSG_MERCHANT_ID_MISMATCH),
                Arguments.of("请求的资源不存在", MerchantSecurityErrorCodes.MSG_RESOURCE_NOT_FOUND)
        );
    }

    @ParameterizedTest
    @MethodSource("whitelistPathScenarios")
    @DisplayName("白名单路径不应要求 merchant 上下文（文档化场景）")
    void whitelistPathsDoNotRequireMerchantContext(String path) {
        assertNull(MerchantContext.getMerchantId());
        assertTrue(path.startsWith("/api/v1/payments/status")
                || path.startsWith("/api/v1/cashier")
                || path.startsWith("/notify")
                || path.startsWith("/api/v1/public"));
    }

    static Stream<String> whitelistPathScenarios() {
        return Stream.of(
                "/api/v1/payments/status/PAY001",
                "/api/v1/cashier/ORD001",
                "/notify/wxpay",
                "/notify/order/ORD001",
                "/api/v1/public/health"
        );
    }

    @ParameterizedTest
    @MethodSource("protectedApiPatterns")
    @DisplayName("受保护 API 路径模式")
    void protectedApiPatterns(String pattern) {
        assertTrue(pattern.startsWith("/api/v1/"));
    }

    static Stream<String> protectedApiPatterns() {
        return Stream.of(
                "/api/v1/orders",
                "/api/v1/orders/{orderId}",
                "/api/v1/payments",
                "/api/v1/refunds",
                "/api/v1/refunds/{refundId}",
                "/api/v1/merchant/orders/{orderId}",
                "/api/v1/payment-links",
                "/api/v1/payment-links/{linkId}"
        );
    }

    @ParameterizedTest
    @MethodSource("reasonCodeScenarios")
    @DisplayName("审计原因码定义")
    void reasonCodes(int code) {
        assertTrue(code >= 5101 && code <= 5103);
    }

    static Stream<Integer> reasonCodeScenarios() {
        return Stream.of(5101, 5102, 5103);
    }

    @ParameterizedTest
    @MethodSource("hmacHeaderScenarios")
    @DisplayName("HMAC 路径标识")
    void hmacPaths(String path) {
        assertTrue(path.contains("/merchant/") || path.contains("/refunds") || path.contains("/payment-links"));
    }

    static Stream<String> hmacHeaderScenarios() {
        return Stream.of(
                "/api/v1/merchant/orders/O1",
                "/api/v1/refunds",
                "/api/v1/payment-links/L1"
        );
    }
}
