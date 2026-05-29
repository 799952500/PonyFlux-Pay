package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.Channel;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.entity.cashier.CashierChannel;
import com.payflow.admin.entity.cashier.CashierChannelAccount;
import com.payflow.admin.entity.cashier.CashierChannelMerchantRoute;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.mapper.PaymentAccountMapper;
import com.payflow.admin.mapper.PaymentMethodMapper;
import com.payflow.admin.mapper.cashier.CashierChannelAccountMapper;
import com.payflow.admin.mapper.cashier.CashierChannelMapper;
import com.payflow.admin.mapper.cashier.CashierChannelMerchantRouteMapper;
import com.payflow.admin.redis.CashierConfigRefreshPublisher;
import com.payflow.admin.service.MerchantCashierRouteSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商户支付配置 → 收银台实付路由同步（方案 A）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantCashierRouteSyncServiceImpl implements MerchantCashierRouteSyncService {

    private static final String CASHIER_ACCOUNT_ENABLED = "ENABLED";

    private final MerchantPaymentRouteMapper merchantPaymentRouteMapper;
    private final PaymentMethodMapper paymentMethodMapper;
    private final PaymentAccountMapper paymentAccountMapper;
    private final ChannelMapper channelMapper;
    private final CashierChannelMapper cashierChannelMapper;
    private final CashierChannelAccountMapper cashierChannelAccountMapper;
    private final CashierChannelMerchantRouteMapper cashierRouteMapper;
    private final ObjectProvider<CashierConfigRefreshPublisher> refreshPublisher;

    @Override
    public void syncAndNotify(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalArgumentException("商户号不能为空");
        }
        syncMerchantRoutes(merchantId);
        refreshPublisher.ifAvailable(p -> p.publish("merchant_payment_route:sync"));
    }

    @Transactional(transactionManager = "cashierTransactionManager")
    protected void syncMerchantRoutes(String merchantId) {
        cashierRouteMapper.delete(new LambdaQueryWrapper<CashierChannelMerchantRoute>()
                .eq(CashierChannelMerchantRoute::getMerchantId, merchantId));

        List<MerchantPaymentRoute> adminRoutes = merchantPaymentRouteMapper.selectList(
                new LambdaQueryWrapper<MerchantPaymentRoute>()
                        .eq(MerchantPaymentRoute::getMerchantId, merchantId)
                        .orderByDesc(MerchantPaymentRoute::getPriority)
                        .orderByAsc(MerchantPaymentRoute::getId));
        if (adminRoutes.isEmpty()) {
            return;
        }

        Set<Long> methodIds = new HashSet<>();
        Set<Long> accountIds = new HashSet<>();
        for (MerchantPaymentRoute route : adminRoutes) {
            if (!Boolean.TRUE.equals(route.getEnabled())) {
                continue;
            }
            if (route.getPaymentMethodId() != null) {
                methodIds.add(route.getPaymentMethodId());
            }
            if (route.getPaymentAccountId() != null) {
                accountIds.add(route.getPaymentAccountId());
            }
        }
        Map<Long, PaymentMethod> methodMap = methodIds.isEmpty()
                ? Map.of()
                : paymentMethodMapper.selectBatchIds(methodIds).stream()
                .collect(Collectors.toMap(PaymentMethod::getId, m -> m, (a, b) -> a));
        Map<Long, PaymentAccount> accountMap = accountIds.isEmpty()
                ? Map.of()
                : paymentAccountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(PaymentAccount::getId, a -> a, (x, y) -> x));
        Set<Long> channelIds = accountMap.values().stream()
                .map(PaymentAccount::getChannelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Channel> channelMap = channelIds.isEmpty()
                ? Map.of()
                : channelMapper.selectBatchIds(channelIds).stream()
                .collect(Collectors.toMap(Channel::getId, c -> c, (a, b) -> a));
        Map<String, CashierChannel> cashierChannelByCode = loadCashierChannels(channelMap.values());

        Map<Long, RouteDraft> byAccountId = new HashMap<>();
        for (MerchantPaymentRoute route : adminRoutes) {
            if (!Boolean.TRUE.equals(route.getEnabled())) {
                continue;
            }
            if (route.getPaymentMethodId() == null || route.getPaymentAccountId() == null) {
                continue;
            }
            PaymentMethod method = methodMap.get(route.getPaymentMethodId());
            PaymentAccount account = accountMap.get(route.getPaymentAccountId());
            if (method == null || account == null) {
                throw new IllegalArgumentException(String.format(
                        "商户 %s 路由配置无效: paymentMethodId=%s paymentAccountId=%s",
                        merchantId, route.getPaymentMethodId(), route.getPaymentAccountId()));
            }
            if (!method.getChannelId().equals(account.getChannelId())) {
                throw new IllegalArgumentException(String.format(
                        "商户 %s 路由中支付方式与账号渠道不一致: methodId=%d accountId=%d",
                        merchantId, route.getPaymentMethodId(), route.getPaymentAccountId()));
            }

            Channel adminChannel = channelMap.get(account.getChannelId());
            Long cashierAccountId = resolveCashierChannelAccountId(account, adminChannel, cashierChannelByCode);
            int priority = route.getPriority() != null ? route.getPriority() : 0;
            byAccountId.merge(cashierAccountId, new RouteDraft(cashierAccountId, priority),
                    (a, b) -> b.priority > a.priority ? b : a);
        }

        LocalDateTime now = LocalDateTime.now();
        for (RouteDraft draft : byAccountId.values()) {
            CashierChannelMerchantRoute row = new CashierChannelMerchantRoute();
            row.setMerchantId(merchantId);
            row.setChannelAccountId(draft.channelAccountId);
            row.setEnabled(true);
            row.setPriority(draft.priority);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            cashierRouteMapper.insert(row);
        }
    }

    private Map<String, CashierChannel> loadCashierChannels(Iterable<Channel> channels) {
        Set<String> codes = new HashSet<>();
        for (Channel channel : channels) {
            if (channel != null && channel.getChannelCode() != null) {
                codes.add(channel.getChannelCode().toUpperCase(Locale.ROOT));
            }
        }
        if (codes.isEmpty()) {
            return Map.of();
        }
        List<CashierChannel> list = cashierChannelMapper.selectList(null);
        Map<String, CashierChannel> map = new HashMap<>();
        for (CashierChannel ch : list) {
            if (ch.getChannelCode() != null) {
                map.putIfAbsent(ch.getChannelCode().toUpperCase(Locale.ROOT), ch);
            }
        }
        return map;
    }

    private Long resolveCashierChannelAccountId(PaymentAccount adminAccount,
                                               Channel adminChannel,
                                               Map<String, CashierChannel> cashierChannelByCode) {
        if (adminChannel == null || adminChannel.getChannelCode() == null) {
            throw new IllegalArgumentException("管理端渠道不存在: channelId=" + adminAccount.getChannelId());
        }
        String codeKey = adminChannel.getChannelCode().toUpperCase(Locale.ROOT);
        CashierChannel cashierChannel = cashierChannelByCode.get(codeKey);
        if (cashierChannel == null) {
            throw new IllegalArgumentException(String.format(
                    "收银台未配置渠道 %s，无法同步账号 %s",
                    adminChannel.getChannelCode(), adminAccount.getAccountCode()));
        }

        CashierChannelAccount exact = cashierChannelAccountMapper.selectOne(
                new LambdaQueryWrapper<CashierChannelAccount>()
                        .eq(CashierChannelAccount::getChannelId, cashierChannel.getId())
                        .eq(CashierChannelAccount::getAccountCode, adminAccount.getAccountCode())
                        .eq(CashierChannelAccount::getStatus, CASHIER_ACCOUNT_ENABLED)
                        .last("LIMIT 1"));
        if (exact != null) {
            return exact.getId();
        }

        CashierChannelAccount fallback = cashierChannelAccountMapper.selectOne(
                new LambdaQueryWrapper<CashierChannelAccount>()
                        .eq(CashierChannelAccount::getChannelId, cashierChannel.getId())
                        .eq(CashierChannelAccount::getStatus, CASHIER_ACCOUNT_ENABLED)
                        .orderByDesc(CashierChannelAccount::getId)
                        .last("LIMIT 1"));
        if (fallback == null) {
            throw new IllegalArgumentException(String.format(
                    "收银台渠道 %s 下无可用账户，无法同步管理端账号 %s",
                    adminChannel.getChannelCode(), adminAccount.getAccountCode()));
        }
        log.warn("管理端账号 {} 在收银台无同编码账户，已回退为渠道 {} 下账户 {}",
                adminAccount.getAccountCode(), adminChannel.getChannelCode(), fallback.getAccountCode());
        return fallback.getId();
    }

    private record RouteDraft(Long channelAccountId, int priority) {
    }
}
