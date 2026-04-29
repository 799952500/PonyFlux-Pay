package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.mapper.PaymentAccountMapper;
import com.payflow.admin.service.PaymentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class PaymentAccountServiceImpl implements PaymentAccountService {

    private final PaymentAccountMapper mapper;
    private final MerchantPaymentRouteMapper routeMapper;

    @Override
    public List<PaymentAccount> listAll() {
        return mapper.listWithChannelName();
    }

    @Override
    public PaymentAccount getById(Long id) {
        PaymentAccount account = mapper.selectById(id);
        if (account != null) {
            // Load channelName via the same join query approach
            List<PaymentAccount> list = mapper.listWithChannelName();
            for (PaymentAccount pa : list) {
                if (pa.getId().equals(id)) {
                    account.setChannelName(pa.getChannelName());
                    break;
                }
            }
        }
        return account;
    }

    @Override
    public PaymentAccount create(PaymentAccount account) {
        mapper.insert(account);
        return account;
    }

    @Override
    public PaymentAccount update(PaymentAccount account) {
        mapper.updateById(account);
        return mapper.selectById(account.getId());
    }

    @Override
    public void delete(Long id) {
        // Check if any merchant payment routes reference this account
        QueryWrapper<MerchantPaymentRoute> qw = new QueryWrapper<>();
        qw.eq("payment_account_id", id);
        Long count = routeMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new IllegalStateException("该支付账号已关联商户支付路由，无法删除");
        }
        mapper.deleteById(id);
    }

    @Override
    public List<PaymentAccount> listByChannelId(Long channelId) {
        QueryWrapper<PaymentAccount> qw = new QueryWrapper<>();
        qw.eq("channel_id", channelId);
        return mapper.selectList(qw);
    }

    @Override
    public List<Map<String, Object>> channelRouteListWithDetails() {
        return mapper.channelRouteListWithDetails();
    }
}

