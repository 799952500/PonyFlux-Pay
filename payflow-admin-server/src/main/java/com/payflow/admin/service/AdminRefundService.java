package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.client.CashierInternalRefundClient;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.entity.cashier.Refund;
import com.payflow.admin.mapper.cashier.OrderMapper;
import com.payflow.admin.mapper.cashier.RefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端退款查询与审批（通过后调用收银台执行渠道退款）。
 *
 * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private final RefundMapper refundMapper;
    private final OrderMapper orderMapper;
    private final CashierInternalRefundClient cashierInternalRefundClient;

    /**
     * 分页查询退款列表（可选关键词、状态、日期）。
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param status     前端状态筛选（PENDING/COMPLETED 等），空表示全部
     * @param keyword    退款单号或订单号模糊匹配
     * @param startDate  开始日期（含）
     * @param endDate    结束日期（含）
     * @return 分页数据（records 已转换为前端视图字段）
     */
    public IPage<Map<String, Object>> page(int page, int pageSize, String status, String keyword,
                                           LocalDate startDate, LocalDate endDate,
                                           List<String> merchantScopeIds) {
        LambdaQueryWrapper<Refund> w = new LambdaQueryWrapper<>();
        applyMerchantScope(w, merchantScopeIds);
        String dbStatus = toDbStatusFilter(status);
        if (dbStatus != null) {
            w.eq(Refund::getStatus, dbStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(q -> q.like(Refund::getRefundId, kw).or().like(Refund::getOrderId, kw));
        }
        if (startDate != null) {
            w.ge(Refund::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            w.le(Refund::getCreatedAt, endDate.atTime(LocalTime.MAX));
        }
        w.orderByDesc(Refund::getCreatedAt);

        Page<Refund> p = new Page<>(page, pageSize);
        IPage<Refund> raw = refundMapper.selectPage(p, w);

        Page<Map<String, Object>> viewPage = new Page<>(page, pageSize, raw.getTotal());
        viewPage.setRecords(raw.getRecords().stream().map(this::toViewRow).toList());
        return viewPage;
    }

    /**
     * 审批通过：调用收银台内部接口执行渠道退款并落库。
     *
     * @param refundId 退款单号
     */
    public void approve(String refundId, List<String> merchantScopeIds) {
        Refund r = requireRefund(refundId);
        assertRefundMerchantAllowed(r, merchantScopeIds);
        if (!Refund.STATUS_REFUNDING.equals(r.getStatus())) {
            throw new IllegalStateException("当前状态不允许审批通过: " + r.getStatus());
        }
        cashierInternalRefundClient.executeRefund(refundId);
    }

    private void assertRefundMerchantAllowed(Refund r, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return;
        }
        if (merchantScopeIds.isEmpty()) {
            throw new IllegalStateException("无权操作该退款");
        }
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, r.getOrderId()));
        if (o == null || o.getMerchantId() == null || !merchantScopeIds.contains(o.getMerchantId())) {
            throw new IllegalStateException("无权操作该退款");
        }
    }

    private void applyMerchantScope(LambdaQueryWrapper<Refund> w, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return;
        }
        if (merchantScopeIds.isEmpty()) {
            w.apply("1 = 0");
            return;
        }
        // 仅允许安全字符的商户 ID（字母数字下划线连字符），防止 SQL 注入
        String inList = merchantScopeIds.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty() && s.matches("[A-Za-z0-9_-]+"))
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(","));
        if (inList.isEmpty()) {
            return;
        }
        w.apply("order_id IN (SELECT order_id FROM cashier_orders WHERE merchant_id IN (" + inList + "))");
    }

    /**
     * 审批拒绝：将退款中记录标记为失败。
     *
     * @param refundId 退款单号
     */
    @Transactional(transactionManager = "cashierTransactionManager")
    public void reject(String refundId, List<String> merchantScopeIds) {
        Refund r = requireRefund(refundId);
        assertRefundMerchantAllowed(r, merchantScopeIds);
        if (!Refund.STATUS_REFUNDING.equals(r.getStatus())) {
            throw new IllegalStateException("当前状态不允许拒绝: " + r.getStatus());
        }
        r.setStatus(Refund.STATUS_FAILED);
        refundMapper.updateById(r);
    }

    private Refund requireRefund(String refundId) {
        Refund r = refundMapper.selectOne(new LambdaQueryWrapper<Refund>().eq(Refund::getRefundId, refundId));
        if (r == null) {
            throw new IllegalArgumentException("退款记录不存在: " + refundId);
        }
        return r;
    }

    private Map<String, Object> toViewRow(Refund r) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, r.getOrderId()));
        Map<String, Object> m = new HashMap<>();
        m.put("refundId", r.getRefundId());
        m.put("orderId", r.getOrderId());
        m.put("merchantOrderNo", order != null ? order.getMerchantOrderNo() : "");
        m.put("amount", r.getRefundAmount());
        m.put("reason", r.getReason() != null ? r.getReason() : "");
        m.put("status", toUiStatus(r.getStatus()));
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }

    /**
     * 数据库状态转前端展示状态。
     */
    private String toUiStatus(String db) {
        if (Refund.STATUS_REFUNDING.equals(db)) {
            return "PENDING";
        }
        if (Refund.STATUS_REFUNDED.equals(db)) {
            return "COMPLETED";
        }
        if (Refund.STATUS_FAILED.equals(db) || Refund.STATUS_CLOSED.equals(db)) {
            return "REJECTED";
        }
        return "PENDING";
    }

    /**
     * 前端筛选状态转数据库单状态等值查询（简化实现）。
     */
    private String toDbStatusFilter(String uiStatus) {
        if (uiStatus == null || uiStatus.isBlank()) {
            return null;
        }
        return switch (uiStatus) {
            case "PENDING", "APPROVED" -> Refund.STATUS_REFUNDING;
            case "COMPLETED" -> Refund.STATUS_REFUNDED;
            case "REJECTED" -> Refund.STATUS_FAILED;
            default -> null;
        };
    }
}
