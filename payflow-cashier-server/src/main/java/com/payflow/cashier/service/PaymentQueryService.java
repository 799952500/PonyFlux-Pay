package com.payflow.cashier.service;

import com.payflow.cashier.dto.PaymentChannelQueryResult;

/**
 * 支付机构查单与本地状态同步（运维排障）。
 */
public interface PaymentQueryService {

    /**
     * 向支付机构查询支付结果；{@code sync} 为 true 且渠道已支付、本地仍为处理中时回写成功状态。
     *
     * @param paymentId 支付记录 ID
     * @param sync      是否将渠道成功结果同步到本地
     */
    PaymentChannelQueryResult queryChannelAndOptionalSync(String paymentId, boolean sync);
}
