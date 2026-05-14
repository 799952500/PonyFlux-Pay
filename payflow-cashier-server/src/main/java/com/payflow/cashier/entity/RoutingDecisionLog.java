package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路由决策日志（90天保留，异步写入）
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_routing_decision_log")
public class RoutingDecisionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 交易流水号 */
    private String tradeNo;

    /** 商户ID */
    private Long merchantId;

    /** 可选渠道列表 JSON: [{code, rate, available}] */
    private String availableChannels;

    /** 最终选择渠道 */
    private String selectedChannel;

    /** 选择原因: lowest_cost / fallback / none_available */
    private String selectionReason;

    /** 决策耗时(毫秒) */
    private Integer decisionCostMs;

    /** 降级次数 */
    private Integer fallbackCount;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
