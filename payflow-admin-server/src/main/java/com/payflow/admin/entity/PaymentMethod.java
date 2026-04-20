package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("payment_methods")
public class PaymentMethod {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String methodCode;
    private String methodName;
    private Long channelId;              // 所属渠道ID
    private String appId;               // 应用ID（微信appId/支付宝appId）
    private String appSecret;            // 应用密钥
    private String mchId;              // 商户号
    private String mchKey;             // 商户密钥
    private String certPath;            // 证书路径
    private String certPassword;        // 证书密码
    private String configJson;          // 扩展配置JSON
    private Boolean enabled;
    private Integer priority;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}