package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.MerchantPaymentMethod;
import com.payflow.admin.mapper.MerchantPaymentMethodMapper;
import com.payflow.admin.service.MerchantPaymentMethodService;
import com.payflow.admin.service.guard.ResourceDeleteGuardService;
import com.payflow.admin.service.guard.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class MerchantPaymentMethodServiceImpl implements MerchantPaymentMethodService {

    private final MerchantPaymentMethodMapper mapper;
    private final ResourceDeleteGuardService resourceDeleteGuardService;

    @Override
    public List<MerchantPaymentMethod> listAll() {
        return mapper.selectList(null);
    }

    @Override
    public List<MerchantPaymentMethod> listAll(List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return listAll();
        }
        if (merchantScopeIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentMethod>()
                .in(MerchantPaymentMethod::getMerchantId, merchantScopeIds));
    }

    @Override
    public List<MerchantPaymentMethod> listByMerchantId(String merchantId) {
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentMethod>()
                .eq(MerchantPaymentMethod::getMerchantId, merchantId));
    }

    @Override
    public List<MerchantPaymentMethod> listByPaymentMethodId(Long paymentMethodId) {
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentMethod>()
                .eq(MerchantPaymentMethod::getPaymentMethodId, paymentMethodId));
    }

    @Override
    public void create(MerchantPaymentMethod mpm) {
        mapper.insert(mpm);
    }

    @Override
    public void update(Long id, MerchantPaymentMethod mpm) {
        mpm.setId(id);
        mapper.updateById(mpm);
    }

    @Override
    public void delete(Long id) {
        resourceDeleteGuardService.assertDeletable(ResourceType.MERCHANT_PAYMENT_METHOD, id);
        mapper.deleteById(id);
    }

    @Override
    public void deleteByMerchantAndMethod(String merchantId, Long paymentMethodId) {
        mapper.delete(new LambdaQueryWrapper<MerchantPaymentMethod>()
                .eq(MerchantPaymentMethod::getMerchantId, merchantId)
                .eq(MerchantPaymentMethod::getPaymentMethodId, paymentMethodId));
    }

    @Override
    public void deleteByMerchant(String merchantId) {
        mapper.delete(new LambdaQueryWrapper<MerchantPaymentMethod>()
                .eq(MerchantPaymentMethod::getMerchantId, merchantId));
    }

    @Override
    public MerchantPaymentMethod getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public MerchantPaymentMethod getById(Long id, List<String> merchantScopeIds) {
        MerchantPaymentMethod mpm = getById(id);
        if (mpm == null || merchantScopeIds == null) {
            return mpm;
        }
        if (merchantScopeIds.isEmpty()
                || mpm.getMerchantId() == null
                || !merchantScopeIds.contains(mpm.getMerchantId())) {
            return null;
        }
        return mpm;
    }
}