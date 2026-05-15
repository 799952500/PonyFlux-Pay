package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付路由请求 DTO，替代 Map&lt;String, Object&gt; 参数。
 *
 * @author PayFlow Team
 */
@Data
public class PaymentRouteRequest {

    @NotBlank(message = "商户号不能为空")
    private String merchantId;

    @NotNull(message = "支付方式ID不能为空")
    private Long paymentMethodId;

    @NotNull(message = "支付账号ID不能为空")
    private Long paymentAccountId;

    private Boolean enabled;

    private Integer priority;

    private String clientScopes;
}
