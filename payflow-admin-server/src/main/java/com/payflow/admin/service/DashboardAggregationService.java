package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.mapper.cashier.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘指标聚合（基于 cashier_orders）。
 *
 * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class DashboardAggregationService {

    private final OrderMapper orderMapper;

    /**
     * 构建前端仪表盘所需 data 结构（与 {@code dashboard.vue} 对齐）。
     */
    public Map<String, Object> buildDashboardPayload(int trendDays) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        if (trendDays < 1) {
            trendDays = 7;
        }
        if (trendDays > 90) {
            trendDays = 90;
        }
        LocalDate trendStart = today.minusDays(trendDays - 1L);

        long todayRevenueFen = nz(orderMapper.sumPaidRevenueFenOnDay(today));
        long yesterdayRevenueFen = nz(orderMapper.sumPaidRevenueFenOnDay(yesterday));
        long todayOrders = nz(orderMapper.countCreatedOnDay(today));
        long yesterdayOrders = nz(orderMapper.countCreatedOnDay(yesterday));
        long todayPaid = nz(orderMapper.countPaidOnDay(today));

        double conversionRate = todayOrders == 0 ? 0D
                : BigDecimal.valueOf(todayPaid * 100.0 / todayOrders).setScale(1, RoundingMode.HALF_UP).doubleValue();

        List<Map<String, Object>> trendRows = orderMapper.trendBuckets(trendStart);
        List<Map<String, Object>> trendData = new ArrayList<>();
        for (Map<String, Object> row : trendRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Object dayBucket = row.get("dayBucket");
            item.put("date", dayBucket != null ? dayBucket.toString() : "");
            item.put("orders", toLong(row.get("orders")));
            item.put("revenue", fenToYuanNumber(toLong(row.get("revenue"))));
            item.put("paid", toLong(row.get("paid")));
            trendData.add(item);
        }

        List<Map<String, Object>> distRows = orderMapper.channelDistributionLast30Days();
        long totalCnt = distRows.stream().mapToLong(r -> toLong(r.get("cnt"))).sum();
        List<Map<String, Object>> channelDistribution = new ArrayList<>();
        for (Map<String, Object> row : distRows) {
            Map<String, Object> item = new LinkedHashMap<>();
            String key = row.get("channelKey") != null ? row.get("channelKey").toString() : "UNKNOWN";
            item.put("channel", key);
            item.put("name", channelDisplayName(key));
            long cnt = toLong(row.get("cnt"));
            item.put("value", cnt);
            item.put("amount", fenToYuanNumber(toLong(row.get("amt"))));
            item.put("ratio", totalCnt <= 0 ? "0%"
                    : BigDecimal.valueOf(cnt * 100.0 / totalCnt).setScale(1, RoundingMode.HALF_UP) + "%");
            channelDistribution.add(item);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayRevenue", fenToYuanNumber(todayRevenueFen));
        data.put("yesterdayRevenue", fenToYuanNumber(yesterdayRevenueFen));
        data.put("todayOrders", todayOrders);
        data.put("yesterdayOrders", yesterdayOrders);
        data.put("todayPaid", todayPaid);
        data.put("conversionRate", conversionRate);
        data.put("trendData", trendData);
        data.put("channelDistribution", channelDistribution);
        data.put("recentOrders", buildRecentOrders());

        data.put("todayRefunds", 0);
        data.put("activeMerchants", 0);
        data.put("successRate",
                BigDecimal.valueOf(conversionRate).setScale(1, RoundingMode.HALF_UP) + "%");

        return data;
    }

    /**
     * 渠道编码 → 饼图/图例展示名（与前端字典一致，避免图例挤英文代码）。
     */
    private static String channelDisplayName(String channelKey) {
        if (channelKey == null || channelKey.isBlank()) {
            return "未知";
        }
        return switch (channelKey.trim()) {
            case "WECHAT_PAY" -> "微信支付";
            case "ALIPAY" -> "支付宝";
            case "UNION_PAY" -> "银联";
            case "UNKNOWN" -> "其他";
            default -> channelKey;
        };
    }

    private List<Map<String, Object>> buildRecentOrders() {
        List<Order> list = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .orderByDesc(Order::getCreatedAt)
                        .last("LIMIT 10"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Order o : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderId", o.getOrderId());
            m.put("merchantId", o.getMerchantId());
            m.put("merchantOrderNo", o.getMerchantOrderNo() != null ? o.getMerchantOrderNo() : "");
            m.put("subject", o.getSubject() != null ? o.getSubject() : "");
            m.put("amount", o.getAmount() != null ? o.getAmount() : 0L);
            m.put("currency", o.getCurrency() != null ? o.getCurrency() : "CNY");
            m.put("channel", o.getChannel() != null ? o.getChannel() : "");
            m.put("status", o.getStatus() != null ? o.getStatus() : "");
            m.put("expireTime", o.getExpireTime() != null ? o.getExpireTime().toString() : "");
            m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString().replace('T', ' ') : "");
            m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString().replace('T', ' ') : "");
            out.add(m);
        }
        return out;
    }

    private static long nz(Long v) {
        return v != null ? v : 0L;
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static double fenToYuanNumber(long fen) {
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }
}
