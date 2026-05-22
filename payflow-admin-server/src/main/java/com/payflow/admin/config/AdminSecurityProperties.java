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

    /** 是否启用「密码错误后再要求验证码」策略（首次登录免验证码） */
    private boolean loginCaptchaEnabled = true;

    /** 连续失败次数阈值后锁定账号 */
    private int loginMaxFailures = 5;

    /** 锁定时长（秒） */
    private int loginLockSeconds = 900;
}
