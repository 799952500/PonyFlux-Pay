package com.payflow.cashier.service;

import com.payflow.cashier.dto.CreateOrderRequest;

public interface RiskCheckService {
    void checkCreateOrder(CreateOrderRequest request);
}

