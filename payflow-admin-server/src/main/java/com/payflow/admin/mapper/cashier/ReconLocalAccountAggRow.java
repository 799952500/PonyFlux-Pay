package com.payflow.admin.mapper.cashier;

import lombok.Data;

/**
 * 本地收款按支付账号汇总行。
 *
 * @author PayFlow Team
 */
@Data
public class ReconLocalAccountAggRow {
    private String accountCode;
    private String payChannel;
    private Long cnt;
    private Long sumAmount;
}
