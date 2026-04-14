package com.payflow.cashier.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MQ 消费者无操作实现（无 MQ 环境降级用）
 * 当 payflow.mq.enabled=false 时生效
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payflow.mq.enabled", havingValue = "false")
public class OrderMqNoOpConsumer {

    public OrderMqNoOpConsumer() {
        log.warn("[MQ-Stub] OrderMqNoOpConsumer initialized — RocketMQ consumer disabled (payflow.mq.enabled=false)");
    }
}
