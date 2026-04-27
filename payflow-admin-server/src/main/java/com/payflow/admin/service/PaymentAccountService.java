package com.payflow.admin.service;

import com.payflow.admin.entity.PaymentAccount;

import java.util.List;
import java.util.Map;

public interface PaymentAccountService {
    List<PaymentAccount> listAll();

    PaymentAccount getById(Long id);

    PaymentAccount create(PaymentAccount account);

    PaymentAccount update(PaymentAccount account);

    void delete(Long id);

    List<PaymentAccount> listByChannelId(Long channelId);

    List<Map<String, Object>> channelRouteListWithDetails();
}

