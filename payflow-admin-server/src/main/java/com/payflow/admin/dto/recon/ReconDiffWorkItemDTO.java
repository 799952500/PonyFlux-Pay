package com.payflow.admin.dto.recon;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 差异工单列表行 DTO。
 *
 * @author PayFlow Team
 */
@Data
public class ReconDiffWorkItemDTO {
    private Long diffId;
    private String taskId;
    private String merchantId;
    private String diffType;
    private String handleStatus;
    private String workflowStatus;
    private String assigneeId;
    private LocalDateTime dueAt;
    private LocalDateTime escalatedAt;
    private LocalDateTime createdAt;
    private Long channelAmount;
    private Long localAmount;
}

