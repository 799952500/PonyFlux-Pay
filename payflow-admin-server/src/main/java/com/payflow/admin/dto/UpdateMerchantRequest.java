package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新商户请求 DTO，替代 Map&lt;String, Object&gt; 参数。
 *
 * @author PayFlow Team
 */
@Data
public class UpdateMerchantRequest {

    @Size(min = 1, max = 100, message = "商户名称长度需在1-100之间")
    private String merchantName;

    @Size(min = 16, max = 256, message = "商户密钥长度需在16-256之间")
    private String merchantKey;

    @Size(max = 500, message = "回调地址长度不能超过500")
    private String callbackUrl;

    @Size(max = 500, message = "通知地址长度不能超过500")
    private String notifyUrl;

    private BigDecimal commissionRate;

    @Pattern(regexp = "ACTIVE|SUSPENDED|DELETED", message = "状态值无效，仅支持 ACTIVE/SUSPENDED/DELETED")
    private String status;
}
