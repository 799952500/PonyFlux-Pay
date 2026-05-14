package com.payflow.cashier.service.routing;

import com.payflow.cashier.entity.PayChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 最低成本路由策略：跨渠道比较费率，选择手续费最低的可用渠道。
 * 失败时自动降级到次低成本渠道。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class CostBasedRoutingStrategy {

    /**
     * 从候选渠道中选择费率最低的。
     *
     * @param availableChannels 可用渠道列表（含费率）
     * @return 按费率升序排列的渠道列表（第一个为最优）
     */
    public List<PayChannel> rankByCost(List<PayChannel> availableChannels) {
        if (availableChannels == null || availableChannels.isEmpty()) {
            return Collections.emptyList();
        }
        return availableChannels.stream()
                .sorted(Comparator.comparing(
                        ch -> ch.getFeeRate() != null ? ch.getFeeRate() : BigDecimal.valueOf(0.10),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * 选择最低成本渠道。
     *
     * @param availableChannels 可用渠道列表
     * @return 选中的渠道，如果无可用渠道返回 null
     */
    public PayChannel selectLowestCost(List<PayChannel> availableChannels) {
        List<PayChannel> ranked = rankByCost(availableChannels);
        return ranked.isEmpty() ? null : ranked.get(0);
    }

    /**
     * 获取次低成本渠道（用于降级）。
     *
     * @param rankedChannels 已排名的渠道列表
     * @param failedChannel  已失败的渠道
     * @return 次优渠道或 null
     */
    public PayChannel selectFallback(List<PayChannel> rankedChannels, PayChannel failedChannel) {
        if (rankedChannels == null || rankedChannels.size() <= 1) {
            return null;
        }
        return rankedChannels.stream()
                .filter(ch -> !ch.getChannelCode().equals(failedChannel.getChannelCode()))
                .findFirst()
                .orElse(null);
    }
}
