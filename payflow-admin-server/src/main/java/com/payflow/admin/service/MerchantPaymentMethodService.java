package com.payflow.admin.service;

import com.payflow.admin.entity.MerchantPaymentMethod;
import java.util.List;

public interface MerchantPaymentMethodService {
    List<MerchantPaymentMethod> listByMerchantId(Long merchantId);
    List<MerchantPaymentMethod> listByPaymentMethodId(Long paymentMethodId);
    void create(MerchantPaymentMethod mpm);
    void update(Long id, MerchantPaymentMethod mpm);
    void delete(Long id);
    void deleteByMerchantAndMethod(Long merchantId, Long paymentMethodId);
}