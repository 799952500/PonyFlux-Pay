package com.payflow.cashier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.entity.RoutingDecisionLog;
import com.payflow.cashier.mapper.RoutingDecisionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 路由决策日志记录器（异步写入，不阻塞支付主流程）。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingDecisionLogger {

    private final RoutingDecisionLogMapper routingDecisionLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 异步记录路由决策。
     */
    @Async
    public void log(String tradeNo, Long merchantId, List<Map<String, Object>> availableChannels,
                    String selectedChannel, String selectionReason, long decisionCostMs, int fallbackCount) {
        try {
            String channelsJson = objectMapper.writeValueAsString(availableChannels);

            RoutingDecisionLog decisionLog = new RoutingDecisionLog();
            decisionLog.setTradeNo(tradeNo);
            decisionLog.setMerchantId(merchantId);
            decisionLog.setAvailableChannels(channelsJson);
            decisionLog.setSelectedChannel(selectedChannel);
            decisionLog.setSelectionReason(selectionReason);
            decisionLog.setDecisionCostMs((int) decisionCostMs);
            decisionLog.setFallbackCount(fallbackCount);
            decisionLog.setCreateTime(LocalDateTime.now());

            routingDecisionLogMapper.insert(decisionLog);
        } catch (Exception e) {
            log.error("记录路由决策日志失败: tradeNo={}", tradeNo, e);
        }
    }
}
