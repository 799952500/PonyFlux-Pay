package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户月费率快照
 *
 * @author PayFlow Team
 */
@Data
@TableName("admin_merchant_fee_snapshot")
public class MerchantFeeSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商户ID */
    private Long merchantId;

    /** 月份: YYYY-MM */
    private String snapshotMonth;

    /** 适用费率 */
    private BigDecimal applicableRate;

    /** 月累计交易额(分) */
    private Long monthlyAmount;

    /** 当前档位序号 */
    private Integer currentTier;

    /** 距下档还需金额(分) */
    private Long nextTierAmount;

    /** 下档费率 */
    private BigDecimal nextTierRate;

    /** 计算模式 */
    private String calcMode;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
