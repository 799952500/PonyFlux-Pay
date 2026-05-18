package com.payflow.cashier.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 商户安全与审计配置。
 *
 * @author PayFlow Team
 */
@Data
@ConfigurationProperties(prefix = "payflow.security")
public class MerchantSecurityProperties {

    private Audit audit = new Audit();

    @Data
    public static class Audit {
        /** 时间窗口内越权拒绝次数阈值 */
        private int alertThreshold = 20;
        /** 告警统计窗口（分钟） */
        private int alertWindowMinutes = 5;
    }
}
