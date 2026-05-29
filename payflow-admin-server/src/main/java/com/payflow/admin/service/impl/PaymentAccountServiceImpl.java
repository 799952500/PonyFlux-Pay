package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.payflow.admin.entity.ChannelRoute;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.mapper.ChannelRouteMapper;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.mapper.PaymentAccountMapper;
import com.payflow.admin.service.PaymentAccountService;
import com.payflow.admin.service.guard.ResourceDeleteGuardService;
import com.payflow.admin.service.guard.ResourceType;
import com.payflow.common.web.PageRequest;
import com.payflow.common.web.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class PaymentAccountServiceImpl implements PaymentAccountService {

    private final PaymentAccountMapper mapper;
    private final MerchantPaymentRouteMapper routeMapper;
    private final ChannelRouteMapper channelRouteMapper;
    private final ResourceDeleteGuardService resourceDeleteGuardService;

    @Override
    public List<PaymentAccount> listAll() {
        return mapper.listWithChannelName();
    }

    @Override
    public List<PaymentAccount> listAll(List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return listAll();
        }
        if (merchantScopeIds.isEmpty()) {
            return List.of();
        }
        Set<Long> accountIds = accountIdsForMerchantScope(merchantScopeIds);
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return listAll().stream()
                .filter(account -> account.getId() != null && accountIds.contains(account.getId()))
                .toList();
    }

    @Override
    public PageResult<PaymentAccount> page(PageRequest pageRequest, Long channelId, String keyword,
                                           List<String> merchantScopeIds) {
        Collection<Long> accountIds = null;
        if (merchantScopeIds != null) {
            if (merchantScopeIds.isEmpty()) {
                return PageResult.of(List.of(), 0, pageRequest);
            }
            Set<Long> ids = accountIdsForMerchantScope(merchantScopeIds);
            if (ids.isEmpty()) {
                return PageResult.of(List.of(), 0, pageRequest);
            }
            accountIds = ids;
        }
        String kw = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
        long total = mapper.countFiltered(channelId, kw, accountIds);
        List<PaymentAccount> list = mapper.pageWithChannelName(
                channelId, kw, accountIds, pageRequest.getOffset(), pageRequest.getSize());
        return PageResult.of(list, total, pageRequest);
    }

    @Override
    public PaymentAccount getById(Long id) {
        return mapper.getByIdWithChannelName(id);
    }

    @Override
    public PaymentAccount getById(Long id, List<String> merchantScopeIds) {
        if (!canAccessAccount(id, merchantScopeIds)) {
            return null;
        }
        return getById(id);
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
        resourceDeleteGuardService.assertDeletable(ResourceType.PAYMENT_ACCOUNT, id);
        mapper.deleteById(id);
    }

    private boolean canAccessAccount(Long id, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return true;
        }
        if (id == null || merchantScopeIds.isEmpty()) {
            return false;
        }
        return accountIdsForMerchantScope(merchantScopeIds).contains(id);
    }

    private Set<Long> accountIdsForMerchantScope(List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return Collections.emptySet();
        }
        Set<Long> accountIds = new HashSet<>();
        QueryWrapper<MerchantPaymentRoute> routeQw = new QueryWrapper<>();
        routeQw.in("merchant_id", merchantScopeIds);
        for (MerchantPaymentRoute route : routeMapper.selectList(routeQw)) {
            if (route.getPaymentAccountId() != null) {
                accountIds.add(route.getPaymentAccountId());
            }
        }
        QueryWrapper<ChannelRoute> channelQw = new QueryWrapper<>();
        channelQw.in("merchant_id", merchantScopeIds);
        for (ChannelRoute route : channelRouteMapper.selectList(channelQw)) {
            if (route.getPaymentAccountId() != null) {
                accountIds.add(route.getPaymentAccountId());
            }
        }
        return accountIds;
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

