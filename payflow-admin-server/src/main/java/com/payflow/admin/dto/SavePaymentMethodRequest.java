package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量保存商户支付方式请求 DTO，替代 Map&lt;String, Object&gt; 参数。
 *
 * @author PayFlow Team
 */
@Data
public class SavePaymentMethodRequest {

    @NotBlank(message = "商户号不能为空")
    private String merchantId;

    @NotNull(message = "支付方式ID列表不能为null")
    private List<Long> paymentMethodIds;
}
