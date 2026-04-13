package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商户登录请求 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginRequest", description = "商户登录请求")
public class LoginRequest {

    /** 商户号 */
    @NotBlank(message = "merchantId 不能为空")
    @Schema(description = "商户号")
    private String merchantId;

    /** 登录密码 */
    @NotBlank(message = "password 不能为空")
    @Schema(description = "登录密码")
    private String password;
}
