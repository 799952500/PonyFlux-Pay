package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
/**
 * @author Lucas
 */
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码 ID（密码错误后必填，GET /auth/captcha 返回） */
    private String captchaId;

    /** 用户输入的验证码答案（密码错误后必填） */
    private String captchaAnswer;
}
