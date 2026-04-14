package com.payflow.cashier.config;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 * 定义 Topic 和 ConsumerGroup 常量
 *
 * @author PayFlow Team
 */
@Configuration
public class MqConfig {

    // ==================== Topic 定义 ====================

    /** 订单超时处理 Topic */
    public static final String TOPIC_ORDER_TIMEOUT = "order_timeout";

    /** 支付结果回调商户 Topic */
    public static final String TOPIC_MERCHANT_NOTIFY = "merchant_notify";

    /** 支付结果通知 Topic */
    public static final String TOPIC_PAYMENT_RESULT = "payment_result";

    // ==================== ConsumerGroup 定义 ====================

    /** 订单超时处理 ConsumerGroup */
    public static final String CG_ORDER_TIMEOUT = "payflow-cashier-consumer-timeout";

    /** 商户回调 ConsumerGroup */
    public static final String CG_MERCHANT_NOTIFY = "payflow-cashier-consumer-notify";

    /** 支付结果 ConsumerGroup */
    public static final String CG_PAYMENT_RESULT = "payflow-cashier-consumer-payment";
}
