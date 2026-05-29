package com.payflow.admin.service;

import com.payflow.admin.dto.FunnelResult;
import com.payflow.admin.dto.FunnelStageDTO;
import com.payflow.admin.mapper.cashier.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 支付漏斗分析服务：从 cashier_orders 聚合各状态计数，计算漏斗转化率和流失分布。
 */
@Service
@RequiredArgsConstructor
public class FunnelService {

    private final OrderMapper orderMapper;

    public FunnelResult queryFunnel(LocalDate dateFrom, LocalDate dateTo,
                                    String merchantId, String channel,
                                    List<String> merchantScopeIds) {
        LocalDateTime start = dateFrom.atStartOfDay();
        LocalDateTime end = dateTo.atTime(LocalTime.MAX);

        List<Map<String, Object>> rows = orderMapper.funnelAggregate(start, end, merchantId, channel, merchantScopeIds);

        Map<String, Long> statusCounts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("status"));
            long cnt = ((Number) row.get("cnt")).longValue();
            statusCounts.put(status, cnt);
        }

        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        /* 漏斗阶段累计值 */
        long payingCount = getCount(statusCounts, "PAYING")
                + getCount(statusCounts, "PAID")
                + getCount(statusCounts, "SUCCESS");
        long paidCount = getCount(statusCounts, "PAID")
                + getCount(statusCounts, "SUCCESS");

        List<FunnelStageDTO> stages = new ArrayList<>();
        stages.add(new FunnelStageDTO("CREATED", total, null));
        stages.add(new FunnelStageDTO("PAYING", payingCount, rate(payingCount, total)));
        stages.add(new FunnelStageDTO("PAID", paidCount, rate(paidCount, payingCount)));

        /* 流失分布 */
        List<FunnelResult.LossItem> lossBreakdown = new ArrayList<>();
        addLossItem(lossBreakdown, "FAILED", statusCounts, total);
        addLossItem(lossBreakdown, "CLOSED", statusCounts, total);
        addLossItem(lossBreakdown, "EXPIRED", statusCounts, total);

        FunnelResult result = new FunnelResult();
        result.setDateFrom(dateFrom.toString());
        result.setDateTo(dateTo.toString());
        result.setStages(stages);
        result.setOverallConversionRate(rate(paidCount, total));
        result.setLossBreakdown(lossBreakdown);
        return result;
    }

    private static long getCount(Map<String, Long> map, String key) {
        return map.getOrDefault(key, 0L);
    }

    private static Double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static void addLossItem(List<FunnelResult.LossItem> list,
                                     String name, Map<String, Long> statusCounts, long total) {
        long cnt = getCount(statusCounts, name);
        list.add(new FunnelResult.LossItem(name, cnt, rate(cnt, total)));
    }
}
