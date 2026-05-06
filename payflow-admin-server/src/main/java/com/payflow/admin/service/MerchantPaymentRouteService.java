package com.payflow.admin.service;

import com.payflow.admin.entity.MerchantPaymentRoute;

import java.util.List;
/**
 * @author Lucas
 */

public interface MerchantPaymentRouteService {
    List<MerchantPaymentRoute> listAll();

    List<MerchantPaymentRoute> listByMerchantId(String merchantId);

    MerchantPaymentRoute getById(Long id);

    /**
     * 新增一条路由（含商户号）。
     */
    void createRoute(MerchantPaymentRoute route);

    void updateRoute(Long id, MerchantPaymentRoute patch);

    void deleteRoute(Long id);

    void toggleRoute(Long id);

    void replaceRoutes(String merchantId, List<MerchantPaymentRoute> routes);
}

