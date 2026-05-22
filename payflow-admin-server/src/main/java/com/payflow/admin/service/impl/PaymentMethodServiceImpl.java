package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.mapper.PaymentMethodMapper;
import com.payflow.admin.service.PaymentMethodService;
import com.payflow.admin.service.guard.ResourceDeleteGuardService;
import com.payflow.admin.service.guard.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodMapper paymentMethodMapper;
    private final ResourceDeleteGuardService resourceDeleteGuardService;

    @Override
    public Map<String, Object> page(int page, int pageSize, Long channelId, String channelType, String keyword, String status) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = pageSize < 1 ? 20 : Math.min(pageSize, 100);
        List<PaymentMethod> filtered = listAll().stream()
                .filter(pm -> channelId == null || channelId.equals(pm.getChannelId()))
                .filter(pm -> matchesChannelType(pm, channelType))
                .filter(pm -> matchesKeyword(pm, keyword))
                .filter(pm -> matchesStatus(pm, status))
                .toList();
        int total = filtered.size();
        int fromIndex = Math.max(0, (safePage - 1) * safeSize);
        int toIndex = Math.min(total, fromIndex + safeSize);
        List<PaymentMethod> pageList = fromIndex >= total ? List.of() : filtered.subList(fromIndex, toIndex);
        Map<String, Object> data = new HashMap<>();
        data.put("list", pageList);
        data.put("total", total);
        data.put("page", safePage);
        data.put("pageSize", safeSize);
        return data;
    }

    @Override
    public List<PaymentMethod> listAll() {
        List<PaymentMethod> list = paymentMethodMapper.listWithChannelName();
        // 设置 status 字段
        list.forEach(pm -> {
            if (pm.getEnabled() != null && pm.getEnabled()) {
                pm.setStatus("ACTIVE");
            } else {
                pm.setStatus("INACTIVE");
            }
        });
        return list;
    }

    @Override
    public List<PaymentMethod> listByChannelId(Long channelId) {
        return paymentMethodMapper.selectList(new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getChannelId, channelId));
    }

    @Override
    public PaymentMethod getById(Long id) {
        return paymentMethodMapper.selectById(id);
    }

    @Override
    public void create(PaymentMethod method) {
        paymentMethodMapper.insert(method);
    }

    @Override
    public void update(Long id, PaymentMethod method) {
        method.setId(id);
        paymentMethodMapper.updateById(method);
    }

    @Override
    public void delete(Long id) {
        resourceDeleteGuardService.assertDeletable(ResourceType.PAYMENT_METHOD, id);
        paymentMethodMapper.deleteById(id);
    }

    private static boolean matchesChannelType(PaymentMethod pm, String channelType) {
        if (!StringUtils.hasText(channelType)) {
            return true;
        }
        return normalizeChannelType(channelType).equals(normalizeChannelType(nullToEmpty(pm.getChannelType())));
    }

    /**
     * 与 channels.channel_type 对齐：库内银联为 UNION，前端/订单侧常见 UNIONPAY、UNION_PAY。
     */
    private static String normalizeChannelType(String type) {
        if (!StringUtils.hasText(type)) {
            return "";
        }
        String t = type.trim().toUpperCase();
        if ("UNIONPAY".equals(t) || "UNION_PAY".equals(t)) {
            return "UNION";
        }
        if ("WECHAT_PAY".equals(t)) {
            return "WECHAT";
        }
        return t;
    }

    private static boolean matchesKeyword(PaymentMethod pm, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String kw = keyword.trim().toLowerCase();
        return nullToEmpty(pm.getMethodName()).toLowerCase().contains(kw)
                || nullToEmpty(pm.getMethodCode()).toLowerCase().contains(kw);
    }

    private static boolean matchesStatus(PaymentMethod pm, String status) {
        if (!StringUtils.hasText(status)) {
            return true;
        }
        boolean enabled = Boolean.TRUE.equals(pm.getEnabled());
        if ("ACTIVE".equalsIgnoreCase(status)) {
            return enabled;
        }
        if ("DISABLED".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status)) {
            return !enabled;
        }
        return status.equalsIgnoreCase(nullToEmpty(pm.getStatus()));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}