package com.payflow.admin.service;

import com.payflow.admin.entity.PaymentAccount;
import com.payflow.common.web.PageRequest;
import com.payflow.common.web.PageResult;

import java.util.List;
import java.util.Map;
/**
 * @author Lucas
 */

public interface PaymentAccountService {
    List<PaymentAccount> listAll();

    List<PaymentAccount> listAll(List<String> merchantScopeIds);

    PageResult<PaymentAccount> page(PageRequest pageRequest, Long channelId, String keyword,
                                    List<String> merchantScopeIds);

    PaymentAccount getById(Long id);

    PaymentAccount getById(Long id, List<String> merchantScopeIds);

    PaymentAccount create(PaymentAccount account);

    PaymentAccount update(PaymentAccount account);

    void delete(Long id);

    List<PaymentAccount> listByChannelId(Long channelId);

    List<Map<String, Object>> channelRouteListWithDetails();
}

