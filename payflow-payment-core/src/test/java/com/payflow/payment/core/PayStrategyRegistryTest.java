package com.payflow.payment.core;

import com.payflow.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayStrategyRegistryTest {

    private PayStrategyRegistry registry;

    @BeforeEach
    void setUp() {
        // 用空列表构建（无实际渠道依赖）
        registry = new PayStrategyRegistry(Collections.emptyList());
    }

    @Test
    void shouldThrowBizExceptionForUnknownPayMethod() {
        assertThrows(BizException.class, () -> registry.requireByCode("NONEXISTENT"));
    }

    @Test
    void shouldBuildEmptyRegistryWithoutErrors() {
        assertNotNull(registry);
    }

    @Test
    void shouldRegisterAllStrategiesWithoutDuplicates() {
        TestStrategy wechat = new TestStrategy(PayMethod.WECHAT_NATIVE);
        TestStrategy alipay = new TestStrategy(PayMethod.ALIPAY_QR);
        PayStrategyRegistry reg = new PayStrategyRegistry(List.of(wechat, alipay));

        assertNotNull(reg.requireByCode("WECHAT_NATIVE"));
        assertNotNull(reg.requireByCode("ALIPAY_QR"));
    }

    @Test
    void shouldThrowOnDuplicateRegistration() {
        TestStrategy s1 = new TestStrategy(PayMethod.WECHAT_NATIVE);
        TestStrategy s2 = new TestStrategy(PayMethod.WECHAT_NATIVE);
        assertThrows(IllegalStateException.class, () -> new PayStrategyRegistry(List.of(s1, s2)));
    }

    private record TestStrategy(PayMethod payMethod) implements PayStrategy {
        @Override
        public PayMethod getPayMethod() { return payMethod; }
        @Override
        public PayResult pay(String o, Long a, String s, String r, String n, ChannelConfigHolder c, java.util.Map<String, String> e) { return null; }
        @Override
        public RefundResult refund(String t, Long a, String r, ChannelConfigHolder c) { return null; }
        @Override
        public NotifyResult parseNotify(jakarta.servlet.http.HttpServletRequest r) { return null; }
    }
}
