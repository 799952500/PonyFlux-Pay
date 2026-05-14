package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费率变更审计日志
 *
 * @author PayFlow Team
 */
@Data
@TableName("admin_fee_rate_audit_log")
public class FeeRateAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户ID */
    private Long merchantId;

    /** 变更时间 */
    private LocalDateTime changeTime;

    /** 旧费率 */
    private BigDecimal oldRate;

    /** 新费率 */
    private BigDecimal newRate;

    /** 触发原因: monthly_upgrade / manual_adjust / merchant_group_change */
    private String triggerReason;

    /** 操作人 */
    private String operator;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
