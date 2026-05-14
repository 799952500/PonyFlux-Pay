package com.payflow.admin.task;

import com.payflow.admin.service.FeeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 费率月度结算定时任务（每月1日00:00执行）。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeeRateMonthBeginTask {

    private final FeeRateService feeRateService;

    @Scheduled(cron = "0 0 0 1 * ?")
    public void settleRates() {
        log.info("开始月度费率结算...");
        try {
            feeRateService.settleMonthlyRates();
            log.info("月度费率结算完成");
        } catch (Exception e) {
            log.error("月度费率结算失败", e);
        }
    }
}
