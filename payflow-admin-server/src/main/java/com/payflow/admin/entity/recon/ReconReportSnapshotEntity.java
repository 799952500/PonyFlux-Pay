package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账报告快照（运营库 payflow_admin.recon_report_snapshot）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_report_snapshot")
public class ReconReportSnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String snapshotId;
    private String subscriberId;
    private String reportType;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private String payloadJson;
    private LocalDateTime generatedAt;
}

