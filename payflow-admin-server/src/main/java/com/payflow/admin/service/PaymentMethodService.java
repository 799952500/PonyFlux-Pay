package com.payflow.admin.service;

import com.payflow.admin.entity.PaymentMethod;
import java.util.List;
/**
 * @author Lucas
 */

public interface PaymentMethodService {
    List<PaymentMethod> listAll();
    List<PaymentMethod> listByChannelId(Long channelId);
    PaymentMethod getById(Long id);
    void create(PaymentMethod method);
    void update(Long id, PaymentMethod method);
    void delete(Long id);
}