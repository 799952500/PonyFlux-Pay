package com.payflow.admin.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明方法所需的角色权限。
 *
 * <p>用于 Controller 方法级别，由 {@link RoleBasedInterceptor} 拦截校验。
 * 未标注的方法默认允许所有已认证角色访问。</p>
 *
 * <p>使用示例：
 * <pre>{@code
 *   @RequireRole(SUPER_ADMIN)
 *   @PostMapping("/users")
 *   public Map<String, Object> createUser(@RequestBody SysUser user) { ... }
 * }</pre>
 *
 * @author PayFlow Team
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许的角色常量 */
    String SUPER_ADMIN = "SUPER_ADMIN";
    String ADMIN = "ADMIN";

    /**
     * 所需的角色列表，满足其一即放行。
     *
     * @return 角色代码
     */
    String[] value() default {SUPER_ADMIN};
}
