package com.payflow.recon.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.common.exception.BizException;
import com.payflow.recon.entity.ReconMerchantTask;
import com.payflow.recon.mapper.ReconMerchantTaskMapper;
import com.payflow.recon.mapper.cashier.CashierMerchantStatementPaymentRow;
import com.payflow.recon.mapper.cashier.CashierReconMerchantMapper;
import com.payflow.recon.storage.ReconFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商户对账子进程：根据 INIT 子任务汇总收银成功支付并生成对账单文件。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconMerchantStatementService {

    private static final String STATUS_GENERATING = "GENERATING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";

    private final ReconMerchantTaskMapper reconMerchantTaskMapper;
    private final CashierReconMerchantMapper cashierReconMerchantMapper;
    private final ReconFileStorage reconFileStorage;

    /**
     * 执行单条商户对账子任务。
     */
    public void execute(ReconMerchantTask task) {
        long t0 = System.currentTimeMillis();
        String merchantTaskId = task.getMerchantTaskId();
        try {
            updateTask(merchantTaskId, STATUS_GENERATING, null, null, null, null, null, null);

            List<CashierMerchantStatementPaymentRow> rows = cashierReconMerchantMapper
                    .listSuccessPaymentsForMerchantOnDate(task.getMerchantId(), task.getBillDate());
            int count = rows.size();
            long totalFen = rows.stream()
                    .mapToLong(r -> r.getAmount() != null ? r.getAmount() : 0L)
                    .sum();

            String csv = buildCsv(task.getMerchantId(), task.getBillDate(), count, totalFen, rows);
            Path temp = Files.createTempFile("mrc-statement-", ".csv");
            try {
                Files.writeString(temp, csv, StandardCharsets.UTF_8);
                String logicalName = "merchant_" + task.getMerchantId() + "_" + task.getBillDate() + ".csv";
                String key = reconFileStorage.put(temp, logicalName);
                long size = Files.size(Path.of(key));
                long elapsed = System.currentTimeMillis() - t0;
                updateTask(merchantTaskId, STATUS_SUCCESS, null, count, totalFen, key, size, elapsed);
                log.info("商户对账单生成成功: merchantTaskId={}, merchantId={}, payments={}",
                        merchantTaskId, task.getMerchantId(), count);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (BizException e) {
            markFail(merchantTaskId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("商户对账单生成失败: merchantTaskId={}", merchantTaskId, e);
            markFail(merchantTaskId, e.getMessage());
            throw new BizException(7600, "商户对账单生成失败: " + e.getMessage());
        }
    }

    private void updateTask(String merchantTaskId, String status, String err,
                            Integer paymentCount, Long paymentAmountFen,
                            String statementKey, Long statementSize, Long elapsedMs) {
        ReconMerchantTask t = reconMerchantTaskMapper.selectOne(
                Wrappers.<ReconMerchantTask>lambdaQuery()
                        .eq(ReconMerchantTask::getMerchantTaskId, merchantTaskId));
        if (t == null) {
            return;
        }
        t.setStatus(status);
        t.setErrorMsg(err);
        if (paymentCount != null) {
            t.setPaymentCount(paymentCount);
        }
        if (paymentAmountFen != null) {
            t.setPaymentAmountFen(paymentAmountFen);
        }
        if (statementKey != null) {
            t.setStatementObjectKey(statementKey);
        }
        if (statementSize != null) {
            t.setStatementSize(statementSize);
        }
        if (elapsedMs != null) {
            t.setElapsedMs(elapsedMs);
        }
        t.setUpdatedAt(LocalDateTime.now());
        reconMerchantTaskMapper.updateById(t);
    }

    private void markFail(String merchantTaskId, String msg) {
        ReconMerchantTask t = reconMerchantTaskMapper.selectOne(
                Wrappers.<ReconMerchantTask>lambdaQuery()
                        .eq(ReconMerchantTask::getMerchantTaskId, merchantTaskId));
        if (t != null) {
            t.setStatus(STATUS_FAIL);
            t.setErrorMsg(msg != null && msg.length() > 1000 ? msg.substring(0, 1000) : msg);
            t.setUpdatedAt(LocalDateTime.now());
            reconMerchantTaskMapper.updateById(t);
        }
    }

    private static String buildCsv(String merchantId, java.time.LocalDate billDate,
                                   int count, long totalFen,
                                   List<CashierMerchantStatementPaymentRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("merchant_id,bill_date,success_payment_count,success_amount_fen\n");
        sb.append(csvField(merchantId)).append(',')
                .append(billDate).append(',')
                .append(count).append(',')
                .append(totalFen).append('\n');
        sb.append("\n");
        sb.append("payment_id,order_id,pay_channel,channel_transaction_id,amount_fen,status,paid_at\n");
        for (CashierMerchantStatementPaymentRow r : rows) {
            sb.append(csvField(r.getPaymentId())).append(',')
                    .append(csvField(r.getOrderId())).append(',')
                    .append(csvField(r.getPayChannel())).append(',')
                    .append(csvField(r.getChannelTransactionId())).append(',')
                    .append(r.getAmount() != null ? r.getAmount() : "").append(',')
                    .append(csvField(r.getStatus())).append(',')
                    .append(r.getPaidAt() != null ? r.getPaidAt() : "")
                    .append('\n');
        }
        return sb.toString();
    }

    private static String csvField(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
