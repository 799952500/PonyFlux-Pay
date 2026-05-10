package com.payflow.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI payflowAdminOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayFlow 管理后台 API")
                        .version("1.0.0")
                        .description("PonyFlux-Pay 支付网关管理后台接口文档"));
    }
}
