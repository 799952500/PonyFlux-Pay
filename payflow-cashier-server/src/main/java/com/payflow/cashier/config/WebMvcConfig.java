package com.payflow.cashier.config;

import com.payflow.cashier.middleware.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册 JWT 认证拦截器
 *
 * @author PayFlow Team
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                // 仅拦截 /api/v1/orders/**（商户管理接口，需商户认证）
                // 收银台相关接口全部公开，无需认证：
                //   - /api/v1/cashier/**   收银台信息，消费者直接访问
                //   - /api/v1/payments/**  支付接口，消费者直接调用
                .addPathPatterns("/api/v1/orders/**")
                // 排除不需要认证的路径
                .excludePathPatterns(
                        "/api/v1/auth/**",           // 认证接口
                        "/api/v1/cashier/**",        // 收银台信息（无需认证）
                        "/api/v1/payments/**",        // 支付接口（无需认证，消费者直接调用）
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/h2-console/**",
                        "/error"
                );
    }
}
