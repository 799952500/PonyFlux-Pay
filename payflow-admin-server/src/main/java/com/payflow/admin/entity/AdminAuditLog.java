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

    private String action;

    private String resourcePath;

    private String detail;

    private String clientIp;

    private LocalDateTime createdAt;
}
