package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchants")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantId;          // 商户号（如 M2024040001）
    private String merchantName;         // 商户名称
    private String merchantKey;           // 商户密钥（用于签名验证）
    private String callbackUrl;          // 支付结果回调地址
    private String notifyUrl;             // 通知地址
    private BigDecimal commissionRate;    // 手续费分成比例
    private String status;               // ACTIVE/SUSPENDED

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}