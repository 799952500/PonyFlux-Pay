package com.payflow.recon.job;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.recon.service.ReconExecuteService;
import com.payflow.recon.service.ReconTaskSeedService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * xxl-job：对账任务入口。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconcileJobHandler {

    private final ReconExecuteService reconExecuteService;
    private final ReconTaskSeedService reconTaskSeedService;

    /**
     * 主对账调度（推荐）：默认账单日为昨天，生成各支付账号子对账任务与各商户子对账任务（INIT），
     * 由 recon-server 后台轮询器异步拉取执行。
     */
    @XxlJob("reconcileMasterDailyJobHandler")
    public void reconcileMasterDaily() {
        LocalDate billDate = LocalDate.now().minusDays(1);
        long logId = XxlJobHelper.getJobId();
        XxlJobHelper.log("主对账调度开始: billDate={}", billDate);
        int accountSeeds = reconTaskSeedService.seedAccountTasks(billDate, "XXL_MASTER", logId);
        int merchantSeeds = reconTaskSeedService.seedMerchantTasks(billDate, "XXL_MASTER", logId);
        XxlJobHelper.log("已生成账户子任务={}, 商户子任务={}", accountSeeds, merchantSeeds);
    }

    /**
     * 每日对账（兼容旧任务名）：与 {@link #reconcileMasterDaily()} 相同，仅生成子任务。
     */
    @XxlJob("reconcileDailyJobHandler")
    public void reconcileDaily() {
        reconcileMasterDaily();
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
