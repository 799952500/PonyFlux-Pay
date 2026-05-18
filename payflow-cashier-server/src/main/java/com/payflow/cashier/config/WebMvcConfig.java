package com.payflow.cashier.config;

import com.payflow.cashier.middleware.JwtAuthInterceptor;
import com.payflow.cashier.middleware.MerchantContextInterceptor;
import com.payflow.cashier.middleware.MerchantIdBindingInterceptor;
import com.payflow.cashier.middleware.MerchantRateLimitInterceptor;
import com.payflow.cashier.middleware.MerchantResourceOwnershipInterceptor;
import com.payflow.cashier.middleware.MerchantSignatureInterceptor;
import com.payflow.cashier.middleware.PaymentIdempotencyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：认证 → 商户上下文 → merchantId 绑定 → 资源所有权。
 *
 * @author PayFlow Team
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String[] MERCHANT_API_PATTERNS = {
            "/api/v1/merchant/**",
            "/api/v1/payments/**",
            "/api/v1/refunds/**",
            "/api/v1/payment-links/**"
    };

    private static final String[] MERCHANT_API_EXCLUDES = {
            "/api/v1/payments/status/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/error"
    };

    private static final String[] ORDER_API_PATTERNS = {"/api/v1/orders/**"};

    private static final String[] ORDER_API_EXCLUDES = {
            "/api/v1/auth/**",
            "/api/v1/cashier/**",
            "/api/v1/payments/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/error"
    };

    private static final String[] ISOLATION_PATTERNS = {
            "/api/v1/orders/**",
            "/api/v1/merchant/**",
            "/api/v1/payments/**",
            "/api/v1/refunds/**",
            "/api/v1/payment-links/**"
    };

    private static final String[] ISOLATION_EXCLUDES = {
            "/api/v1/payments/status/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**",
            "/error"
    };

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final MerchantSignatureInterceptor merchantSignatureInterceptor;
    private final MerchantRateLimitInterceptor merchantRateLimitInterceptor;
    private final PaymentIdempotencyInterceptor paymentIdempotencyInterceptor;
    private final MerchantContextInterceptor merchantContextInterceptor;
    private final MerchantIdBindingInterceptor merchantIdBindingInterceptor;
    private final MerchantResourceOwnershipInterceptor merchantResourceOwnershipInterceptor;

    public WebMvcConfig(JwtAuthInterceptor jwtAuthInterceptor,
                        MerchantSignatureInterceptor merchantSignatureInterceptor,
                        MerchantRateLimitInterceptor merchantRateLimitInterceptor,
                        PaymentIdempotencyInterceptor paymentIdempotencyInterceptor,
                        MerchantContextInterceptor merchantContextInterceptor,
                        MerchantIdBindingInterceptor merchantIdBindingInterceptor,
                        MerchantResourceOwnershipInterceptor merchantResourceOwnershipInterceptor) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.merchantSignatureInterceptor = merchantSignatureInterceptor;
        this.merchantRateLimitInterceptor = merchantRateLimitInterceptor;
        this.paymentIdempotencyInterceptor = paymentIdempotencyInterceptor;
        this.merchantContextInterceptor = merchantContextInterceptor;
        this.merchantIdBindingInterceptor = merchantIdBindingInterceptor;
        this.merchantResourceOwnershipInterceptor = merchantResourceOwnershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(merchantSignatureInterceptor)
                .addPathPatterns(MERCHANT_API_PATTERNS)
                .excludePathPatterns(MERCHANT_API_EXCLUDES);

        registry.addInterceptor(merchantRateLimitInterceptor)
                .addPathPatterns(MERCHANT_API_PATTERNS)
                .excludePathPatterns(MERCHANT_API_EXCLUDES);

        registry.addInterceptor(paymentIdempotencyInterceptor)
                .addPathPatterns("/api/v1/payments")
                .excludePathPatterns("/api/v1/payments/status/**");

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns(ORDER_API_PATTERNS)
                .excludePathPatterns(ORDER_API_EXCLUDES);

        registry.addInterceptor(merchantContextInterceptor)
                .addPathPatterns(ISOLATION_PATTERNS)
                .excludePathPatterns(ISOLATION_EXCLUDES);

        registry.addInterceptor(merchantIdBindingInterceptor)
                .addPathPatterns(ISOLATION_PATTERNS)
                .excludePathPatterns(ISOLATION_EXCLUDES);

        registry.addInterceptor(merchantResourceOwnershipInterceptor)
                .addPathPatterns(ISOLATION_PATTERNS)
                .excludePathPatterns(ISOLATION_EXCLUDES);
    }
}
