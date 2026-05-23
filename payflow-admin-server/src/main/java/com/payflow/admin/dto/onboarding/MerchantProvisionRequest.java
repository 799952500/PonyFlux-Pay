package com.payflow.admin.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端调用收银台内部接口，写入 cashier_merchants。
 */
@Data
public class MerchantProvisionRequest {

    @NotBlank
    private String merchantId;

    @NotBlank
    private String merchantName;

    @NotBlank
    private String appSecret;

    @NotBlank
    private String passwordHash;

    private String contact;

    private String phone;

    private String email;

    private String description;
}
