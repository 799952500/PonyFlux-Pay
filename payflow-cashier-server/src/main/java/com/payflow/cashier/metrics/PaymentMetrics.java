package com.payflow.cashier.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 支付业务自定义 Micrometer 指标。
 *
 * @author PayFlow Team
 */
@Component
public class PaymentMetrics {

    private final MeterRegistry registry;

    public PaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 记录支付成功 */
    public void recordSuccess(String channelCode) {
        counter("payment.success.count", channelCode).increment();
    }

    /** 记录支付失败 */
    public void recordFailure(String channelCode, String reason) {
        Counter c = Counter.builder("payment.failure.count")
                .tag("channel", channelCode != null ? channelCode : "UNKNOWN")
                .tag("reason", reason != null ? reason : "unknown")
                .description("支付失败次数")
                .register(registry);
        c.increment();
    }

    /** 记录渠道调用延迟 */
    public void recordChannelDuration(String channelCode, long durationMs) {
        Timer t = Timer.builder("payment.channel.duration")
                .tag("channel", channelCode != null ? channelCode : "UNKNOWN")
                .description("支付渠道调用延迟")
                .register(registry);
        t.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /** 记录退款成功 */
    public void recordRefundSuccess(String channelCode) {
        counter("refund.success.count", channelCode).increment();
    }

    /** 记录退款失败 */
    public void recordRefundFailure(String channelCode, String reason) {
        Counter c = Counter.builder("refund.failure.count")
                .tag("channel", channelCode != null ? channelCode : "UNKNOWN")
                .tag("reason", reason != null ? reason : "unknown")
                .description("退款失败次数")
                .register(registry);
        c.increment();
    }

    private Counter counter(String name, String channelCode) {
        return Counter.builder(name)
                .tag("channel", channelCode != null ? channelCode : "UNKNOWN")
                .description("支付成功次数")
                .register(registry);
    }
}
