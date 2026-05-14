package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.DashboardMetrics;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.mapper.DashboardMetricsMapper;
import com.payflow.admin.mapper.cashier.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘指标聚合（基于 cashier_orders + admin_dashboard_metrics 预聚合表）。
 *
 * @author Lucas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardAggregationService {

    private final OrderMapper orderMapper;
    private final DashboardMetricsMapper dashboardMetricsMapper;

    // ==================== 预聚合写入 ====================

    /**
     * 从 cashier_payments/cashier_refunds 聚合数据，写入 admin_dashboard_metrics。
     *
     * @param granularity 粒度: 5min / hour / day
     * @param startTime   聚合窗口起始
     * @param endTime     聚合窗口结束
     */
    public int aggregateMetrics(String granularity, LocalDateTime startTime, LocalDateTime endTime) {
        String timeFormat = switch (granularity) {
            // truncate to 5min — handled in Java
            case "5min" -> "%Y-%m-%d %H:%i:00";
            case "hour" -> "%Y-%m-%d %H:00:00";
            case "day" -> "%Y-%m-%d 00:00:00";
            default -> "%Y-%m-%d %H:00:00";
        };

        List<Map<String, Object>> paymentRows = orderMapper.aggregatePayments(startTime, endTime, timeFormat);
        List<Map<String, Object>> refundRows = orderMapper.aggregateRefunds(startTime, endTime, timeFormat);

        // 合并 payment 和 refund 数据
        Map<String, DashboardMetrics> metricsMap = new HashMap<>();

        for (Map<String, Object> row : paymentRows) {
            String key = buildKey(row.get("timeBucket"), row.get("channelCode"));
            DashboardMetrics m = metricsMap.computeIfAbsent(key, k -> {
                DashboardMetrics dm = new DashboardMetrics();
                dm.setMetricTime(parseDateTime(row.get("timeBucket")));
                dm.setGranularity(granularity);
                dm.setChannelCode(str(row.get("channelCode")));
                return dm;
            });
            m.setTotalAmount(toLong(row.get("totalAmount")));
            m.setTotalCount(toInt(row.get("totalCount")));
            m.setActiveMerchants(toInt(row.get("activeMerchants")));
        }

        for (Map<String, Object> row : refundRows) {
            String key = buildKey(row.get("timeBucket"), row.get("channelCode"));
            DashboardMetrics m = metricsMap.get(key);
            if (m != null) {
                m.setRefundAmount(toLong(row.get("refundAmount")));
                m.setRefundCount(toInt(row.get("refundCount")));
            }
        }

        // 批量插入
        int count = 0;
        for (DashboardMetrics m : metricsMap.values()) {
            if (m.getTotalAmount() == null) {
                m.setTotalAmount(0L);
            }
            if (m.getTotalCount() == null) {
                m.setTotalCount(0);
            }
            if (m.getActiveMerchants() == null) {
                m.setActiveMerchants(0);
            }
            if (m.getFeeIncome() == null) {
                m.setFeeIncome(0L);
            }
            if (m.getRefundAmount() == null) {
                m.setRefundAmount(0L);
            }
            if (m.getRefundCount() == null) {
                m.setRefundCount(0);
            }
            dashboardMetricsMapper.insert(m);
            count++;
        }
        log.info("聚合完成: granularity={}, window=[{} ~ {}], 写入{}条", granularity, startTime, endTime, count);
        return count;
    }

    // ==================== 预聚合表读取 ====================

    /**
     * 从预聚合表读取仪表盘核心指标。
     */
    public Map<String, Object> queryMetrics(LocalDateTime start, LocalDateTime end, String granularity, String channelCode) {
        LambdaQueryWrapper<DashboardMetrics> wrapper = new LambdaQueryWrapper<DashboardMetrics>()
                .eq(DashboardMetrics::getGranularity, granularity)
                .ge(DashboardMetrics::getMetricTime, start)
                .le(DashboardMetrics::getMetricTime, end);
        if (channelCode != null && !channelCode.isEmpty() && !"ALL".equals(channelCode)) {
            wrapper.eq(DashboardMetrics::getChannelCode, channelCode);
        } else {
            wrapper.eq(DashboardMetrics::getChannelCode, "ALL");
        }
        wrapper.orderByAsc(DashboardMetrics::getMetricTime);
        List<DashboardMetrics> list = dashboardMetricsMapper.selectList(wrapper);

        long totalAmount = list.stream().mapToLong(DashboardMetrics::getTotalAmount).sum();
        long totalCount = list.stream().mapToLong(DashboardMetrics::getTotalCount).sum();
        long refundAmount = list.stream().mapToLong(DashboardMetrics::getRefundAmount).sum();
        long refundCount = list.stream().mapToLong(DashboardMetrics::getRefundCount).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("totalCount", totalCount);
        result.put("activeMerchants", list.isEmpty() ? 0 : list.get(list.size() - 1).getActiveMerchants());
        result.put("refundAmount", refundAmount);
        result.put("refundCount", refundCount);
        result.put("trendData", list);
        return result;
    }

    /**
     * 获取商户交易排行（从流水表实时查询 Top N）
     */
    public List<Map<String, Object>> getMerchantRanking(LocalDateTime start, LocalDateTime end, int limit) {
        return orderMapper.merchantRanking(start, end, limit);
    }

    // ==================== 现有仪表盘（兼容旧逻辑） ====================

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

        // 商户排行 Top 10（近 30 天）
        LocalDateTime rankStart = today.minusDays(30).atStartOfDay();
        LocalDateTime rankEnd = today.plusDays(1).atStartOfDay();
        List<Map<String, Object>> ranking = orderMapper.merchantRanking(rankStart, rankEnd, 10);
        List<Map<String, Object>> merchantRanking = ranking.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("merchantId", str(r.get("merchantId")));
            item.put("totalAmount", toLong(r.get("totalAmount")));
            item.put("totalCount", toLong(r.get("totalCount")));
            item.put("totalAmountYuan", fenToYuanNumber(toLong(r.get("totalAmount"))));
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayRevenue", fenToYuanNumber(todayRevenueFen));
        data.put("yesterdayRevenue", fenToYuanNumber(yesterdayRevenueFen));
        data.put("todayOrders", todayOrders);
        data.put("yesterdayOrders", yesterdayOrders);
        data.put("todayPaid", todayPaid);
        data.put("conversionRate", conversionRate);
        data.put("trendData", trendData);
        data.put("channelDistribution", channelDistribution);
        data.put("merchantRanking", merchantRanking);
        data.put("recentOrders", buildRecentOrders());

        // 计算环比变化率
        long revenueChangePct = yesterdayRevenueFen == 0 ? 0
                : (todayRevenueFen - yesterdayRevenueFen) * 100 / yesterdayRevenueFen;

        // 计算同比变化率（与7天前对比）
        LocalDate sevenDaysAgo = today.minusDays(7);
        long sevenDaysAgoRevenue = nz(orderMapper.sumPaidRevenueFenOnDay(sevenDaysAgo));
        long revenueYoyPct = sevenDaysAgoRevenue == 0 ? 0
                : (todayRevenueFen - sevenDaysAgoRevenue) * 100 / sevenDaysAgoRevenue;

        data.put("revenueChangePct", revenueChangePct);
        data.put("revenueYoYPct", revenueYoyPct);
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

    // ==================== 工具方法 ====================

    private static String buildKey(Object timeBucket, Object channelCode) {
        return (timeBucket != null ? timeBucket.toString() : "") + "|" + (channelCode != null ? channelCode.toString() : "ALL");
    }

    private static LocalDateTime parseDateTime(Object obj) {
        if (obj == null) {
            return LocalDateTime.now();
        }
        String s = obj.toString();
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
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

    private static int toInt(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private static double fenToYuanNumber(long fen) {
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }
}
