package com.payflow.admin.mapper.cashier;

import lombok.Data;

/**
 * 订单号与商户号。
 *
 * @author PayFlow Team
 */
@Data
public class OrderMerchantRow {
    private String orderId;
    private String merchantId;
}
