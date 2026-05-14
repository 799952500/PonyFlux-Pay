package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BI仪表盘预聚合指标
 *
 * @author PayFlow Team
 */
@Data
@TableName("admin_dashboard_metrics")
public class DashboardMetrics {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指标时间 */
    private LocalDateTime metricTime;

    /** 粒度: 5min / hour / day */
    private String granularity;

    /** 渠道代码，ALL=汇总 */
    private String channelCode;

    /** 交易总额(分) */
    private Long totalAmount;

    /** 交易笔数 */
    private Integer totalCount;

    /** 活跃商户数 */
    private Integer activeMerchants;

    /** 手续费收入(分) */
    private Long feeIncome;

    /** 退款金额(分) */
    private Long refundAmount;

    /** 退款笔数 */
    private Integer refundCount;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
