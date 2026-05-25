package com.payflow.admin.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明方法所需的按钮权限码（perm_code）。
 *
 * <p>由 {@link com.payflow.admin.interceptor.PermissionInterceptor} 校验；
 * SUPER_ADMIN 角色始终放行。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 所需权限码，对应 admin_sys_menus.perm_code。
     */
    String[] value();

    /**
     * 多个权限码时的组合逻辑，默认全部满足。
     */
    LogicalOp logical() default LogicalOp.AND;
}
