package com.payflow.recon.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.entity.ReconDiff;
import com.payflow.recon.kit.ReconChannelKit;
import com.payflow.recon.mapper.ReconBillRecordMapper;
import com.payflow.recon.mapper.ReconDiffMapper;
import com.payflow.recon.mapper.cashier.CashierReconPaymentMapper;
import com.payflow.recon.mapper.cashier.CashierReconPaymentRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 三方账单与本地支付明细比对。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconCompareService {

    public static final String DIFF_CHANNEL_ONLY = "CHANNEL_ONLY";
    public static final String DIFF_LOCAL_ONLY = "LOCAL_ONLY";
    public static final String DIFF_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String DIFF_STATUS_MISMATCH = "STATUS_MISMATCH";

    private final ReconBillRecordMapper reconBillRecordMapper;
    private final ReconDiffMapper reconDiffMapper;
    private final CashierReconPaymentMapper cashierReconPaymentMapper;

    /**
     * 执行比对并写入差异表（先删本任务旧差异）。
     */
    @Transactional(transactionManager = "adminTransactionManager")
    public int compareAndPersist(String taskId, String reconChannel, LocalDate billDate) {
        reconDiffMapper.delete(Wrappers.<ReconDiff>lambdaQuery().eq(ReconDiff::getTaskId, taskId));

        List<ReconBillRecord> bills = reconBillRecordMapper.selectList(
                Wrappers.<ReconBillRecord>lambdaQuery()
                        .eq(ReconBillRecord::getTaskId, taskId)
                        .eq(ReconBillRecord::getParseError, false));

        Map<String, ReconBillRecord> billByTrade = new HashMap<>();
        for (ReconBillRecord b : bills) {
            if (b.getChannelTradeNo() == null || b.getChannelTradeNo().isBlank()) {
                continue;
            }
            billByTrade.putIfAbsent(b.getChannelTradeNo(), b);
        }

        String payChannel = ReconChannelKit.reconToPayChannel(reconChannel);
        List<CashierReconPaymentRow> locals = cashierReconPaymentMapper.listSuccessByBillDate(payChannel, billDate);
        Map<String, CashierReconPaymentRow> localByTxn = new HashMap<>();
        for (CashierReconPaymentRow p : locals) {
            if (p.getChannelTransactionId() == null || p.getChannelTransactionId().isBlank()) {
                continue;
            }
            localByTxn.putIfAbsent(p.getChannelTransactionId(), p);
        }

        int diffCount = 0;

        for (Map.Entry<String, ReconBillRecord> e : billByTrade.entrySet()) {
            String txn = e.getKey();
            ReconBillRecord bill = e.getValue();
            CashierReconPaymentRow pay = localByTxn.get(txn);
            if (pay == null) {
                reconDiffMapper.insert(ReconDiff.builder()
                        .taskId(taskId)
                        .diffType(DIFF_CHANNEL_ONLY)
                        .channelTradeNo(txn)
                        .localOrderId(null)
                        .channelAmount(bill.getAmountFen())
                        .localAmount(null)
                        .channelStatus(bill.getChannelStatus())
                        .localStatus(null)
                        .handleStatus("PENDING")
                        .build());
                diffCount++;
                continue;
            }
            Long chAmt = bill.getAmountFen();
            Long locAmt = pay.getAmount();
            if (chAmt != null && locAmt != null && !chAmt.equals(locAmt)) {
                reconDiffMapper.insert(ReconDiff.builder()
                        .taskId(taskId)
                        .diffType(DIFF_AMOUNT_MISMATCH)
                        .channelTradeNo(txn)
                        .localOrderId(pay.getOrderId())
                        .channelAmount(chAmt)
                        .localAmount(locAmt)
                        .channelStatus(bill.getChannelStatus())
                        .localStatus(pay.getStatus())
                        .handleStatus("PENDING")
                        .build());
                diffCount++;
                continue;
            }
            if (statusMismatch(bill.getChannelStatus(), pay.getStatus())) {
                reconDiffMapper.insert(ReconDiff.builder()
                        .taskId(taskId)
                        .diffType(DIFF_STATUS_MISMATCH)
                        .channelTradeNo(txn)
                        .localOrderId(pay.getOrderId())
                        .channelAmount(chAmt)
                        .localAmount(locAmt)
                        .channelStatus(bill.getChannelStatus())
                        .localStatus(pay.getStatus())
                        .handleStatus("PENDING")
                        .build());
                diffCount++;
            }
        }

        for (Map.Entry<String, CashierReconPaymentRow> e : localByTxn.entrySet()) {
            String txn = e.getKey();
            if (billByTrade.containsKey(txn)) {
                continue;
            }
            CashierReconPaymentRow pay = e.getValue();
            reconDiffMapper.insert(ReconDiff.builder()
                    .taskId(taskId)
                    .diffType(DIFF_LOCAL_ONLY)
                    .channelTradeNo(txn)
                    .localOrderId(pay.getOrderId())
                    .channelAmount(null)
                    .localAmount(pay.getAmount())
                    .channelStatus(null)
                    .localStatus(pay.getStatus())
                    .handleStatus("PENDING")
                    .build());
            diffCount++;
        }

        log.info("对账比对完成: taskId={}, diffCount={}", taskId, diffCount);
        return diffCount;
    }

    private static boolean statusMismatch(String channelStatus, String localStatus) {
        if (channelStatus == null || localStatus == null) {
            return false;
        }
        boolean billOk = channelStatus.contains("成功") || "SUCCESS".equalsIgnoreCase(channelStatus);
        boolean localOk = "SUCCESS".equalsIgnoreCase(localStatus) || "PAID".equalsIgnoreCase(localStatus);
        return billOk != localOk;
    }
}
