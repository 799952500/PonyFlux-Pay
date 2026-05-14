package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路由决策日志实体（管理后台查询用，对应 recon_routing_decision_log 表）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_routing_decision_log")
public class RoutingDecisionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tradeNo;
    private Long merchantId;
    private String availableChannels;
    private String selectedChannel;
    private String selectionReason;
    private Integer decisionCostMs;
    private Integer fallbackCount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
