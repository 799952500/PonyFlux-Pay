package com.payflow.admin.service;

import com.payflow.admin.entity.MerchantPaymentMethod;
import java.util.List;
/**
 * @author Lucas
 */

public interface MerchantPaymentMethodService {
    List<MerchantPaymentMethod> listAll();
    List<MerchantPaymentMethod> listAll(List<String> merchantScopeIds);
    List<MerchantPaymentMethod> listByMerchantId(String merchantId);
    List<MerchantPaymentMethod> listByPaymentMethodId(Long paymentMethodId);
    void create(MerchantPaymentMethod mpm);
    void update(Long id, MerchantPaymentMethod mpm);
    void delete(Long id);
    void deleteByMerchantAndMethod(String merchantId, Long paymentMethodId);
    void deleteByMerchant(String merchantId);
    MerchantPaymentMethod getById(Long id);
    MerchantPaymentMethod getById(Long id, List<String> merchantScopeIds);
}