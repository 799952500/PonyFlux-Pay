package com.payflow.admin.dto.recon;

import lombok.Builder;
import lombok.Data;

/**
 * 单笔订单/支付维度的对账结果视图。
 *
 * @author PayFlow Team
 */
@Data
@Builder
public class ReconOrderResultVO {
    private String orderId;
    private String merchantId;
    private String paymentId;
    private String payChannel;
    private String channelTransactionId;
    private Long localAmountFen;
    /** MATCHED / ABNORMAL / NO_RECON */
    private String reconStatus;
    private String diffType;
    private String handleStatus;
    private Long diffId;
    private String taskId;
    private String reconChannel;
    private String accountCode;
    private Long channelAmountFen;
}
