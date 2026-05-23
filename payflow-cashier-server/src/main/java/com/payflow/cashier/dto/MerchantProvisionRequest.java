package com.payflow.cashier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端同步商户到收银台库。
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
