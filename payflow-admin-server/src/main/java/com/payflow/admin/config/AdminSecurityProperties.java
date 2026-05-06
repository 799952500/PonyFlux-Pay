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

    /** 是否展示「登录需验证码」元信息（登录接口始终校验验证码） */
    private boolean loginCaptchaEnabled = true;

    /** 连续失败次数阈值后锁定账号 */
    private int loginMaxFailures = 5;

    /** 锁定时长（秒） */
    private int loginLockSeconds = 900;
}
