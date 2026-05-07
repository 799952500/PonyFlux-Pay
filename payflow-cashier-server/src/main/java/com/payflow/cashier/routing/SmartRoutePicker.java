package com.payflow.cashier.routing;

import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.PayChannelMerchantRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能路由：多账户时按路由优先级与健康度（失败率）选择账户，失败率过高且样本足够则熔断跳过。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartRoutePicker {

    private static final double CIRCUIT_THRESHOLD = 0.55;
    private static final int MIN_SAMPLES = 8;

    private final ChannelHealthRedisService channelHealthRedisService;

    /**
     * 从候选账户中选择一个；单候选直接返回。
     *
     * @param routes        商户路由（含 channelAccountId、priority）
     * @param candidates    已过滤为同一业务渠道且状态 ENABLED 的账户
     * @param channelId     渠道主键
     * @return 选中账户及原因说明（用于日志/追踪）
     */
    public RoutePick pick(List<PayChannelMerchantRoute> routes,
                          List<PayChannelAccount> candidates,
                          Long channelId) {
        if (candidates == null || candidates.isEmpty()) {
            return new RoutePick(null, "无候选账户");
        }
        if (candidates.size() == 1) {
            return new RoutePick(candidates.get(0), "单账户");
        }
        Map<Long, Integer> priorityByAccountId = routes.stream()
                .filter(r -> r.getChannelAccountId() != null)
                .collect(Collectors.toMap(
                        PayChannelMerchantRoute::getChannelAccountId,
                        r -> r.getPriority() == null ? 0 : r.getPriority(),
                        Math::max));

        List<PayChannelAccount> filtered = candidates.stream()
                .filter(a -> a.getChannelId() != null && a.getChannelId().equals(channelId))
                .filter(a -> !channelHealthRedisService.isCircuitOpen(a.getAccountCode(), CIRCUIT_THRESHOLD, MIN_SAMPLES))
                .sorted(Comparator
                        .comparing((PayChannelAccount a) -> failureScore(a.getAccountCode()))
                        .thenComparing((PayChannelAccount a) -> priorityByAccountId.getOrDefault(a.getId(), 0), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            PayChannelAccount fallback = candidates.stream()
                    .filter(a -> a.getChannelId() != null && a.getChannelId().equals(channelId))
                    .findFirst()
                    .orElse(candidates.get(0));
            log.warn("智能路由: 全部账户处于熔断阈值，回退首个账户: accountCode={}", fallback.getAccountCode());
            return new RoutePick(fallback, "熔断回退");
        }
        PayChannelAccount best = filtered.get(0);
        return new RoutePick(best, "健康度优先,priority=" + priorityByAccountId.getOrDefault(best.getId(), 0));
    }

    private double failureScore(String accountCode) {
        return channelHealthRedisService.failureRate(accountCode);
    }

    /**
     * 路由选择结果。
     */
    public record RoutePick(PayChannelAccount account, String reason) {
    }
}
