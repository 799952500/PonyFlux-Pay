package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.payflow.admin.config.EncryptedStringTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
/**
 * @author Lucas
 */
@TableName("admin_merchants")
public class Merchant {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    /** 商户号（如 M2024040001） */
    private String merchantId;
    /** 商户名称 */
    private String merchantName;
    /** 商户密钥（用于签名验证） */
    @JsonProperty(access = Access.WRITE_ONLY)
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String merchantKey;
    /** 支付结果回调地址 */
    private String callbackUrl;
    /** 通知地址 */
    private String notifyUrl;
    /** 手续费分成比例 */
    private BigDecimal commissionRate;
    /** ACTIVE/SUSPENDED */
    private String status;

    /** 费率计算模式: flat=全额匹配, segmented=分段累计 */
    private String rateCalcMode;

    /** 商户所属费率组 */
    private String merchantGroup;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}