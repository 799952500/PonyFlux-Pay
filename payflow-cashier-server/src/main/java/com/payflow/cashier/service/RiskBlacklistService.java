package com.payflow.cashier.service;

import com.payflow.cashier.dto.CreateOrderRequest;

/**
 * 风控黑名单校验。
 */
public interface RiskBlacklistService {

    /**
     * 命中黑名单则抛出业务异常。
     */
    void assertNotBlacklisted(CreateOrderRequest request);
}
