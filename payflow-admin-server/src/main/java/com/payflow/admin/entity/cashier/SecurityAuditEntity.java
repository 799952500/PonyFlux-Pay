package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户安全审计记录（cashier 库）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("cashier_security_audit")
public class SecurityAuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantId;

    private String targetMerchantId;

    private String authMode;

    private String httpMethod;

    private String requestPath;

    private String resourceType;

    private String resourceId;

    private String clientIp;

    private String userAgent;

    private String outcome;

    private String reasonCode;

    private String reasonDetail;

    private LocalDateTime createdAt;
}
