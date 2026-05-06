package com.payflow.recon.mapper.cashier;

import lombok.Data;

/**
 * 收银库渠道账户行（对账用）。
 *
 * @author PayFlow Team
 */
@Data
public class CashierReconAccountRow {
    private Long id;
    private String accountCode;
    private String channelConfig;
    /** cashier_channels.channel_code，如 alipay / wechat_pay */
    private String channelCode;
}
