package com.payflow.cashier.config;

import com.payflow.cashier.middleware.JwtAuthInterceptor;
import com.payflow.cashier.middleware.MerchantSignatureInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册 JWT 认证拦截器和商户签名拦截器
 *
 * @author PayFlow Team
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final MerchantSignatureInterceptor merchantSignatureInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor,
                        MerchantSignatureInterceptor merchantSignatureInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.merchantSignatureInterceptor = merchantSignatureInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // ── 商户签名拦截器：拦截 /api/v1/merchant/** ──────────────────────────
        // 商户通过 HMAC-SHA256 签名调用查询接口
        registry.addInterceptor(merchantSignatureInterceptor)
                .addPathPatterns("/api/v1/merchant/**")
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/h2-console/**",
                        "/error"
                );

        // ── JWT 拦截器：拦截 /api/v1/orders/** ─────────────────────────────────
        // 商户通过 JWT Token 调用订单管理接口
        registry.addInterceptor(jwtAuthInterceptor)
                // 收银台相关接口全部公开，无需认证：
                //   - /api/v1/cashier/**   收银台信息，消费者直接访问
                //   - /api/v1/payments/**  支付接口，消费者直接调用
                //   - /api/v1/admin/**     管理接口（JWT 由前端管理，暂跳过签名）
                .addPathPatterns("/api/v1/orders/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",             // 认证接口
                        "/api/v1/cashier/**",           // 收银台信息（无需认证）
                        "/api/v1/payments/**",          // 支付接口（无需认证，消费者直接调用）
                        "/api/v1/admin/**",             // 管理接口
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/h2-console/**",
                        "/error"
                );
    }
}
