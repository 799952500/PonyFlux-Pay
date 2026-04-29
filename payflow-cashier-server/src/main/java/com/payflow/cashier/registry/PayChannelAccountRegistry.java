package com.payflow.cashier.registry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.PayChannel;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.PayChannelMerchantRoute;
import com.payflow.cashier.mapper.PayChannelAccountMapper;
import com.payflow.cashier.mapper.PayChannelMapper;
import com.payflow.cashier.mapper.PayChannelMerchantRouteMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 渠道账户与商户路由内存注册表：启动加载，支持刷新。
 * <p>
 * 用于将「merchantId + payChannel」快速路由到可用的渠道账户，避免每次请求查询数据库。
 * </p>
 *
 * @author Lucas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayChannelAccountRegistry {

    private final PayChannelMapper payChannelMapper;
    private final PayChannelAccountMapper payChannelAccountMapper;
    private final PayChannelMerchantRouteMapper payChannelMerchantRouteMapper;

    private final AtomicReference<Snapshot> snapshotRef = new AtomicReference<>(Snapshot.empty());

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 刷新内存快照（原子替换）。
     */
    public void refresh() {
        Snapshot newSnapshot = loadSnapshot();
        snapshotRef.set(newSnapshot);
        log.info("渠道账户注册表刷新完成: channels={}, accounts={}, routes={}",
                newSnapshot.channelCodeToId.size(),
                newSnapshot.accountIdToAccount.size(),
                newSnapshot.merchantIdToRoutes.size());
    }

    /**
     * 按商户与渠道路由到可用账户。
     *
     * @param merchantId 商户号
     * @param payChannel 支付渠道（订单/支付记录中的 payChannel，如 WECHAT_PAY、ALIPAY）
     * @return 匹配的渠道账户，不存在返回 null
     */
    public PayChannelAccount routeToAccount(String merchantId, String payChannel) {
        if (merchantId == null || merchantId.isBlank() || payChannel == null || payChannel.isBlank()) {
            return null;
        }
        Snapshot s = snapshotRef.get();
        Long channelId = s.channelCodeToId.get(normalizeChannelCode(payChannel));
        if (channelId == null) {
            return null;
        }
        List<RouteEntry> routes = s.merchantIdToRoutes.getOrDefault(merchantId, List.of());
        if (routes.isEmpty()) {
            return null;
        }
        for (RouteEntry r : routes) {
            PayChannelAccount acc = s.accountIdToAccount.get(r.channelAccountId);
            if (acc == null) {
                continue;
            }
            if (!Objects.equals(channelId, acc.getChannelId())) {
                continue;
            }
            if (!"ENABLED".equals(acc.getStatus())) {
                continue;
            }
            return acc;
        }
        return null;
    }

    /**
     * 当前快照版本信息（调试用）。
     */
    public SnapshotInfo snapshotInfo() {
        Snapshot s = snapshotRef.get();
        return new SnapshotInfo(s.channelCodeToId.size(), s.accountIdToAccount.size(), s.merchantIdToRoutes.size());
    }

    private Snapshot loadSnapshot() {
        // 1) 渠道 code -> id（仅 ENABLED）
        Map<String, Long> channelCodeToId = new HashMap<>();
        List<PayChannel> channels = payChannelMapper.selectList(
                new LambdaQueryWrapper<PayChannel>().eq(PayChannel::getStatus, "ENABLED"));
        for (PayChannel c : channels) {
            if (c.getChannelCode() != null && c.getId() != null) {
                channelCodeToId.put(c.getChannelCode(), c.getId());
            }
        }

        // 2) accountId -> account（全部，后续按 ENABLED 过滤）
        Map<Long, PayChannelAccount> accountIdToAccount = new HashMap<>();
        List<PayChannelAccount> accounts = payChannelAccountMapper.selectList(null);
        for (PayChannelAccount a : accounts) {
            if (a.getId() != null) {
                accountIdToAccount.put(a.getId(), a);
            }
        }

        // 3) merchantId -> routes（enabled=true，按 priority desc 排序）
        Map<String, List<RouteEntry>> merchantIdToRoutes = new HashMap<>();
        List<PayChannelMerchantRoute> routes = payChannelMerchantRouteMapper.selectList(
                new LambdaQueryWrapper<PayChannelMerchantRoute>().eq(PayChannelMerchantRoute::getEnabled, true));
        for (PayChannelMerchantRoute r : routes) {
            if (r.getMerchantId() == null || r.getMerchantId().isBlank() || r.getChannelAccountId() == null) {
                continue;
            }
            merchantIdToRoutes.computeIfAbsent(r.getMerchantId(), k -> new ArrayList<>())
                    .add(new RouteEntry(r.getChannelAccountId(), r.getPriority() == null ? 0 : r.getPriority()));
        }
        for (Map.Entry<String, List<RouteEntry>> e : merchantIdToRoutes.entrySet()) {
            e.getValue().sort(Comparator.comparingInt(RouteEntry::getPriority).reversed());
            e.setValue(Collections.unmodifiableList(e.getValue()));
        }

        return new Snapshot(Collections.unmodifiableMap(channelCodeToId),
                Collections.unmodifiableMap(accountIdToAccount),
                Collections.unmodifiableMap(merchantIdToRoutes));
    }

    private String normalizeChannelCode(String payChannel) {
        // 订单/支付中存的是 WECHAT_PAY/ALIPAY，而渠道表 channel_code 存的是 wechat_pay/alipay
        return payChannel.toLowerCase(Locale.ROOT);
    }

    private record RouteEntry(Long channelAccountId, int priority) {
        public int getPriority() {
            return priority;
        }
    }

    private record Snapshot(Map<String, Long> channelCodeToId,
                            Map<Long, PayChannelAccount> accountIdToAccount,
                            Map<String, List<RouteEntry>> merchantIdToRoutes) {
        static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of(), Map.of());
        }
    }

    @Getter
    public static class SnapshotInfo {
        private final int channelCount;
        private final int accountCount;
        private final int merchantRouteCount;

        public SnapshotInfo(int channelCount, int accountCount, int merchantRouteCount) {
            this.channelCount = channelCount;
            this.accountCount = accountCount;
            this.merchantRouteCount = merchantRouteCount;
        }
    }
}

