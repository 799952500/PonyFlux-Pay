package com.payflow.cashier.controller;

import com.payflow.payment.core.PayStrategyRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付回调统一入口：委托 {@link PayStrategyRegistry} 识别渠道。
  * @author Lucas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
public class PayNotifyController {

    private final PayStrategyRegistry payStrategyRegistry;

    @PostMapping
    public String handleNotify(HttpServletRequest request) {
        String result = payStrategyRegistry.dispatchChannelNotify(request);
        log.info("统一回调路由结果: {}", result);
        return result;
    }
}
