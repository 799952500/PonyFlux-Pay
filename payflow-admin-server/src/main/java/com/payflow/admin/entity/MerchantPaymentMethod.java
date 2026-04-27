package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_payment_methods")
public class MerchantPaymentMethod {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantId;       // 商户号（字符串）
    private Long paymentMethodId;
    private Boolean enabled;
    private Integer priority;
    private String customConfigJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}