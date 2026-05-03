package com.payflow.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 管理端登录安全相关开关。
 *
 * @author Lucas
 */
@Data
@ConfigurationProperties(prefix = "payflow.security")
public class AdminSecurityProperties {

    /** 是否要求图形验证码（算术题） */
    private boolean loginCaptchaEnabled = false;

    /** 连续失败次数阈值后锁定账号 */
    private int loginMaxFailures = 5;

    /** 锁定时长（秒） */
    private int loginLockSeconds = 900;
}
