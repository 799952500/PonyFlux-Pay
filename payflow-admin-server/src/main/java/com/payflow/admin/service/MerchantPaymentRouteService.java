package com.payflow.admin.service;

import com.payflow.admin.entity.MerchantPaymentRoute;

import java.util.List;

public interface MerchantPaymentRouteService {
    List<MerchantPaymentRoute> listAll();
    List<MerchantPaymentRoute> listByMerchantId(String merchantId);

    void replaceRoutes(String merchantId, List<MerchantPaymentRoute> routes);
}

