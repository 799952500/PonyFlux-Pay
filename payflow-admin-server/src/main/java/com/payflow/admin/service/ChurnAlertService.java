package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.ChurnAlert;
import com.payflow.admin.mapper.ChurnAlertMapper;
import com.payflow.admin.mapper.cashier.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商户流失预警服务。
 * 算法：最近7天日均交易笔数 对比 前7天（第8-14天前）日均。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChurnAlertService {

    private final ChurnAlertMapper churnAlertMapper;
    private final OrderMapper orderMapper;

    /**
     * 执行流失检测（每日凌晨调用）。
     * 扫描近30天有交易的活跃商户，计算两个7天窗口的日均笔数，下降超阈值则生成预警。
     */
    public int detectChurn() {
        LocalDate today = LocalDate.now();
        LocalDate currentStart = today.minusDays(7);
        LocalDate baselineStart = today.minusDays(14);
        LocalDate baselineEnd = today.minusDays(7);

        List<Map<String, Object>> currentStats = orderMapper.merchantOrderCountsInRange(currentStart, today);
        List<Map<String, Object>> baselineStats = orderMapper.merchantOrderCountsInRange(baselineStart, baselineEnd);

        // 构建 baseline map
        Map<String, Double> baselineMap = new java.util.HashMap<>();
        for (Map<String, Object> row : baselineStats) {
            String merchantId = str(row.get("merchantId"));
            double avg = toDouble(row.get("dailyAvg"));
            baselineMap.put(merchantId, avg);
        }

        int alertCount = 0;
        for (Map<String, Object> row : currentStats) {
            String merchantId = str(row.get("merchantId"));
            double currentAvg = toDouble(row.get("dailyAvg"));
            long consecutiveDays = toLong(row.get("consecutiveDays"));
            double baselineAvg = baselineMap.getOrDefault(merchantId, 0.0);

            if (baselineAvg <= 0 || currentAvg >= baselineAvg) {
                continue; // 无基线或未下降
            }

            double declinePct = (baselineAvg - currentAvg) / baselineAvg * 100.0;
            String alertLevel;
            if (declinePct > 90) {
                alertLevel = "red";
            } else if (declinePct > 70) {
                alertLevel = "orange";
            } else if (declinePct > 50) {
                alertLevel = "yellow";
            } else {
                continue; // 未达阈值
            }

            // 检查是否已有未处理的预警
            long existingCount = churnAlertMapper.selectCount(
                    new LambdaQueryWrapper<ChurnAlert>()
                            .eq(ChurnAlert::getMerchantId, Long.parseLong(merchantId))
                            .eq(ChurnAlert::getStatus, "pending")
            );
            if (existingCount > 0) {
                continue;
            }

            ChurnAlert alert = new ChurnAlert();
            alert.setMerchantId(Long.parseLong(merchantId));
            alert.setAlertLevel(alertLevel);
            alert.setCurrentAvgCount(BigDecimal.valueOf(currentAvg));
            alert.setBaselineAvgCount(BigDecimal.valueOf(baselineAvg));
            alert.setDeclinePct(BigDecimal.valueOf(declinePct).setScale(2, RoundingMode.HALF_UP));
            alert.setConsecutiveDays((int) consecutiveDays);
            alert.setStatus("pending");
            churnAlertMapper.insert(alert);
            alertCount++;
            log.info("流失预警: merchantId={}, level={}, declinePct={}%, currentAvg={}, baselineAvg={}",
                    merchantId, alertLevel, declinePct, currentAvg, baselineAvg);
        }
        log.info("流失检测完成: 生成{}条预警", alertCount);
        return alertCount;
    }

    /**
     * 查询预警列表（分页）
     */
    public IPage<ChurnAlert> getAlerts(int pageNum, int pageSize, String merchantId, String status) {
        LambdaQueryWrapper<ChurnAlert> wrapper = new LambdaQueryWrapper<>();
        if (merchantId != null && !merchantId.isEmpty()) {
            wrapper.eq(ChurnAlert::getMerchantId, Long.parseLong(merchantId));
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ChurnAlert::getStatus, status);
        }
        wrapper.orderByDesc(ChurnAlert::getDeclinePct);
        return churnAlertMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 获取预警详情
     */
    public ChurnAlert getAlertDetail(Long id) {
        return churnAlertMapper.selectById(id);
    }

    /**
     * 更新预警状态
     */
    public boolean updateAlertStatus(Long id, String status, String note, String assignee) {
        ChurnAlert alert = churnAlertMapper.selectById(id);
        if (alert == null) {
            return false;
        }
        alert.setStatus(status);
        if (note != null) {
            alert.setNote(note);
        }
        if (assignee != null) {
            alert.setAssignee(assignee);
        }
        if ("resolved".equals(status) || "false_alarm".equals(status)) {
            alert.setResolvedTime(LocalDateTime.now());
        }
        return churnAlertMapper.updateById(alert) > 0;
    }

    /**
     * 统计超过指定小时数未处理的 pending 预警数。
     * 用于触发运营主管告警通知。
     */
    public int countOverdueAlerts(int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        Long count = churnAlertMapper.selectCount(
                new LambdaQueryWrapper<ChurnAlert>()
                        .eq(ChurnAlert::getStatus, "pending")
                        .lt(ChurnAlert::getCreateTime, cutoff)
        );
        return count.intValue();
    }

    // ==================== 工具方法 ====================

    private static String str(Object o) { return o != null ? o.toString() : ""; }

    private static double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try { return Double.parseDouble(o.toString()); }
        catch (NumberFormatException e) { return 0; }
    }

    private static long toLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) {
            return n.longValue();
        }
        try { return Long.parseLong(o.toString()); }
        catch (NumberFormatException e) { return 0; }
    }
}
