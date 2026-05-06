package com.payflow.recon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PayFlow 对账服务入口。
 *
 * @author PayFlow Team
 */
@SpringBootApplication(scanBasePackages = "com.payflow")
public class ReconApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconApplication.class, args);
    }
}
