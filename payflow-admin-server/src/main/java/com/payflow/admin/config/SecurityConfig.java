package com.payflow.admin.config;

import com.payflow.admin.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
/**
 * JWT 拦截策略与 CORS。
 * 菜单「按钮显隐」应与后端接口权限一致，新增敏感接口时请同步校验角色或数据范围（{@link com.payflow.admin.kit.AdminRequestContext}）。
 *
 * @author Lucas
 */
public class SecurityConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns(
                        "/api/v1/admin/auth/login",
                        "/api/v1/admin/auth/captcha",
                        "/api/v1/admin/meta/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
