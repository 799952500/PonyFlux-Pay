package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户流失预警记录
 *
 * @author PayFlow Team
 */
@Data
@TableName("admin_churn_alert")
public class ChurnAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户ID */
    private Long merchantId;

    /** 商户名称(冗余) */
    private String merchantName;

    /** 预警等级: yellow / orange / red */
    private String alertLevel;

    /** 近7天日均笔数 */
    private BigDecimal currentAvgCount;

    /** 前7天日均笔数 */
    private BigDecimal baselineAvgCount;

    /** 下降百分比 */
    private BigDecimal declinePct;

    /** 连续下降天数 */
    private Integer consecutiveDays;

    /** 状态: pending / in_progress / resolved / false_alarm */
    private String status;

    /** 跟进人 */
    private String assignee;

    /** 跟进备注 */
    private String note;

    /** 处理完成时间 */
    private LocalDateTime resolvedTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
