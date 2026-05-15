package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.payflow.admin.config.EncryptedStringTypeHandler;
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
@TableName("payment_accounts")
/**
 * @author Lucas
 */
public class PaymentAccount {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    private Long channelId;

    private String accountCode;

    private String accountName;

    @TableField(exist = false)
    private String channelName;

    private String appId;

    @JsonProperty(access = Access.WRITE_ONLY)
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String appSecret;

    private String mchId;

    @JsonProperty(access = Access.WRITE_ONLY)
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String mchKey;

    private String certPath;

    @JsonProperty(access = Access.WRITE_ONLY)
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
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

