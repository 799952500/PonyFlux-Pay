package com.payflow.recon.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.recon.entity.ReconMerchantTask;
import com.payflow.recon.entity.ReconTask;
import com.payflow.recon.kit.ReconChannelKit;
import com.payflow.recon.mapper.ReconMerchantTaskMapper;
import com.payflow.recon.mapper.ReconTaskMapper;
import com.payflow.recon.mapper.cashier.CashierReconAccountMapper;
import com.payflow.recon.mapper.cashier.CashierReconAccountRow;
import com.payflow.recon.mapper.cashier.CashierReconMerchantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 主对账调度：按支付账号、按商户生成子任务（INIT），由后台轮询器异步执行。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconTaskSeedService {

    private static final String STATUS_INIT = "INIT";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String BILL_TYPE_TRADE = "trade";

    private final CashierReconAccountMapper cashierReconAccountMapper;
    private final CashierReconMerchantMapper cashierReconMerchantMapper;
    private final ReconTaskMapper reconTaskMapper;
    private final ReconMerchantTaskMapper reconMerchantTaskMapper;

    /**
     * 为每个可对账支付账号生成或重置账户子对账任务。
     *
     * @return 写入或更新为 INIT 的任务数量
     */
    public int seedAccountTasks(LocalDate billDate, String triggeredBy, Long xxlLogId) {
        List<CashierReconAccountRow> rows = cashierReconAccountMapper.listEnabledForRecon();
        int n = 0;
        LocalDateTime now = LocalDateTime.now();
        for (CashierReconAccountRow row : rows) {
            String reconCh = ReconChannelKit.cashierChannelToRecon(row.getChannelCode());
            ReconTask existing = reconTaskMapper.selectOne(
                    Wrappers.<ReconTask>lambdaQuery()
                            .eq(ReconTask::getChannel, reconCh)
                            .eq(ReconTask::getAccountCode, row.getAccountCode())
                            .eq(ReconTask::getBillDate, billDate)
                            .eq(ReconTask::getBillType, BILL_TYPE_TRADE));
            if (existing != null && STATUS_SUCCESS.equals(existing.getStatus())) {
                continue;
            }
            if (existing != null) {
                existing.setStatus(STATUS_INIT);
                existing.setTriggeredBy(triggeredBy);
                existing.setXxlLogId(xxlLogId);
                existing.setErrorMsg(null);
                existing.setUpdatedAt(now);
                reconTaskMapper.updateById(existing);
            } else {
                ReconTask t = ReconTask.builder()
                        .taskId(ReconIdGenerator.newTaskId())
                        .channel(reconCh)
                        .accountCode(row.getAccountCode())
                        .billDate(billDate)
                        .billType(BILL_TYPE_TRADE)
                        .status(STATUS_INIT)
                        .diffCount(0)
                        .triggeredBy(triggeredBy)
                        .xxlLogId(xxlLogId)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                reconTaskMapper.insert(t);
            }
            n++;
        }
        log.info("主对账已生成账户子任务: billDate={}, count={}", billDate, n);
        return n;
    }

    /**
     * 为账单日有成功实收的商户生成或重置商户子对账任务。
     *
     * @return 写入或更新为 INIT 的任务数量
     */
    public int seedMerchantTasks(LocalDate billDate, String triggeredBy, Long xxlLogId) {
        List<String> merchantIds = cashierReconMerchantMapper.listMerchantIdsWithSuccessPaymentsOnDate(billDate);
        int n = 0;
        LocalDateTime now = LocalDateTime.now();
        for (String merchantId : merchantIds) {
            ReconMerchantTask existing = reconMerchantTaskMapper.selectOne(
                    Wrappers.<ReconMerchantTask>lambdaQuery()
                            .eq(ReconMerchantTask::getMerchantId, merchantId)
                            .eq(ReconMerchantTask::getBillDate, billDate));
            if (existing != null && STATUS_SUCCESS.equals(existing.getStatus())) {
                continue;
            }
            if (existing != null) {
                existing.setStatus(STATUS_INIT);
                existing.setTriggeredBy(triggeredBy);
                existing.setXxlLogId(xxlLogId);
                existing.setErrorMsg(null);
                existing.setStatementObjectKey(null);
                existing.setStatementSize(null);
                existing.setPaymentCount(null);
                existing.setPaymentAmountFen(null);
                existing.setElapsedMs(null);
                existing.setUpdatedAt(now);
                reconMerchantTaskMapper.updateById(existing);
            } else {
                ReconMerchantTask t = ReconMerchantTask.builder()
                        .merchantTaskId(ReconIdGenerator.newMerchantTaskId())
                        .merchantId(merchantId)
                        .billDate(billDate)
                        .status(STATUS_INIT)
                        .triggeredBy(triggeredBy)
                        .xxlLogId(xxlLogId)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                reconMerchantTaskMapper.insert(t);
            }
            n++;
        }
        log.info("主对账已生成商户子任务: billDate={}, count={}", billDate, n);
        return n;
    }
}
