package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账差异 SLA 规则（运营库 payflow_admin.recon_diff_sla_rule）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_diff_sla_rule")
public class ReconDiffSlaRuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String diffType;
    private Integer enabled;
    private Integer slaHours;
    private BigDecimal dueSoonRatio;
    private String escalateToRole;
    private String updatedBy;
    private LocalDateTime updatedAt;
}

