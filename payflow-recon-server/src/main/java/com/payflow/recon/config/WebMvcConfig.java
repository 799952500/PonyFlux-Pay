package com.payflow.recon.config;

import com.payflow.recon.interceptor.InternalApiTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC：内部接口鉴权。
 *
 * @author PayFlow Team
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final InternalApiTokenInterceptor internalApiTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalApiTokenInterceptor)
                .addPathPatterns("/api/v1/internal/recon/**");
    }
}
