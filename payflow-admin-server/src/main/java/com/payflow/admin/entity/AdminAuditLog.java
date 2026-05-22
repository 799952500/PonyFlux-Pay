package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端操作审计日志。
 *
 * @author Lucas
 */
@Data
@TableName("admin_audit_logs")
public class AdminAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String operatorType;

    private String merchantId;

    private String action;

    private String resourcePath;

    private String resourceType;

    private String resourceId;

    private String detail;

    private String result;

    private String denyReason;

    private String clientIp;

    private LocalDateTime createdAt;
}
