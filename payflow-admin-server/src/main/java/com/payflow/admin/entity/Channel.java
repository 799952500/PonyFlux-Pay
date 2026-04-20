package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("channels")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channelCode;
    private String channelName;
    private String channelType;        // WECHAT/ALIPAY/UNION/CARD
    private String apiUrl;           // 渠道API地址
    private String apiKey;           // 渠道密钥/公钥
    private Boolean enabled;
    private Integer priority;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}