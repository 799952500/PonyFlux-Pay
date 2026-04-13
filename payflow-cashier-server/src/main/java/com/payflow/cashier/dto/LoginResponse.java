package com.payflow.cashier.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 商户登录响应 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LoginResponse", description = "商户登录响应")
public class LoginResponse {

    /** JWT Token */
    @Schema(description = "JWT Token")
    private String token;

    /** 商户号 */
    @Schema(description = "商户号")
    private String merchantId;

    /** 商户名称 */
    @Schema(description = "商户名称")
    private String merchantName;

    /** Token 过期时间（ISO 8601 格式） */
    @Schema(description = "Token过期时间")
    private String expireTime;
}
