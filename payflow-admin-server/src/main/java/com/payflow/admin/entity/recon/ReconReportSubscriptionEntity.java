package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账报告订阅（运营库 payflow_admin.recon_report_subscription）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_report_subscription")
public class ReconReportSubscriptionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String subscriberId;
    private String reportType;
    private String scope;
    private Integer enabled;
    private LocalDateTime lastSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

