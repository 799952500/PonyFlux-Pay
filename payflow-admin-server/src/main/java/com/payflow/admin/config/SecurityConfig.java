package com.payflow.admin.config;

import com.payflow.admin.interceptor.AdminAuditInterceptor;
import com.payflow.admin.interceptor.InternalApiTokenInterceptor;
import com.payflow.admin.interceptor.JwtInterceptor;
import com.payflow.admin.interceptor.PermissionInterceptor;
import com.payflow.admin.interceptor.RoleBasedInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * JWT 拦截策略与 CORS。
 * 菜单「按钮显隐」应与后端接口权限一致，新增敏感接口时请同步校验角色或数据范围（{@link com.payflow.admin.kit.AdminRequestContext}）。
 *
 * <p>CORS 白名单通过 {@code payflow.cors.allowed-origins} 配置，生产环境禁止使用通配符。</p>
 *
 * @author Lucas
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RoleBasedInterceptor roleBasedInterceptor;
    private final PermissionInterceptor permissionInterceptor;
    private final AdminAuditInterceptor adminAuditInterceptor;
    private final InternalApiTokenInterceptor internalApiTokenInterceptor;

    /**
     * CORS 允许的来源白名单，从配置文件读取。
     * 生产环境必须显式列出域名，禁止使用通配符 "*"。
     */
    @Value("#{'${payflow.cors.allowed-origins:http://localhost:3001,http://127.0.0.1:3001}'.split(',')}")
    private List<String> allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalApiTokenInterceptor)
                .addPathPatterns("/api/v1/internal/**");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/admin/**", "/api/v1/merchants/**")
                .excludePathPatterns(
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/captcha",
                        "/api/v1/admin/auth/captcha-required",
                        "/api/v1/admin/auth/logout",
                        "/api/v1/admin/meta/**"
                );
        registry.addInterceptor(roleBasedInterceptor)
                .addPathPatterns("/api/v1/admin/**", "/api/v1/merchants/**")
                .excludePathPatterns(
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/captcha",
                        "/api/v1/admin/auth/captcha-required",
                        "/api/v1/admin/auth/logout",
                        "/api/v1/admin/meta/**"
                );
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/v1/admin/**", "/api/v1/merchants/**")
                .excludePathPatterns(
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/captcha",
                        "/api/v1/admin/auth/captcha-required",
                        "/api/v1/admin/auth/logout",
                        "/api/v1/admin/meta/**"
                );
        registry.addInterceptor(adminAuditInterceptor)
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns(
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/captcha",
                        "/api/v1/admin/auth/captcha-required",
                        "/api/v1/admin/auth/logout",
                        "/api/v1/admin/meta/**"
                );
    }

    /**
     * 配置 CORS 映射，仅允许白名单中的来源访问。
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("CORS 白名单加载: {}", allowedOrigins);
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600L);
    }

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilter() {
        FilterRegistrationBean<TraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE + 1);
        return registration;
    }
}
