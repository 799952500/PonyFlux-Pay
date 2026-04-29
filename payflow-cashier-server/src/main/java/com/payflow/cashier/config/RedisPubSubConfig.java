package com.payflow.cashier.config;

import com.payflow.cashier.redis.CashierConfigRefreshSubscriber;
import com.payflow.cashier.registry.PayChannelAccountRegistry;
import com.payflow.common.redis.RedisTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis Pub/Sub 配置：订阅管理端发布的收银台配置刷新事件。
 *
 * @author Lucas
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payflow.cache.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisPubSubConfig {

    @Bean
    public CashierConfigRefreshSubscriber cashierConfigRefreshSubscriber(PayChannelAccountRegistry registry) {
        return new CashierConfigRefreshSubscriber(registry);
    }

    @Bean
    public MessageListenerAdapter cashierConfigRefreshListener(CashierConfigRefreshSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter cashierConfigRefreshListener
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cashierConfigRefreshListener, ChannelTopic.of(RedisTopics.CASHIER_CONFIG_REFRESH));
        return container;
    }
}

