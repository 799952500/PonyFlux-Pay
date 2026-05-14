package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName("channels")
public class Channel {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    private String channelCode;
    private String channelName;
    /** WECHAT/ALIPAY/UNION/CARD */
    private String channelType;
    /** 渠道API地址 */
    private String apiUrl;
    /** 渠道密钥/公钥 */
    private String apiKey;
    private Boolean enabled;
    private Integer priority;
    private String icon;
    private String description;

    /** 渠道默认手续费率（如0.0060=0.6%） */
    private BigDecimal feeRate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}