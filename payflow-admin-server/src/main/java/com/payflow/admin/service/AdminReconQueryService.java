package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.dto.recon.ReconAbnormalPageRow;
import com.payflow.admin.dto.recon.ReconAccountSummaryVO;
import com.payflow.admin.dto.recon.ReconDiffWorkItemDTO;
import com.payflow.admin.dto.recon.ReconOrderResultVO;
import com.payflow.admin.dto.recon.ReconSummaryResponse;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconHandlerAuditEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.kit.AdminReconChannelKit;
import com.payflow.admin.mapper.cashier.OrderMerchantRow;
import com.payflow.admin.mapper.cashier.ReconCashierPaymentRow;
import com.payflow.admin.mapper.cashier.ReconCashierReportMapper;
import com.payflow.admin.mapper.cashier.ReconLocalAccountAggRow;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconHandlerAuditEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.payflow.admin.service.recon.ReconLongTailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对账报表：订单结果、汇总、异常明细。
 *
 * @author PayFlow Team
 */
@Service
@RequiredArgsConstructor
public class AdminReconQueryService {

    private final ReconTaskEntityMapper reconTaskEntityMapper;
    private final ReconDiffEntityMapper reconDiffEntityMapper;
    private final ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper;
    private final ReconHandlerAuditEntityMapper reconHandlerAuditEntityMapper;
    private final ReconCashierReportMapper reconCashierReportMapper;

    /**
     * 分页查询对账订单结果。
     */
    public Map<String, Object> pageOrderResults(
            LocalDate billDate,
            String channel,
            String merchantId,
            String orderKeyword,
            boolean onlyAbnormal,
            long page,
            long size) {
        return pageOrderResults(billDate, channel, merchantId, orderKeyword, onlyAbnormal, page, size, null);
    }

    public Map<String, Object> pageOrderResults(
            LocalDate billDate,
            String channel,
            String merchantId,
            String orderKeyword,
            boolean onlyAbnormal,
            long page,
            long size,
            List<String> merchantScopeIds) {
        String scopedMerchantId = resolveMerchantFilter(merchantId, merchantScopeIds);
        if ("__NO_ACCESS__".equals(scopedMerchantId)) {
            return pagePayload(List.of(), 0, page, size);
        }
        String channelNorm = StringUtils.hasText(channel) ? channel.trim().toLowerCase() : null;
        if (onlyAbnormal) {
            return pageAbnormalOnly(billDate, channelNorm, page, size, merchantScopeIds);
        }
        return pageAllPayments(billDate, channelNorm, scopedMerchantId, orderKeyword, page, size);
    }

    private Map<String, Object> pageAllPayments(
            LocalDate billDate,
            String channelNorm,
            String merchantId,
            String orderKeyword,
            long page,
            long size) {
        String payChannel = AdminReconChannelKit.reconToPayChannel(channelNorm);

        List<ReconTaskEntity> successTasks = reconTaskEntityMapper.selectList(
                Wrappers.<ReconTaskEntity>lambdaQuery()
                        .eq(ReconTaskEntity::getBillDate, billDate)
                        .eq(ReconTaskEntity::getStatus, "SUCCESS")
                        .eq(StringUtils.hasText(channelNorm), ReconTaskEntity::getChannel, channelNorm));
        List<String> taskIds = successTasks.stream().map(ReconTaskEntity::getTaskId).toList();
        Map<String, ReconTaskEntity> taskById = successTasks.stream()
                .collect(Collectors.toMap(ReconTaskEntity::getTaskId, t -> t, (a, b) -> a));

        List<ReconDiffEntity> diffs = taskIds.isEmpty()
                ? List.of()
                : reconDiffEntityMapper.selectList(
                        Wrappers.<ReconDiffEntity>lambdaQuery().in(ReconDiffEntity::getTaskId, taskIds));
        Map<String, ReconDiffEntity> byTxn = new HashMap<>();
        Map<String, ReconDiffEntity> byOrder = new HashMap<>();
        for (ReconDiffEntity d : diffs) {
            if (StringUtils.hasText(d.getChannelTradeNo())) {
                byTxn.putIfAbsent(d.getChannelTradeNo(), d);
            }
            if (StringUtils.hasText(d.getLocalOrderId())) {
                byOrder.putIfAbsent(d.getLocalOrderId(), d);
            }
        }

        long offset = (page - 1) * size;
        var dayStart = billDate.atStartOfDay();
        var dayEnd = billDate.plusDays(1).atStartOfDay();
        long total = reconCashierReportMapper.countSuccessPaymentsOnBillDate(
                dayStart, dayEnd, payChannel, emptyToNull(merchantId), emptyToNull(orderKeyword));
        List<ReconCashierPaymentRow> rows = reconCashierReportMapper.listSuccessPaymentsOnBillDate(
                dayStart, dayEnd, payChannel, emptyToNull(merchantId), emptyToNull(orderKeyword), offset, size);

        List<ReconOrderResultVO> list = new ArrayList<>();
        for (ReconCashierPaymentRow p : rows) {
            list.add(buildVoFromPayment(p, taskIds.isEmpty(), byTxn, byOrder, taskById));
        }
        return pagePayload(list, total, page, size);
    }

    /**
     * 仅差异表分页（异常订单）；商户/订单筛选请用 {@link #pageAllPayments} 全量模式。
     */
    private Map<String, Object> pageAbnormalOnly(
            LocalDate billDate,
            String channelNorm,
            long page,
            long size) {
        return pageAbnormalOnly(billDate, channelNorm, page, size, null);
    }

    private Map<String, Object> pageAbnormalOnly(
            LocalDate billDate,
            String channelNorm,
            long page,
            long size,
            List<String> merchantScopeIds) {
        long offset = (page - 1) * size;
        long total = reconDiffEntityMapper.countAbnormalByBillDate(billDate, channelNorm, null, null);
        List<ReconAbnormalPageRow> abnormalRows = reconDiffEntityMapper.listAbnormalByBillDate(
                billDate, channelNorm, null, null, offset, size);

        Set<String> orderIds = abnormalRows.stream()
                .map(ReconAbnormalPageRow::getLocalOrderId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, String> merchantByOrder = lookupMerchantMap(orderIds);

        List<ReconOrderResultVO> list = new ArrayList<>();
        for (ReconAbnormalPageRow r : abnormalRows) {
            String mid = r.getLocalOrderId() != null ? merchantByOrder.get(r.getLocalOrderId()) : null;
            if (!isMerchantAllowed(mid, merchantScopeIds)) {
                continue;
            }
            list.add(ReconOrderResultVO.builder()
                    .orderId(r.getLocalOrderId())
                    .merchantId(mid)
                    .paymentId(null)
                    .payChannel(null)
                    .channelTransactionId(r.getChannelTradeNo())
                    .localAmountFen(r.getLocalAmount())
                    .reconStatus("ABNORMAL")
                    .diffType(r.getDiffType())
                    .handleStatus(r.getHandleStatus())
                    .diffId(r.getDiffId())
                    .taskId(r.getTaskId())
                    .reconChannel(r.getReconChannel())
                    .accountCode(r.getAccountCode())
                    .channelAmountFen(r.getChannelAmount())
                    .build());
        }
        return pagePayload(list, total, page, size);
    }

    /**
     * 汇总：按支付账号（收款账户）对比本地成功收款与渠道账单（SUCCESS 任务），及待处理差异笔数。
     */
    public ReconSummaryResponse buildSummary(LocalDate billDate, String channel, String accountCode) {
        return buildSummary(billDate, channel, accountCode, null);
    }

    public ReconSummaryResponse buildSummary(LocalDate billDate, String channel, String accountCode,
                                             List<String> merchantScopeIds) {
        String channelNorm = StringUtils.hasText(channel) ? channel.trim().toLowerCase() : null;
        String acctFilter = StringUtils.hasText(accountCode) ? accountCode.trim() : null;

        List<ReconLocalAccountAggRow> localRows = reconCashierReportMapper.aggregateLocalSuccessByAccount(
                billDate.atStartOfDay(), billDate.plusDays(1).atStartOfDay(), acctFilter);
        if (merchantScopeIds != null && merchantScopeIds.isEmpty()) {
            localRows = List.of();
        }
        List<ReconTaskEntity> tasks = reconTaskEntityMapper.selectList(
                Wrappers.<ReconTaskEntity>lambdaQuery()
                        .eq(ReconTaskEntity::getBillDate, billDate)
                        .eq(ReconTaskEntity::getStatus, "SUCCESS")
                        .eq(StringUtils.hasText(channelNorm), ReconTaskEntity::getChannel, channelNorm)
                        .eq(StringUtils.hasText(acctFilter), ReconTaskEntity::getAccountCode, acctFilter));

        Map<String, long[]> billAgg = new HashMap<>();
        Map<String, String> channelByAccount = new HashMap<>();
        for (ReconTaskEntity t : tasks) {
            String key = StringUtils.hasText(t.getAccountCode()) ? t.getAccountCode() : "__NO_ACCOUNT__";
            long[] agg = billAgg.computeIfAbsent(key, k -> new long[2]);
            agg[0] += t.getBillTotalCount() != null ? t.getBillTotalCount() : 0L;
            agg[1] += t.getBillTotalAmount() != null ? t.getBillTotalAmount() : 0L;
            channelByAccount.putIfAbsent(key, t.getChannel());
        }

        Map<String, Long> localCount = new HashMap<>();
        Map<String, Long> localAmt = new HashMap<>();
        Map<String, String> payChByAccount = new HashMap<>();
        for (ReconLocalAccountAggRow lr : localRows) {
            String key = StringUtils.hasText(lr.getAccountCode()) ? lr.getAccountCode() : "__NO_ACCOUNT__";
            localCount.merge(key, lr.getCnt() != null ? lr.getCnt() : 0L, Long::sum);
            localAmt.merge(key, lr.getSumAmount() != null ? lr.getSumAmount() : 0L, Long::sum);
            if (StringUtils.hasText(lr.getPayChannel())) {
                payChByAccount.putIfAbsent(key, lr.getPayChannel());
            }
        }

        Set<String> accountKeys = new HashSet<>();
        accountKeys.addAll(localCount.keySet());
        accountKeys.addAll(billAgg.keySet());

        List<ReconAccountSummaryVO> byAccount = new ArrayList<>();
        long totalLocal = 0;
        long totalBill = 0;
        for (String key : accountKeys.stream().sorted().toList()) {
            long lc = localCount.getOrDefault(key, 0L);
            long la = localAmt.getOrDefault(key, 0L);
            long[] ba = billAgg.getOrDefault(key, new long[2]);
            long bc = ba[0];
            long bf = ba[1];
            totalLocal += la;
            totalBill += bf;
            String ch = channelByAccount.get(key);
            if (!StringUtils.hasText(ch)) {
                ch = payChannelToRecon(payChByAccount.get(key));
            }
            byAccount.add(ReconAccountSummaryVO.builder()
                    .accountCode(key)
                    .channel(ch)
                    .localSuccessCount(lc)
                    .localSuccessAmountFen(la)
                    .channelBillCount(bc)
                    .channelBillAmountFen(bf)
                    .amountDeltaFen(la - bf)
                    .build());
        }

        List<ReconTaskEntity> tasksForDiff = reconTaskEntityMapper.selectList(
                Wrappers.<ReconTaskEntity>lambdaQuery()
                        .eq(ReconTaskEntity::getBillDate, billDate)
                        .eq(StringUtils.hasText(channelNorm), ReconTaskEntity::getChannel, channelNorm)
                        .eq(StringUtils.hasText(acctFilter), ReconTaskEntity::getAccountCode, acctFilter));
        List<String> allTaskIds = tasksForDiff.stream().map(ReconTaskEntity::getTaskId).toList();
        long pendingDiff = 0;
        if (!allTaskIds.isEmpty()) {
            pendingDiff = reconDiffEntityMapper.selectCount(
                    Wrappers.<ReconDiffEntity>lambdaQuery()
                            .in(ReconDiffEntity::getTaskId, allTaskIds)
                            .eq(ReconDiffEntity::getHandleStatus, "PENDING"));
        }

        return ReconSummaryResponse.builder()
                .byAccount(byAccount)
                .totalLocalAmountFen(totalLocal)
                .totalChannelBillAmountFen(totalBill)
                .totalAmountDeltaFen(totalLocal - totalBill)
                .pendingDiffCount(pendingDiff)
                .build();
    }

    /**
     * 异常订单明细分页（汇总页「详情」用）。
     */
    public Map<String, Object> pageAnomalies(
            LocalDate billDate, String channel, String accountCode, String handleStatus, long page, long size) {
        return pageAnomalies(billDate, channel, accountCode, handleStatus, page, size, null);
    }

    public Map<String, Object> pageAnomalies(
            LocalDate billDate, String channel, String accountCode, String handleStatus, long page, long size,
            List<String> merchantScopeIds) {
        String channelNorm = StringUtils.hasText(channel) ? channel.trim().toLowerCase() : null;
        String acct = StringUtils.hasText(accountCode) ? accountCode.trim() : null;
        String handle = StringUtils.hasText(handleStatus) ? handleStatus.trim().toUpperCase() : null;
        long offset = (page - 1) * size;
        long total = reconDiffEntityMapper.countAbnormalByBillDate(billDate, channelNorm, acct, handle);
        List<ReconAbnormalPageRow> rows = reconDiffEntityMapper.listAbnormalByBillDate(
                billDate, channelNorm, acct, handle, offset, size);
        Set<String> orderIds = rows.stream()
                .map(ReconAbnormalPageRow::getLocalOrderId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, String> merchantByOrder = lookupMerchantMap(orderIds);
        List<Map<String, Object>> list = new ArrayList<>();
        for (ReconAbnormalPageRow r : rows) {
            String mid = r.getLocalOrderId() != null ? merchantByOrder.get(r.getLocalOrderId()) : null;
            if (!isMerchantAllowed(mid, merchantScopeIds)) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("diffId", r.getDiffId());
            m.put("taskId", r.getTaskId());
            m.put("diffType", r.getDiffType());
            m.put("channelTradeNo", r.getChannelTradeNo());
            m.put("localOrderId", r.getLocalOrderId());
            m.put("merchantId", mid);
            m.put("channelAmount", r.getChannelAmount());
            m.put("localAmount", r.getLocalAmount());
            m.put("handleStatus", r.getHandleStatus());
            m.put("reconChannel", r.getReconChannel());
            m.put("accountCode", r.getAccountCode());
            m.put("billDate", r.getBillDate());
            list.add(m);
        }
        return pagePayload(list, total, page, size);
    }

    private ReconOrderResultVO buildVoFromPayment(
            ReconCashierPaymentRow p,
            boolean noReconTask,
            Map<String, ReconDiffEntity> byTxn,
            Map<String, ReconDiffEntity> byOrder,
            Map<String, ReconTaskEntity> taskById) {
        ReconDiffEntity d = null;
        if (StringUtils.hasText(p.getChannelTransactionId())) {
            d = byTxn.get(p.getChannelTransactionId());
        }
        if (d == null && StringUtils.hasText(p.getOrderId())) {
            d = byOrder.get(p.getOrderId());
        }
        if (noReconTask) {
            return ReconOrderResultVO.builder()
                    .orderId(p.getOrderId())
                    .merchantId(p.getMerchantId())
                    .paymentId(p.getPaymentId())
                    .payChannel(p.getPayChannel())
                    .accountCode(p.getAccountCode())
                    .channelTransactionId(p.getChannelTransactionId())
                    .localAmountFen(p.getAmount())
                    .reconStatus("NO_RECON")
                    .build();
        }
        if (d != null) {
            ReconTaskEntity t = taskById.get(d.getTaskId());
            String acct = StringUtils.hasText(p.getAccountCode())
                    ? p.getAccountCode()
                    : (t != null ? t.getAccountCode() : null);
            return ReconOrderResultVO.builder()
                    .orderId(p.getOrderId())
                    .merchantId(p.getMerchantId())
                    .paymentId(p.getPaymentId())
                    .payChannel(p.getPayChannel())
                    .channelTransactionId(p.getChannelTransactionId())
                    .localAmountFen(p.getAmount())
                    .reconStatus("ABNORMAL")
                    .diffType(d.getDiffType())
                    .handleStatus(d.getHandleStatus())
                    .diffId(d.getId())
                    .taskId(d.getTaskId())
                    .reconChannel(t != null ? t.getChannel() : null)
                    .accountCode(acct)
                    .channelAmountFen(d.getChannelAmount())
                    .build();
        }
        return ReconOrderResultVO.builder()
                .orderId(p.getOrderId())
                .merchantId(p.getMerchantId())
                .paymentId(p.getPaymentId())
                .payChannel(p.getPayChannel())
                .accountCode(p.getAccountCode())
                .channelTransactionId(p.getChannelTransactionId())
                .localAmountFen(p.getAmount())
                .reconStatus("MATCHED")
                .build();
    }

    private Map<String, String> lookupMerchantMap(Set<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<OrderMerchantRow> rows = reconCashierReportMapper.lookupMerchantsByOrderIds(new ArrayList<>(orderIds));
        return rows.stream()
                .filter(r -> r.getOrderId() != null)
                .collect(Collectors.toMap(OrderMerchantRow::getOrderId, OrderMerchantRow::getMerchantId, (a, b) -> a));
    }

    private static String payChannelToRecon(String payChannel) {
        if (payChannel == null) {
            return null;
        }
        String u = payChannel.toUpperCase();
        if ("ALIPAY".equals(u)) {
            return "alipay";
        }
        if ("WECHAT_PAY".equals(u)) {
            return "wxpay";
        }
        return null;
    }

    private static String emptyToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private static String resolveMerchantFilter(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return emptyToNull(merchantId);
        }
        if (merchantScopeIds.isEmpty()) {
            return "__NO_ACCESS__";
        }
        if (StringUtils.hasText(merchantId)) {
            String requested = merchantId.trim();
            return merchantScopeIds.contains(requested) ? requested : "__NO_ACCESS__";
        }
        return merchantScopeIds.size() == 1 ? merchantScopeIds.get(0) : null;
    }

    private static boolean isMerchantAllowed(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return true;
        }
        return StringUtils.hasText(merchantId) && merchantScopeIds.contains(merchantId);
    }

    private static Map<String, Object> pagePayload(List<?> list, long total, long page, long size) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);
        return data;
    }

    public Map<String, Object> pageWorkItems(
            LocalDate billDate,
            String channel,
            String diffType,
            String workflowStatus,
            String onlyMineAssignee,
            boolean onlyUnassigned,
            boolean onlyOverdue,
            String ageBucket,
            long page,
            long size,
            List<String> merchantScopeIds) {
        long safeSize = Math.min(size, 500);
        Page<ReconDiffAssignmentEntity> p = new Page<>(page, safeSize);

        var qw = Wrappers.<ReconDiffAssignmentEntity>lambdaQuery();
        if (merchantScopeIds != null) {
            if (merchantScopeIds.isEmpty()) {
                return pagePayload(List.of(), 0, page, safeSize);
            }
            qw.in(ReconDiffAssignmentEntity::getMerchantId, merchantScopeIds);
        }
        if (StringUtils.hasText(workflowStatus)) {
            qw.eq(ReconDiffAssignmentEntity::getWorkflowStatus, workflowStatus.trim().toUpperCase());
        }
        if (StringUtils.hasText(onlyMineAssignee)) {
            qw.eq(ReconDiffAssignmentEntity::getAssigneeId, onlyMineAssignee);
        }
        if (onlyUnassigned) {
            qw.eq(ReconDiffAssignmentEntity::getWorkflowStatus, "UNASSIGNED");
        }
        qw.orderByDesc(ReconDiffAssignmentEntity::getUpdatedAt);
        reconDiffAssignmentEntityMapper.selectPage(p, qw);

        List<Long> diffIds = p.getRecords().stream().map(ReconDiffAssignmentEntity::getDiffId).toList();
        Map<Long, ReconDiffEntity> diffById = diffIds.isEmpty()
                ? Map.of()
                : reconDiffEntityMapper.selectBatchIds(diffIds).stream()
                .collect(Collectors.toMap(ReconDiffEntity::getId, d -> d, (a, b) -> a));

        List<ReconDiffWorkItemDTO> list = new ArrayList<>();
        for (ReconDiffAssignmentEntity a : p.getRecords()) {
            ReconDiffEntity d = diffById.get(a.getDiffId());
            if (d == null) {
                continue;
            }
            if (billDate != null || StringUtils.hasText(channel)) {
                ReconTaskEntity t = reconTaskEntityMapper.selectOne(
                        Wrappers.<ReconTaskEntity>lambdaQuery().eq(ReconTaskEntity::getTaskId, d.getTaskId()));
                if (t == null) {
                    continue;
                }
                if (billDate != null && !billDate.equals(t.getBillDate())) {
                    continue;
                }
                if (StringUtils.hasText(channel) && !channel.trim().equalsIgnoreCase(t.getChannel())) {
                    continue;
                }
            }
            if (StringUtils.hasText(diffType) && !diffType.trim().equalsIgnoreCase(d.getDiffType())) {
                continue;
            }
            if (onlyOverdue && (a.getDueAt() == null || !a.getDueAt().isBefore(LocalDateTime.now()))) {
                continue;
            }
            if (StringUtils.hasText(ageBucket)) {
                int ageDays = (int) ChronoUnit.DAYS.between(a.getCreatedAt().toLocalDate(), LocalDate.now());
                if (!ageBucket.trim().equalsIgnoreCase(ReconLongTailService.ageBucket(ageDays))) {
                    continue;
                }
            }
            ReconDiffWorkItemDTO dto = new ReconDiffWorkItemDTO();
            dto.setDiffId(d.getId());
            dto.setTaskId(d.getTaskId());
            dto.setMerchantId(a.getMerchantId());
            dto.setDiffType(d.getDiffType());
            dto.setHandleStatus(d.getHandleStatus());
            dto.setWorkflowStatus(a.getWorkflowStatus());
            dto.setAssigneeId(a.getAssigneeId());
            dto.setDueAt(a.getDueAt());
            dto.setEscalatedAt(a.getEscalatedAt());
            dto.setCreatedAt(d.getCreatedAt());
            dto.setChannelAmount(d.getChannelAmount());
            dto.setLocalAmount(d.getLocalAmount());
            list.add(dto);
        }
        return pagePayload(list, p.getTotal(), page, safeSize);
    }

    public Map<String, Object> getWorkItemDetail(long diffId, List<String> merchantScopeIds) {
        ReconDiffEntity diff = reconDiffEntityMapper.selectById(diffId);
        if (diff == null) {
            throw new BizException(7543, "差异记录不存在: " + diffId);
        }
        ReconDiffAssignmentEntity assignment = reconDiffAssignmentEntityMapper.selectOne(
                Wrappers.<ReconDiffAssignmentEntity>lambdaQuery().eq(ReconDiffAssignmentEntity::getDiffId, diffId));
        if (assignment == null) {
            throw new BizException(7560, "工单不存在: " + diffId);
        }
        if (merchantScopeIds != null) {
            if (merchantScopeIds.isEmpty() || !merchantScopeIds.contains(assignment.getMerchantId())) {
                throw new BizException(7503, "授权商户范围不包含目标资源");
            }
        }
        List<ReconHandlerAuditEntity> audits = reconHandlerAuditEntityMapper.selectList(
                Wrappers.<ReconHandlerAuditEntity>lambdaQuery()
                        .eq(ReconHandlerAuditEntity::getDiffId, diffId)
                        .orderByAsc(ReconHandlerAuditEntity::getId));

        Map<String, Object> data = new HashMap<>();
        data.put("diff", diff);
        data.put("assignment", assignment);
        data.put("audits", audits);
        return data;
    }
}
