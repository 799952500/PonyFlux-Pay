package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
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
@TableName("admin_channel_routes")
public class ChannelRoute {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include    private Long id;

    private String merchantId;

    private Long channelId;

    private Long paymentAccountId;

    private Boolean enabled;

    private Integer priority;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}