package com.payflow.admin.dto.recon;

import lombok.Data;

import java.time.LocalDate;

/**
 * 异常对账明细（分页行，含任务维度）。
 *
 * @author PayFlow Team
 */
@Data
public class ReconAbnormalPageRow {
    private Long diffId;
    private String taskId;
    private String diffType;
    private String channelTradeNo;
    private String localOrderId;
    private Long channelAmount;
    private Long localAmount;
    private String handleStatus;
    private String suggestedAction;
    private String reconChannel;
    private String accountCode;
    private LocalDate billDate;
}
