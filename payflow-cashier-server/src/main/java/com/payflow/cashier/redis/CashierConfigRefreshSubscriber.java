package com.payflow.cashier.redis;

import com.payflow.cashier.registry.PayChannelAccountRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 收银台配置刷新订阅处理器。
 *
 * @author Lucas
 */
@Slf4j
@RequiredArgsConstructor
public class CashierConfigRefreshSubscriber {

    private final PayChannelAccountRegistry registry;

    public void onMessage(String message) {
        log.info("收到收银台配置刷新事件: payload={}", message);
        try {
            registry.refresh();
        } catch (Exception e) {
            log.error("刷新渠道账户注册表失败", e);
        }
    }
}

