package com.payflow.admin.dto.recon;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 对账汇总接口响应。
 *
 * @author PayFlow Team
 */
@Data
@Builder
public class ReconSummaryResponse {
    private List<ReconAccountSummaryVO> byAccount;
    private Long totalLocalAmountFen;
    private Long totalChannelBillAmountFen;
    private Long totalAmountDeltaFen;
    private Long pendingDiffCount;
}
