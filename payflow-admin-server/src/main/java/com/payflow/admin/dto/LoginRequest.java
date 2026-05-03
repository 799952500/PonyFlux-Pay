package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * @author Lucas
 */
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    /** 验证码 ID（启用图形/算术验证码时必填） */
    private String captchaId;

    /** 用户输入的验证码答案 */
    private String captchaAnswer;
}
