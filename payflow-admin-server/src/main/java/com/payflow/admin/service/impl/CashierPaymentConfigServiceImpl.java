package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.Channel;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.kit.ClientScopesKit;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.service.CashierPaymentConfigService;
import com.payflow.admin.service.MerchantPaymentRouteService;
import com.payflow.admin.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class CashierPaymentConfigServiceImpl implements CashierPaymentConfigService {

    private final ChannelMapper channelMapper;
    private final PaymentMethodService paymentMethodService;
    private final MerchantPaymentRouteService merchantPaymentRouteService;

    @Override
    public List<Map<String, Object>> listPaymentMethodsForCashier(String merchantId, String orderChannelCode) {
        String channelType = mapOrderChannelToChannelType(orderChannelCode);
        if (channelType == null || merchantId == null || merchantId.isBlank()) {
            return List.of();
        }

        List<Channel> channels = channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .eq(Channel::getChannelType, channelType)
                .eq(Channel::getEnabled, true));
        Set<Long> channelIds = channels.stream().map(Channel::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (channelIds.isEmpty()) {
            return List.of();
        }

        Map<Long, PaymentMethod> methodById = paymentMethodService.listAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()) && m.getChannelId() != null
                        && channelIds.contains(m.getChannelId()))
                .collect(Collectors.toMap(PaymentMethod::getId, m -> m, (a, b) -> a));

        if (methodById.isEmpty()) {
            return List.of();
        }

        return merchantPaymentRouteService.listByMerchantId(merchantId).stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .filter(r -> methodById.containsKey(r.getPaymentMethodId()))
                .map(r -> {
                    PaymentMethod pm = methodById.get(r.getPaymentMethodId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("methodCode", pm.getMethodCode());
                    row.put("methodName", pm.getMethodName());
                    row.put("description", pm.getDescription());
                    row.put("priority", r.getPriority());
                    row.put("clientScopes", ClientScopesKit.parseToList(r.getClientScopes()));
                    return row;
                })
                .collect(Collectors.toList());
    }

    /**
     * 订单渠道 → 渠道表 channel_type
     */
    private static String mapOrderChannelToChannelType(String orderChannelCode) {
        if (orderChannelCode == null) {
            return null;
        }
        return switch (orderChannelCode) {
            case "WECHAT_PAY" -> "WECHAT";
            case "ALIPAY" -> "ALIPAY";
            case "UNION_PAY" -> "UNION";
            default -> null;
        };
    }
}
