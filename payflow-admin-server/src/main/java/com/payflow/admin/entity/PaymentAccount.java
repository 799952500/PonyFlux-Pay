package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_accounts")
public class PaymentAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    private String accountCode;

    private String accountName;

    @TableField(exist = false)
    private String channelName;

    private String appId;

    private String appSecret;

    private String mchId;

    private String mchKey;

    private String certPath;

    private String certPassword;

    private String configJson;

    private Boolean enabled;

    private Integer priority;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

