package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 阶梯费率配置
 *
 * @author PayFlow Team
 */
@Data
@TableName("admin_fee_rate_config")
public class FeeRateConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 适用范围: global / merchant_group */
    @NotBlank(message = "scopeType 不能为空")
    private String scopeType;

    /** 商户组名称(scope_type=merchant_group时) */
    private String scopeValue;

    /** 渠道代码，ALL=全渠道 */
    @NotBlank(message = "channelCode 不能为空")
    private String channelCode;

    /** 区间下限(分) */
    @Min(value = 0, message = "tierMin 不能为负数")
    private Long tierMin;

    /** 区间上限(分)，NULL=无上限 */
    private Long tierMax;

    /** 费率(如0.0060=0.6%) */
    @DecimalMin(value = "0.0", message = "费率不能为负数")
    @DecimalMax(value = "1.0", message = "费率不能超过100%")
    private BigDecimal feeRate;

    /** 计算模式: flat=全额匹配, segmented=分段累计 */
    private String calcMode;

    /** 优先级，组规则>全局默认 */
    private Integer priority;

    /** 状态: enabled / disabled */
    private String status;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
