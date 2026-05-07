package com.payflow.recon.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.recon.entity.ReconMerchantTask;
import com.payflow.recon.mapper.ReconMerchantTaskMapper;
import com.payflow.recon.service.ReconMerchantStatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商户对账子任务轮询：扫描 recon_merchant_task(INIT)，生成各商户对账单。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payflow.recon.merchant-poller.enabled", havingValue = "true")
public class ReconMerchantTaskPoller {

    private final ReconMerchantTaskMapper reconMerchantTaskMapper;
    private final ReconMerchantStatementService reconMerchantStatementService;

    @Scheduled(fixedDelayString = "${payflow.recon.merchant-poller.fixed-delay-ms:5000}")
    public void poll() {
        List<ReconMerchantTask> list = reconMerchantTaskMapper.selectList(
                Wrappers.<ReconMerchantTask>lambdaQuery()
                        .eq(ReconMerchantTask::getStatus, "INIT")
                        .orderByAsc(ReconMerchantTask::getCreatedAt)
                        .last("LIMIT 10"));
        for (ReconMerchantTask t : list) {
            try {
                reconMerchantStatementService.execute(t);
            } catch (Exception e) {
                log.warn("商户对账子任务执行失败: merchantTaskId={}, message={}",
                        t.getMerchantTaskId(), e.getMessage());
            }
        }
    }
}
