package com.payflow.recon.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.recon.entity.ReconTask;
import com.payflow.recon.mapper.ReconTaskMapper;
import com.payflow.recon.service.ReconExecuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * INIT 任务轮询执行器：由 admin 写入 recon_task(INIT)，recon-server 后台捞取执行。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payflow.recon.poller.enabled", havingValue = "true")
public class ReconTaskPoller {

    private final ReconTaskMapper reconTaskMapper;
    private final ReconExecuteService reconExecuteService;

    @Scheduled(fixedDelayString = "${payflow.recon.poller.fixed-delay-ms:5000}")
    public void poll() {
        List<ReconTask> list = reconTaskMapper.selectList(
                Wrappers.<ReconTask>lambdaQuery()
                        .eq(ReconTask::getStatus, "INIT")
                        .orderByAsc(ReconTask::getCreatedAt)
                        .last("LIMIT 10"));
        for (ReconTask t : list) {
            try {
                reconExecuteService.execute(t.getChannel(), t.getAccountCode(), t.getBillDate(), "API", null);
            } catch (Exception e) {
                log.warn("INIT 对账任务执行失败: taskId={}, message={}", t.getTaskId(), e.getMessage());
            }
        }
    }
}

