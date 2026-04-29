package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.service.MerchantPaymentRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class MerchantPaymentRouteServiceImpl implements MerchantPaymentRouteService {

    private final MerchantPaymentRouteMapper mapper;

    @Override
    public List<MerchantPaymentRoute> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentRoute>()
                .orderByDesc(MerchantPaymentRoute::getPriority)
                .orderByAsc(MerchantPaymentRoute::getId));
    }

    @Override
    public List<MerchantPaymentRoute> listByMerchantId(String merchantId) {
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentRoute>()
                .eq(MerchantPaymentRoute::getMerchantId, merchantId)
                .orderByDesc(MerchantPaymentRoute::getPriority)
                .orderByAsc(MerchantPaymentRoute::getId));
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void replaceRoutes(String merchantId, List<MerchantPaymentRoute> routes) {
        mapper.delete(new LambdaQueryWrapper<MerchantPaymentRoute>()
                .eq(MerchantPaymentRoute::getMerchantId, merchantId));
        if (routes == null || routes.isEmpty()) {
            return;
        }
        for (MerchantPaymentRoute route : routes) {
            route.setId(null);
            route.setMerchantId(merchantId);
            mapper.insert(route);
        }
    }
}

