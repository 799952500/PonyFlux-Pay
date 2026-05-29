package com.payflow.recon.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 对账自定义 Prometheus 指标。
 */
@Configuration
public class ReconMetricsConfig {

    public static final String METRIC_TASK_DURATION = "recon.task.duration";
    public static final String METRIC_TASK_FAILURES = "recon.task.failures";
    public static final String METRIC_DIFF_COUNT = "recon.diff.count";

    @Bean
    Timer reconTaskDurationTimer(MeterRegistry registry) {
        return Timer.builder(METRIC_TASK_DURATION)
                .description("对账任务执行耗时")
                .register(registry);
    }
}
