package com.payflow.cashier.service;

import com.payflow.cashier.dto.CreateOrderRequest;
/**
 * @author Lucas
 */

public interface RiskCheckService {
    void checkCreateOrder(CreateOrderRequest request);
}

