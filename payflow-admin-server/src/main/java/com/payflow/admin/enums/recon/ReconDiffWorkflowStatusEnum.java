package com.payflow.admin.enums.recon;

import lombok.Getter;

/**
 * 对账差异工单状态枚举。
 */
@Getter
public enum ReconDiffWorkflowStatusEnum {
    UNASSIGNED,
    ASSIGNED,
    IN_PROGRESS,
    ESCALATED,
    PROCESSED,
    IGNORED,
    ACCEPTED_LOSS
}

