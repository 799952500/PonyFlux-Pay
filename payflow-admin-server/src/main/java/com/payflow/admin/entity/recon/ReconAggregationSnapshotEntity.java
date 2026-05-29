package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账差异预聚合快照（运营库 payflow_admin.recon_aggregation_snapshot）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_aggregation_snapshot")
public class ReconAggregationSnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate statDate;
    private String merchantId;
    private String channel;
    private String diffType;
    private Long diffCount;
    private Long diffAmount;
    private Long processedCount;
    private Long ignoredCount;
    private Long acceptedLossCount;
    private Long slaMetCount;
    private Long slaTotalCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

