package com.payflow.cashier.service.routing;

import com.payflow.cashier.entity.PayChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CostBasedRoutingStrategy 单元测试
 * 验证最低成本选择（含自动降级和全部不可用场景）
 */
@DisplayName("CostBasedRoutingStrategy 测试")
class CostBasedRoutingStrategyTest {

    private CostBasedRoutingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CostBasedRoutingStrategy();
    }

    @Test
    @DisplayName("按费率升序排列三个渠道")
    void rankByCostAscending() {
        List<PayChannel> channels = List.of(
                channel("wechat", BigDecimal.valueOf(0.006)),
                channel("alipay", BigDecimal.valueOf(0.0055)),
                channel("unionpay", BigDecimal.valueOf(0.005))
        );

        List<PayChannel> ranked = strategy.rankByCost(channels);

        assertEquals(3, ranked.size());
        assertEquals("unionpay", ranked.get(0).getChannelCode()); // 0.005 最低
        assertEquals("alipay", ranked.get(1).getChannelCode());   // 0.0055
        assertEquals("wechat", ranked.get(2).getChannelCode());   // 0.006 最高
    }

    @Test
    @DisplayName("选择最低成本渠道")
    void selectLowestCost() {
        List<PayChannel> channels = List.of(
                channel("wechat", BigDecimal.valueOf(0.006)),
                channel("unionpay", BigDecimal.valueOf(0.005))
        );

        PayChannel selected = strategy.selectLowestCost(channels);

        assertNotNull(selected);
        assertEquals("unionpay", selected.getChannelCode());
    }

    @Test
    @DisplayName("费率相同的渠道保持原始顺序")
    void sameRatePreservesOrder() {
        List<PayChannel> channels = List.of(
                channel("wechat", BigDecimal.valueOf(0.006)),
                channel("alipay", BigDecimal.valueOf(0.006))
        );

        List<PayChannel> ranked = strategy.rankByCost(channels);

        assertEquals(2, ranked.size());
        // 费率相同，保持原始顺序
        assertEquals("wechat", ranked.get(0).getChannelCode());
        assertEquals("alipay", ranked.get(1).getChannelCode());
    }

    @Test
    @DisplayName("费率为null的渠道按默认费率0.10处理")
    void nullFeeRateUsesDefault() {
        List<PayChannel> channels = new ArrayList<>();
        channels.add(channel("wechat", BigDecimal.valueOf(0.006)));
        channels.add(channel("unknown", null)); // 默认 0.10

        List<PayChannel> ranked = strategy.rankByCost(channels);

        assertEquals(2, ranked.size());
        assertEquals("wechat", ranked.get(0).getChannelCode()); // 0.006 < 0.10
        assertEquals("unknown", ranked.get(1).getChannelCode());
    }

    @Test
    @DisplayName("空列表返回空结果")
    void emptyListReturnsEmpty() {
        List<PayChannel> ranked = strategy.rankByCost(Collections.emptyList());
        assertTrue(ranked.isEmpty());

        PayChannel selected = strategy.selectLowestCost(Collections.emptyList());
        assertNull(selected);

        PayChannel fallback = strategy.selectFallback(Collections.emptyList(), null);
        assertNull(fallback);
    }

    @Test
    @DisplayName("单个渠道无降级可用")
    void singleChannelNoFallback() {
        List<PayChannel> channels = List.of(
                channel("wechat", BigDecimal.valueOf(0.006))
        );

        PayChannel selected = strategy.selectLowestCost(channels);
        assertNotNull(selected);
        assertEquals("wechat", selected.getChannelCode());

        PayChannel fallback = strategy.selectFallback(channels, selected);
        assertNull(fallback); // 只有一个渠道，无法降级
    }

    @Test
    @DisplayName("降级选择次低成本渠道")
    void fallbackToNextBest() {
        List<PayChannel> channels = List.of(
                channel("wechat", BigDecimal.valueOf(0.006)),
                channel("alipay", BigDecimal.valueOf(0.0055)),
                channel("unionpay", BigDecimal.valueOf(0.005))
        );

        List<PayChannel> ranked = strategy.rankByCost(channels);
        PayChannel failed = ranked.get(0); // unionpay 最低但假定失败
        assertEquals("unionpay", failed.getChannelCode());

        PayChannel fallback = strategy.selectFallback(ranked, failed);

        assertNotNull(fallback);
        assertEquals("alipay", fallback.getChannelCode()); // 次低成本
    }

    private PayChannel channel(String code, BigDecimal feeRate) {
        PayChannel ch = new PayChannel();
        ch.setChannelCode(code);
        ch.setChannelName(code);
        ch.setFeeRate(feeRate);
        ch.setStatus("ENABLED");
        return ch;
    }
}
