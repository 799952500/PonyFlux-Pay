package com.payflow.admin.service;

import com.payflow.admin.entity.MerchantPaymentRoute;

import java.util.List;
/**
 * @author Lucas
 */

public interface MerchantPaymentRouteService {
    List<MerchantPaymentRoute> listAll();

    List<MerchantPaymentRoute> listAll(List<String> merchantScopeIds);

    List<MerchantPaymentRoute> listByMerchantId(String merchantId);

    List<MerchantPaymentRoute> listByMerchantId(String merchantId, List<String> merchantScopeIds);

    MerchantPaymentRoute getById(Long id);

    MerchantPaymentRoute getById(Long id, List<String> merchantScopeIds);

    /**
     * 新增一条路由（含商户号）。
     */
    void createRoute(MerchantPaymentRoute route);

    void updateRoute(Long id, MerchantPaymentRoute patch);

    void updateRoute(Long id, MerchantPaymentRoute patch, List<String> merchantScopeIds);

    void deleteRoute(Long id);

    void deleteRoute(Long id, List<String> merchantScopeIds);

    void toggleRoute(Long id);

    void toggleRoute(Long id, List<String> merchantScopeIds);

    void replaceRoutes(String merchantId, List<MerchantPaymentRoute> routes);

    void replaceRoutes(String merchantId, List<MerchantPaymentRoute> routes, List<String> merchantScopeIds);
}

