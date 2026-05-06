package com.payflow.recon.job;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.recon.kit.ReconChannelKit;
import com.payflow.recon.mapper.cashier.CashierReconAccountMapper;
import com.payflow.recon.mapper.cashier.CashierReconAccountRow;
import com.payflow.recon.service.ReconExecuteService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * xxl-job：对账任务入口。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconcileJobHandler {

    private final CashierReconAccountMapper cashierReconAccountMapper;
    private final ReconExecuteService reconExecuteService;

    /**
     * 每日对账：默认账单日为昨天，遍历所有启用中的支付宝/微信渠道账户。
     */
    @XxlJob("reconcileDailyJobHandler")
    public void reconcileDaily() {
        LocalDate billDate = LocalDate.now().minusDays(1);
        long logId = XxlJobHelper.getJobId();
        List<CashierReconAccountRow> rows = cashierReconAccountMapper.listEnabledForRecon();
        XxlJobHelper.log("对账日={}, 账户数={}", billDate, rows.size());
        for (CashierReconAccountRow row : rows) {
            try {
                String reconCh = ReconChannelKit.cashierChannelToRecon(row.getChannelCode());
                String taskId = reconExecuteService.execute(reconCh, row.getAccountCode(), billDate, "XXL_JOB", logId);
                XxlJobHelper.log("完成: account={}, taskId={}", row.getAccountCode(), taskId);
            } catch (Exception e) {
                XxlJobHelper.log("失败: account={}, error={}", row.getAccountCode(), e.getMessage());
                log.error("对账任务失败: account={}", row.getAccountCode(), e);
            }
        }
    }

    /**
     * 单笔补跑：参数 JSON {@code {"reconChannel":"alipay","accountCode":"ALIPAY_ACC_001","billDate":"2026-05-05"}}。
     */
    @XxlJob("reconcileSingleHandler")
    public void reconcileSingle() {
        String param = XxlJobHelper.getJobParam();
        JSONObject o = JSONUtil.parseObj(param == null || param.isBlank() ? "{}" : param);
        String reconChannel = o.getStr("reconChannel");
        String accountCode = o.getStr("accountCode");
        LocalDate billDate = LocalDate.parse(o.getStr("billDate"));
        long logId = XxlJobHelper.getJobId();
        String taskId = reconExecuteService.execute(reconChannel, accountCode, billDate, "XXL_JOB", logId);
        XxlJobHelper.log("完成 taskId={}", taskId);
    }
}
