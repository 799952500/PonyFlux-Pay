package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户 Webhook 端点配置。
 *
 * @author PayFlow Team
 */
@Data
@TableName("merchant_webhook_endpoint")
public class MerchantWebhookEndpoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户号 */
    private String merchantId;

    /** Webhook 回调 URL */
    private String url;

    /** HMAC-SHA256 签名密钥 */
    private String secret;

    /** 订阅事件代码（逗号分隔，如 "payment.success,refund.success"） */
    private String eventCodes;

    /** 是否启用 */
    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
