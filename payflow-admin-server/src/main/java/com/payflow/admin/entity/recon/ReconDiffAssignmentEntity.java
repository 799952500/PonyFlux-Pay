package com.payflow.admin.entity.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账差异工单扩展（运营库 payflow_admin.recon_diff_assignment）。
 *
 * @author PayFlow Team
 */
@Data
@TableName("recon_diff_assignment")
public class ReconDiffAssignmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long diffId;
    private String merchantId;
    private String assigneeId;
    private String workflowStatus;
    private LocalDateTime assignedAt;
    private LocalDateTime dueAt;
    private LocalDateTime escalatedAt;
    private String escalatedToRole;
    private LocalDateTime lastRemindedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

