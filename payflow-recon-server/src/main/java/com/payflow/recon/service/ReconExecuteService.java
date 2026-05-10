package com.payflow.recon.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.common.exception.BizException;
import com.payflow.recon.entity.ReconBillRecord;
import com.payflow.recon.entity.ReconTask;
import com.payflow.recon.kit.ReconChannelKit;
import com.payflow.recon.mapper.ReconBillRecordMapper;
import com.payflow.recon.mapper.ReconDiffMapper;
import com.payflow.recon.mapper.ReconTaskMapper;
import com.payflow.recon.mapper.cashier.CashierReconAccountMapper;
import com.payflow.recon.mapper.cashier.CashierReconAccountRow;
import com.payflow.recon.mapper.cashier.CashierReconPaymentMapper;
import com.payflow.recon.mapper.cashier.CashierReconPaymentRow;
import com.payflow.recon.model.BillDownloadResult;
import com.payflow.recon.model.PayChannelAccountView;
import com.payflow.recon.openservice.bill.ReconChannelOpenService;
import com.payflow.recon.openservice.bill.ReconChannelOpenServiceLocator;
import com.payflow.recon.storage.ReconFileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单任务对账编排：下载 → 存储 → 解析入库 → 比对。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconExecuteService {

    private static final String STATUS_INIT = "INIT";
    private static final String STATUS_DOWNLOADING = "DOWNLOADING";
    private static final String STATUS_PARSING = "PARSING";
    private static final String STATUS_COMPARING = "COMPARING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";

    private final CashierReconAccountMapper cashierReconAccountMapper;
    private final ReconChannelOpenServiceLocator reconChannelOpenServiceLocator;
    private final ReconFileStorage reconFileStorage;
    private final ReconTaskMapper reconTaskMapper;
    private final ReconBillRecordMapper reconBillRecordMapper;
    private final ReconDiffMapper reconDiffMapper;
    private final ReconCompareService reconCompareService;
    private final ReconDiffHealService reconDiffHealService;
    private final CashierReconPaymentMapper cashierReconPaymentMapper;

    /**
     * 执行对账任务。
     *
     * @param reconChannel 小写 alipay / wxpay
     * @param accountCode  渠道账户编码
     * @param billDate     账单日
     * @param triggeredBy  触发来源
     * @param xxlLogId     xxl 日志 ID，可空
     */
    public String execute(String reconChannel, String accountCode, LocalDate billDate,
                          String triggeredBy, Long xxlLogId) {
        long t0 = System.currentTimeMillis();
        CashierReconAccountRow row = cashierReconAccountMapper.findByAccountCode(accountCode);
        if (row == null) {
            throw new BizException(7530, "渠道账户不存在或未启用: " + accountCode);
        }
        String expected = ReconChannelKit.cashierChannelToRecon(row.getChannelCode());
        if (!expected.equals(reconChannel)) {
            throw new BizException(7531, "渠道与账户不匹配: channel=" + reconChannel + ", account=" + accountCode);
        }

        ReconTask existing = reconTaskMapper.selectOne(
                Wrappers.<ReconTask>lambdaQuery()
                        .eq(ReconTask::getChannel, reconChannel)
                        .eq(ReconTask::getAccountCode, accountCode)
                        .eq(ReconTask::getBillDate, billDate)
                        .eq(ReconTask::getBillType, "trade"));
        if (existing != null && STATUS_SUCCESS.equals(existing.getStatus())) {
            log.info("对账任务已成功，跳过: taskId={}", existing.getTaskId());
            return existing.getTaskId();
        }

        String taskId = existing != null ? existing.getTaskId() : ReconIdGenerator.newTaskId();
        ReconTask task = buildOrResetTask(existing, taskId, reconChannel, accountCode, billDate, triggeredBy, xxlLogId);
        if (existing == null) {
            reconTaskMapper.insert(task);
        } else {
            reconTaskMapper.updateById(task);
            reconBillRecordMapper.delete(Wrappers.<ReconBillRecord>lambdaQuery().eq(ReconBillRecord::getTaskId, taskId));
            reconDiffMapper.delete(Wrappers.lambdaQuery(com.payflow.recon.entity.ReconDiff.class)
                    .eq(com.payflow.recon.entity.ReconDiff::getTaskId, taskId));
        }

        try {
            updateTaskStatus(taskId, STATUS_DOWNLOADING, null);
            PayChannelAccountView accountView = PayChannelAccountView.builder()
                    .id(row.getId())
                    .accountCode(row.getAccountCode())
                    .channelConfig(row.getChannelConfig())
                    .build();
            ReconChannelOpenService open = reconChannelOpenServiceLocator.requireByChannelCode(reconChannel);
            BillDownloadResult downloaded = open.downloadBill(billDate, "trade", accountView);

            String storageKey = reconFileStorage.put(downloaded.getCsvPath(), downloaded.getOriginalFileName());
            long fileSize = downloaded.getSizeBytes();

            updateTaskFile(taskId, storageKey, fileSize);
            updateTaskStatus(taskId, STATUS_PARSING, null);

            List<ReconBillRecord> records = open.parseBill(downloaded, taskId);
            persistBillRecords(records);

            long billTotalAmount = records.stream()
                    .filter(r -> Boolean.FALSE.equals(r.getParseError()))
                    .mapToLong(r -> r.getAmountFen() != null ? r.getAmountFen() : 0L)
                    .sum();
            int billCount = (int) records.stream().filter(r -> Boolean.FALSE.equals(r.getParseError())).count();

            updateTaskStatus(taskId, STATUS_COMPARING, null);
            int diffCount = reconCompareService.compareAndPersist(taskId, reconChannel, billDate);
            reconDiffHealService.annotateSuggestions(taskId);

            String payChannel = ReconChannelKit.reconToPayChannel(reconChannel);
            List<CashierReconPaymentRow> localRows = cashierReconPaymentMapper.listSuccessByBillDate(payChannel, billDate);
            int localCount = localRows.size();
            long localAmount = localRows.stream()
                    .mapToLong(p -> p.getAmount() != null ? p.getAmount() : 0L)
                    .sum();

            ReconTask done = reconTaskMapper.selectOne(
                    Wrappers.<ReconTask>lambdaQuery().eq(ReconTask::getTaskId, taskId));
            if (done != null) {
                done.setStatus(STATUS_SUCCESS);
                done.setBillTotalCount(billCount);
                done.setBillTotalAmount(billTotalAmount);
                done.setLocalTotalCount(localCount);
                done.setLocalTotalAmount(localAmount);
                done.setDiffCount(diffCount);
                done.setElapsedMs(System.currentTimeMillis() - t0);
                done.setUpdatedAt(LocalDateTime.now());
                reconTaskMapper.updateById(done);
            }
            return taskId;
        } catch (BizException e) {
            markFail(taskId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("对账任务失败: taskId={}", taskId, e);
            markFail(taskId, e.getMessage());
            throw new BizException(7599, "对账任务失败", e);
        }
    }

    private ReconTask buildOrResetTask(ReconTask existing, String taskId, String reconChannel,
                                       String accountCode, LocalDate billDate,
                                       String triggeredBy, Long xxlLogId) {
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setStatus(STATUS_INIT);
            existing.setFileObjectKey(null);
            existing.setFileSize(null);
            existing.setBillTotalCount(null);
            existing.setBillTotalAmount(null);
            existing.setLocalTotalCount(null);
            existing.setLocalTotalAmount(null);
            existing.setDiffCount(0);
            existing.setElapsedMs(null);
            existing.setErrorMsg(null);
            existing.setTriggeredBy(triggeredBy);
            existing.setXxlLogId(xxlLogId);
            existing.setUpdatedAt(now);
            return existing;
        }
        return ReconTask.builder()
                .taskId(taskId)
                .channel(reconChannel)
                .accountCode(accountCode)
                .billDate(billDate)
                .billType("trade")
                .status(STATUS_INIT)
                .diffCount(0)
                .triggeredBy(triggeredBy)
                .xxlLogId(xxlLogId)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void updateTaskStatus(String taskId, String status, String err) {
        ReconTask t = reconTaskMapper.selectOne(Wrappers.<ReconTask>lambdaQuery().eq(ReconTask::getTaskId, taskId));
        if (t != null) {
            t.setStatus(status);
            t.setErrorMsg(err);
            t.setUpdatedAt(LocalDateTime.now());
            reconTaskMapper.updateById(t);
        }
    }

    private void updateTaskFile(String taskId, String key, long size) {
        ReconTask t = reconTaskMapper.selectOne(Wrappers.<ReconTask>lambdaQuery().eq(ReconTask::getTaskId, taskId));
        if (t != null) {
            t.setFileObjectKey(key);
            t.setFileSize(size);
            t.setUpdatedAt(LocalDateTime.now());
            reconTaskMapper.updateById(t);
        }
    }

    @Transactional(transactionManager = "adminTransactionManager")
    public void persistBillRecords(List<ReconBillRecord> records) {
        LocalDateTime now = LocalDateTime.now();
        for (ReconBillRecord r : records) {
            r.setCreatedAt(now);
            if (r.getParseError() == null) {
                r.setParseError(false);
            }
            reconBillRecordMapper.insert(r);
        }
    }

    private void markFail(String taskId, String msg) {
        ReconTask t = reconTaskMapper.selectOne(Wrappers.<ReconTask>lambdaQuery().eq(ReconTask::getTaskId, taskId));
        if (t != null) {
            t.setStatus(STATUS_FAIL);
            t.setErrorMsg(msg != null && msg.length() > 1000 ? msg.substring(0, 1000) : msg);
            t.setUpdatedAt(LocalDateTime.now());
            reconTaskMapper.updateById(t);
        }
    }
}
