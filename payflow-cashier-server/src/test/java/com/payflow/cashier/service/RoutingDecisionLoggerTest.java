package com.payflow.cashier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.mapper.RoutingDecisionLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RoutingDecisionLogger 单元测试
 * 验证异步日志写入（正常写入、异常容错）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoutingDecisionLogger 测试")
class RoutingDecisionLoggerTest {

    @Mock
    private RoutingDecisionLogMapper routingDecisionLogMapper;
    private RoutingDecisionLogger logger;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        logger = new RoutingDecisionLogger(routingDecisionLogMapper, objectMapper);
    }

    @Test
    @DisplayName("正常记录路由决策日志")
    void logSuccessfully() {
        when(routingDecisionLogMapper.insert(any(com.payflow.cashier.entity.RoutingDecisionLog.class))).thenReturn(1);

        List<Map<String, Object>> availableChannels = List.of(
                Map.of("code", "unionpay", "feeRate", 0.005),
                Map.of("code", "alipay", "feeRate", 0.0055)
        );

        // 不抛异常即为成功
        assertDoesNotThrow(() ->
                logger.log("TRD20260513001", 1001L, availableChannels,
                        "unionpay", "最低成本路由", 5L, 0)
        );

        ArgumentCaptor<com.payflow.cashier.entity.RoutingDecisionLog> captor =
                ArgumentCaptor.forClass(com.payflow.cashier.entity.RoutingDecisionLog.class);
        verify(routingDecisionLogMapper).insert(captor.capture());

        com.payflow.cashier.entity.RoutingDecisionLog log = captor.getValue();
        assertEquals("TRD20260513001", log.getTradeNo());
        assertEquals(1001L, log.getMerchantId());
        assertEquals("unionpay", log.getSelectedChannel());
        assertEquals("最低成本路由", log.getSelectionReason());
        assertEquals(5, log.getDecisionCostMs());
        assertEquals(0, log.getFallbackCount());
        assertNotNull(log.getAvailableChannels()); // JSON 序列化的可选渠道列表
        assertNotNull(log.getCreateTime());
    }

    @Test
    @DisplayName("数据库异常时不影响主流程")
    void logExceptionDoesNotThrow() {
        when(routingDecisionLogMapper.insert(any(com.payflow.cashier.entity.RoutingDecisionLog.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        List<Map<String, Object>> channels = List.of(
                Map.of("code", "wechat", "feeRate", 0.006)
        );

        // 不抛异常，静默失败
        assertDoesNotThrow(() ->
                logger.log("TRD20260513002", 1002L, channels,
                        "wechat", "指定渠道", 3L, 0)
        );

        verify(routingDecisionLogMapper).insert(any(com.payflow.cashier.entity.RoutingDecisionLog.class));
    }

    @Test
    @DisplayName("记录降级路由决策")
    void logWithFallback() {
        when(routingDecisionLogMapper.insert(any(com.payflow.cashier.entity.RoutingDecisionLog.class))).thenReturn(1);

        List<Map<String, Object>> channels = List.of(
                Map.of("code", "unionpay", "feeRate", 0.005),
                Map.of("code", "alipay", "feeRate", 0.0055),
                Map.of("code", "wechat", "feeRate", 0.006)
        );

        assertDoesNotThrow(() ->
                logger.log("TRD20260513003", 1003L, channels,
                        "alipay", "降级选择: alipay (费率 0.0055), 已降级1次",
                        12L, 1)
        );

        ArgumentCaptor<com.payflow.cashier.entity.RoutingDecisionLog> captor =
                ArgumentCaptor.forClass(com.payflow.cashier.entity.RoutingDecisionLog.class);
        verify(routingDecisionLogMapper).insert(captor.capture());

        assertEquals(1, captor.getValue().getFallbackCount());
        assertTrue(captor.getValue().getSelectionReason().contains("降级"));
    }

    @Test
    @DisplayName("空可用渠道列表正常记录")
    void logWithEmptyChannels() {
        when(routingDecisionLogMapper.insert(any(com.payflow.cashier.entity.RoutingDecisionLog.class))).thenReturn(1);

        assertDoesNotThrow(() ->
                logger.log("TRD20260513004", 1004L, List.of(),
                        "wechat", "默认路由", 1L, 0)
        );

        ArgumentCaptor<com.payflow.cashier.entity.RoutingDecisionLog> captor =
                ArgumentCaptor.forClass(com.payflow.cashier.entity.RoutingDecisionLog.class);
        verify(routingDecisionLogMapper).insert(captor.capture());

        assertEquals("[]", captor.getValue().getAvailableChannels());
    }
}
