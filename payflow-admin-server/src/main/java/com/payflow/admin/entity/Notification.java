package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体，映射 admin_notifications 表。
 */
@Data
@TableName("admin_notifications")
public class Notification {

    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Long recipientUserId;

    private String merchantId;

    private String bizType;

    private String bizKey;

    private String title;

    private String summary;

    private String link;

    private Integer readStatus;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}
