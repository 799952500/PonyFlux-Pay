package com.payflow.admin.dto;

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

    /** 商户管理员可不传，由服务端按登录授权范围回填 */
    private String merchantId;

    @NotNull(message = "支付方式ID列表不能为null")
    private List<Long> paymentMethodIds;
}
