package com.payflow.admin.service;

import com.payflow.admin.entity.PaymentMethod;

import java.util.List;
import java.util.Map;
/**
 * @author Lucas
 */

public interface PaymentMethodService {
    Map<String, Object> page(int page, int pageSize, Long channelId, String channelType, String keyword, String status);

    List<PaymentMethod> listAll();
    List<PaymentMethod> listByChannelId(Long channelId);
    PaymentMethod getById(Long id);
    void create(PaymentMethod method);
    void update(Long id, PaymentMethod method);
    void delete(Long id);
}