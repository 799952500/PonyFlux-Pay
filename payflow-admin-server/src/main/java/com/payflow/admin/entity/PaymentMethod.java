package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@TableName("payment_methods")
public class PaymentMethod {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    private String methodCode;
    private String methodName;
    private Long channelId;              // 所属渠道ID
    @TableField(exist = false)          // 非数据库字段，关联查询
    private String channelName;         // 渠道名称（前端展示用）
    @TableField(exist = false)          // 非数据库字段，关联查询
    private String channelType;         // 渠道类型 WECHAT/ALIPAY/UNION/CARD（表单下拉用）
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
    private String status;              // 状态字段（前端用）

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}