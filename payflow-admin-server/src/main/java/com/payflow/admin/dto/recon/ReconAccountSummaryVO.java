package com.payflow.admin.dto.recon;

import lombok.Builder;
import lombok.Data;

/**
 * 按支付账号（收款账户）汇总的本地实收与渠道账单对比。
 *
 * @author PayFlow Team
 */
@Data
@Builder
public class ReconAccountSummaryVO {
    /** 渠道账户编码，与 recon_task.account_code 一致；历史无账号时为 __NO_ACCOUNT__ */
    private String accountCode;
    /** 对账渠道 alipay / wxpay */
    private String channel;
    private Long localSuccessCount;
    private Long localSuccessAmountFen;
    private Long channelBillCount;
    private Long channelBillAmountFen;
    private Long amountDeltaFen;
}
