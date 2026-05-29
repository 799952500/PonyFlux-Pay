package com.payflow.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 通知列表返回 DTO。
 */
@Data
public class NotificationDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String bizType;

    private String title;

    private String summary;

    private String link;

    private Integer readStatus;

    private String createdAt;
}
