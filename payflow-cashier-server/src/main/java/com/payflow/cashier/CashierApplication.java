package com.payflow.cashier;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * PayFlow 收银台应用启动类
 *
 * @author PayFlow Team
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.payflow.cashier.mapper")
@EnableAsync
public class CashierApplication {

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.error("========================================");
            log.error("PayFlow 收银台服务收到停止信号，即将关闭");
            log.error("========================================");
        }, "cashier-shutdown-hook"));

        log.info("PayFlow 收银台服务启动中...");
        SpringApplication.run(CashierApplication.class, args);
        log.info("PayFlow 收银台服务启动完成，监听端口 3002");
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cashier-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
