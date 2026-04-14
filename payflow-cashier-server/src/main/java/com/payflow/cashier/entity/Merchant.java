package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商户实体类
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("merchants")
@Schema(name = "Merchant", description = "商户实体")
public class Merchant {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 商户号 */
    @TableField("merchant_id")
    @Schema(description = "商户号")
    private String merchantId;

    /** 商户名称 */
    @TableField("merchant_name")
    @Schema(description = "商户名称")
    private String merchantName;

    /** 登录密码（MD5） */
    @TableField("password")
    @Schema(description = "登录密码")
    private String password;

    /** 商户签名密钥（HMAC-SHA256 签名用） */
    @TableField("app_secret")
    @Schema(description = "商户签名密钥")
    private String appSecret;

    /** 商户状态 */
    @Schema(description = "商户状态")
    private String status;

    /** 允许的支付渠道 */
    @TableField("allowed_channels")
    @Schema(description = "允许的支付渠道")
    private String allowedChannels;

    /** 允许的支付方式 */
    @TableField("allowed_pay_methods")
    @Schema(description = "允许的支付方式")
    private String allowedPayMethods;

    /** 联系人 */
    @Schema(description = "联系人")
    private String contact;

    /** 联系电话 */
    @Schema(description = "联系电话")
    private String phone;

    /** 联系邮箱 */
    @Schema(description = "联系邮箱")
    private String email;

    /** 商户描述 */
    @Schema(description = "商户描述")
    private String description;

    /** 创建时间 */
    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // ==================== 状态常量 ====================

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
}
