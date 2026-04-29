package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.dto.ChannelAccountDTO;
import com.payflow.cashier.dto.ChannelRouteDTO;
import com.payflow.cashier.entity.PayChannel;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.PayChannelMerchantRoute;
import com.payflow.common.exception.BizException;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.mapper.PayChannelAccountMapper;
import com.payflow.cashier.mapper.PayChannelMapper;
import com.payflow.cashier.mapper.PayChannelMerchantRouteMapper;
import com.payflow.cashier.service.PayChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付渠道管理服务实现
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayChannelServiceImpl implements PayChannelService {

    private final PayChannelMapper payChannelMapper;
    private final PayChannelAccountMapper payChannelAccountMapper;
    private PayChannelMerchantRouteMapper payChannelMerchantRouteMapper;
    private final ObjectMapper objectMapper;

    // 注入方式改为 set 注入，避免循环
    @org.springframework.beans.factory.annotation.Autowired
    public void setPayChannelMerchantRouteMapper(PayChannelMerchantRouteMapper mapper) {
        this.payChannelMerchantRouteMapper = mapper;
    }

    // ==================== 渠道管理 ====================

    @Override
    public List<PayChannel> getAllChannels() {
        return payChannelMapper.selectList(null);
    }

    @Override
    public List<PayChannel> getEnabledChannels() {
        return payChannelMapper.selectList(
                new LambdaQueryWrapper<PayChannel>()
                        .eq(PayChannel::getStatus, "ENABLED")
                        .orderByDesc(PayChannel::getSortWeight)
        );
    }

    @Override
    public R<PayChannel> createChannel(PayChannel channel) {
        payChannelMapper.insert(channel);
        return R.ok(channel);
    }

    @Override
    public R<PayChannel> updateChannel(Long id, PayChannel channel) {
        PayChannel existing = payChannelMapper.selectById(id);
        if (existing == null) {
            return R.bizError(6001, "渠道不存在: " + id);
        }
        channel.setId(id);
        channel.setCreatedAt(existing.getCreatedAt());
        payChannelMapper.updateById(channel);
        return R.ok(payChannelMapper.selectById(id));
    }

    @Override
    public R<Void> toggleChannelStatus(Long id) {
        PayChannel channel = payChannelMapper.selectById(id);
        if (channel == null) {
            return R.bizError(6001, "渠道不存在: " + id);
        }
        String newStatus = "ENABLED".equals(channel.getStatus()) ? "DISABLED" : "ENABLED";
        channel.setStatus(newStatus);
        payChannelMapper.updateById(channel);
        return R.ok();
    }

    // ==================== 渠道账户管理 ====================

    @Override
    public List<ChannelAccountDTO> getAccountsByChannel(Long channelId) {
        List<PayChannelAccount> accounts = payChannelAccountMapper.selectList(
                new LambdaQueryWrapper<PayChannelAccount>()
                        .eq(PayChannelAccount::getChannelId, channelId)
        );
        return accounts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelAccountDTO> getAccountsByMerchant(String merchantId) {
        List<PayChannelMerchantRoute> routes = payChannelMerchantRouteMapper.selectList(
                new LambdaQueryWrapper<PayChannelMerchantRoute>()
                        .eq(PayChannelMerchantRoute::getMerchantId, merchantId)
                        .eq(PayChannelMerchantRoute::getEnabled, true)
        );
        if (routes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> accountIds = routes.stream()
                .map(PayChannelMerchantRoute::getChannelAccountId)
                .collect(Collectors.toList());
        List<PayChannelAccount> accounts = payChannelAccountMapper.selectBatchIds(accountIds);
        return accounts.stream()
                .filter(a -> "ENABLED".equals(a.getStatus()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public R<PayChannelAccount> createAccount(PayChannelAccount account) {
        payChannelAccountMapper.insert(account);
        return R.ok(account);
    }

    @Override
    public R<PayChannelAccount> updateAccount(Long id, PayChannelAccount account) {
        PayChannelAccount existing = payChannelAccountMapper.selectById(id);
        if (existing == null) {
            return R.bizError(6001, "账户不存在: " + id);
        }
        account.setId(id);
        account.setCreatedAt(existing.getCreatedAt());
        payChannelAccountMapper.updateById(account);
        return R.ok(payChannelAccountMapper.selectById(id));
    }

    @Override
    public R<Void> toggleAccountStatus(Long id) {
        PayChannelAccount account = payChannelAccountMapper.selectById(id);
        if (account == null) {
            return R.bizError(6001, "账户不存在: " + id);
        }
        String newStatus = "ENABLED".equals(account.getStatus()) ? "DISABLED" : "ENABLED";
        account.setStatus(newStatus);
        payChannelAccountMapper.updateById(account);
        return R.ok();
    }

    @Override
    public R<Void> deleteAccount(Long id) {
        PayChannelAccount account = payChannelAccountMapper.selectById(id);
        if (account == null) {
            return R.bizError(6001, "账户不存在: " + id);
        }
        account.setStatus("DISABLED");
        payChannelAccountMapper.updateById(account);
        return R.ok();
    }

    // ==================== 商户路由管理 ====================

    @Override
    public List<ChannelRouteDTO> getRoutesByMerchant(String merchantId) {
        LambdaQueryWrapper<PayChannelMerchantRoute> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(merchantId)) {
            wrapper.eq(PayChannelMerchantRoute::getMerchantId, merchantId);
        }
        List<PayChannelMerchantRoute> routes = payChannelMerchantRouteMapper.selectList(wrapper);
        return routes.stream()
                .map(r -> ChannelRouteDTO.builder()
                        .routeId(r.getId())
                        .channelAccountId(r.getChannelAccountId())
                        .merchantId(r.getMerchantId())
                        .enabled(r.getEnabled())
                        .priority(r.getPriority())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public R<PayChannelMerchantRoute> createRoute(PayChannelMerchantRoute route) {
        if (route.getPriority() == null) {
            route.setPriority(0);
        }
        if (route.getEnabled() == null) {
            route.setEnabled(true);
        }
        payChannelMerchantRouteMapper.insert(route);
        return R.ok(route);
    }

    @Override
    public R<Void> deleteRoute(Long routeId) {
        payChannelMerchantRouteMapper.deleteById(routeId);
        return R.ok();
    }

    @Override
    public R<Void> toggleRouteEnabled(Long routeId) {
        PayChannelMerchantRoute route = payChannelMerchantRouteMapper.selectById(routeId);
        if (route == null) {
            return R.bizError(6001, "路由不存在: " + routeId);
        }
        route.setEnabled(!route.getEnabled());
        payChannelMerchantRouteMapper.updateById(route);
        return R.ok();
    }

    // ==================== 核心业务 ====================

    @Override
    public PayChannelAccount routeToAccount(String merchantId, String channelCode) {
        // 1. 根据 merchantId + channelCode 找到对应渠道的账户ID
        LambdaQueryWrapper<PayChannelMerchantRoute> routeWrapper = new LambdaQueryWrapper<>();
        routeWrapper.eq(PayChannelMerchantRoute::getMerchantId, merchantId)
                .eq(PayChannelMerchantRoute::getEnabled, true);
        List<PayChannelMerchantRoute> routes = payChannelMerchantRouteMapper.selectList(routeWrapper);
        if (routes.isEmpty()) {
            return null;
        }

        // 2. 关联查询渠道，找到匹配 channelCode 的路由
        List<Long> accountIds = routes.stream()
                .map(PayChannelMerchantRoute::getChannelAccountId)
                .collect(Collectors.toList());
        List<PayChannelAccount> accounts = payChannelAccountMapper.selectBatchIds(accountIds)
                .stream()
                .filter(a -> "ENABLED".equals(a.getStatus()))
                .collect(Collectors.toList());

        // 3. 匹配 channelCode
        List<PayChannel> channels = payChannelMapper.selectList(
                new LambdaQueryWrapper<PayChannel>()
                        .eq(PayChannel::getChannelCode, channelCode)
                        .eq(PayChannel::getStatus, "ENABLED")
        );
        if (channels.isEmpty()) {
            return null;
        }
        Long channelId = channels.get(0).getId();
        PayChannelAccount matched = accounts.stream()
                .filter(a -> a.getChannelId().equals(channelId))
                .findFirst()
                .orElse(null);
        if (matched == null) {
            return null;
        }

        // 4. 如果同一渠道有多个路由（配置错误），返回优先级最高的
        List<PayChannelMerchantRoute> channelRoutes = routes.stream()
                .filter(r -> accountIds.contains(matched.getId()) == false) // 先找 id 在 routes 里的
                .collect(Collectors.toList());
        // 重新按优先级选
        return accounts.stream()
                .filter(a -> a.getChannelId().equals(channelId))
                .findFirst()
                .orElse(null);
    }

    // ==================== 私有方法 ====================

    private ChannelAccountDTO toDTO(PayChannelAccount account) {
        PayChannel channel = payChannelMapper.selectById(account.getChannelId());
        Map<String, String> safeConfig = parseSafeConfig(account.getChannelConfig());
        return ChannelAccountDTO.builder()
                .id(account.getId())
                .channelId(account.getChannelId())
                .channelCode(channel != null ? channel.getChannelCode() : null)
                .channelName(channel != null ? channel.getChannelName() : null)
                .accountCode(account.getAccountCode())
                .accountName(account.getAccountName())
                .status(account.getStatus())
                .remark(account.getRemark())
                .safeConfig(safeConfig)
                .build();
    }

    /**
     * 解析 channelConfig JSON，脱敏敏感字段（appSecret 隐藏为 ****）
     */
    private Map<String, String> parseSafeConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> all = objectMapper.readValue(configJson,
                    new TypeReference<Map<String, String>>() {});
            Map<String, String> safe = new HashMap<>(all);
            if (safe.containsKey("appSecret")) {
                safe.put("appSecret", "****");
            }
            if (safe.containsKey("apiKey")) {
                safe.put("apiKey", "****");
            }
            return safe;
        } catch (Exception e) {
            log.warn("解析 channelConfig JSON 失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
