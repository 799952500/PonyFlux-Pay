package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.kit.ClientScopesKit;
import com.payflow.common.exception.BizException;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.service.MerchantCashierRouteSyncService;
import com.payflow.admin.service.MerchantPaymentRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class MerchantPaymentRouteServiceImpl implements MerchantPaymentRouteService {

    private final MerchantPaymentRouteMapper mapper;
    private final MerchantCashierRouteSyncService cashierRouteSyncService;

    @Override
    public List<MerchantPaymentRoute> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentRoute>()
                .orderByDesc(MerchantPaymentRoute::getPriority)
                .orderByAsc(MerchantPaymentRoute::getId));
    }

    @Override
    public List<MerchantPaymentRoute> listAll(List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return listAll();
        }
        if (merchantScopeIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentRoute>()
                .in(MerchantPaymentRoute::getMerchantId, merchantScopeIds)
                .orderByDesc(MerchantPaymentRoute::getPriority)
                .orderByAsc(MerchantPaymentRoute::getId));
    }

    @Override
    public List<MerchantPaymentRoute> listByMerchantId(String merchantId) {
        return listByMerchantId(merchantId, null);
    }

    @Override
    public List<MerchantPaymentRoute> listByMerchantId(String merchantId, List<String> merchantScopeIds) {
        if ("__NO_ACCESS__".equals(AdminRequestContext.resolveMerchantFilter(merchantId, merchantScopeIds))) {
            return List.of();
        }
        AdminRequestContext.assertMerchantAllowed(merchantId, merchantScopeIds);
        return mapper.selectList(new LambdaQueryWrapper<MerchantPaymentRoute>()
                .eq(MerchantPaymentRoute::getMerchantId, merchantId)
                .orderByDesc(MerchantPaymentRoute::getPriority)
                .orderByAsc(MerchantPaymentRoute::getId));
    }

    @Override
    public MerchantPaymentRoute getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public MerchantPaymentRoute getById(Long id, List<String> merchantScopeIds) {
        MerchantPaymentRoute route = getById(id);
        if (route == null || merchantScopeIds == null) {
            return route;
        }
        if (merchantScopeIds.isEmpty()
                || route.getMerchantId() == null
                || !merchantScopeIds.contains(route.getMerchantId())) {
            return null;
        }
        return route;
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void createRoute(MerchantPaymentRoute route) {
        if (route.getMerchantId() == null || route.getMerchantId().isBlank()) {
            throw new IllegalArgumentException("商户号不能为空");
        }
        if (route.getPaymentMethodId() == null || route.getPaymentAccountId() == null) {
            throw new IllegalArgumentException("支付方式与支付账号不能为空");
        }
        if (route.getEnabled() == null) {
            route.setEnabled(true);
        }
        if (route.getPriority() == null) {
            route.setPriority(0);
        }
        if (route.getClientScopes() == null || route.getClientScopes().isBlank()) {
            route.setClientScopes(ClientScopesKit.DEFAULT_DB_VALUE);
        }
        route.setId(null);
        mapper.insert(route);
        scheduleCashierSync(route.getMerchantId());
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void updateRoute(Long id, MerchantPaymentRoute patch) {
        updateRoute(id, patch, null);
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void updateRoute(Long id, MerchantPaymentRoute patch, List<String> merchantScopeIds) {
        MerchantPaymentRoute exist = requireAccessibleRoute(id, merchantScopeIds);
        if (patch.getPaymentMethodId() != null) {
            exist.setPaymentMethodId(patch.getPaymentMethodId());
        }
        if (patch.getPaymentAccountId() != null) {
            exist.setPaymentAccountId(patch.getPaymentAccountId());
        }
        if (patch.getEnabled() != null) {
            exist.setEnabled(patch.getEnabled());
        }
        if (patch.getPriority() != null) {
            exist.setPriority(patch.getPriority());
        }
        if (patch.getClientScopes() != null && !patch.getClientScopes().isBlank()) {
            exist.setClientScopes(patch.getClientScopes());
        }
        mapper.updateById(exist);
        scheduleCashierSync(exist.getMerchantId());
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void deleteRoute(Long id) {
        deleteRoute(id, null);
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void deleteRoute(Long id, List<String> merchantScopeIds) {
        if (id == null) {
            throw new IllegalArgumentException("路由ID不能为空");
        }
        MerchantPaymentRoute exist = requireAccessibleRoute(id, merchantScopeIds);
        String merchantId = exist.getMerchantId();
        mapper.deleteById(id);
        scheduleCashierSync(merchantId);
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void toggleRoute(Long id) {
        toggleRoute(id, null);
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void toggleRoute(Long id, List<String> merchantScopeIds) {
        MerchantPaymentRoute exist = requireAccessibleRoute(id, merchantScopeIds);
        exist.setEnabled(Boolean.FALSE.equals(exist.getEnabled()));
        mapper.updateById(exist);
        scheduleCashierSync(exist.getMerchantId());
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void replaceRoutes(String merchantId, List<MerchantPaymentRoute> routes) {
        replaceRoutes(merchantId, routes, null);
    }

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public void replaceRoutes(String merchantId, List<MerchantPaymentRoute> routes, List<String> merchantScopeIds) {
        AdminRequestContext.assertMerchantAllowed(merchantId, merchantScopeIds);
        mapper.delete(new LambdaQueryWrapper<MerchantPaymentRoute>()
                .eq(MerchantPaymentRoute::getMerchantId, merchantId));
        if (routes != null && !routes.isEmpty()) {
            for (MerchantPaymentRoute route : routes) {
                route.setId(null);
                route.setMerchantId(merchantId);
                if (route.getClientScopes() == null || route.getClientScopes().isBlank()) {
                    route.setClientScopes(ClientScopesKit.DEFAULT_DB_VALUE);
                }
                mapper.insert(route);
            }
        }
        scheduleCashierSync(merchantId);
    }

    private MerchantPaymentRoute requireAccessibleRoute(Long id, List<String> merchantScopeIds) {
        MerchantPaymentRoute exist = getById(id, merchantScopeIds);
        if (exist == null) {
            if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
                throw new BizException(6101, "无权访问该资源");
            }
            throw new IllegalArgumentException("路由不存在: " + id);
        }
        return exist;
    }

    private void scheduleCashierSync(String merchantId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cashierRouteSyncService.syncAndNotify(merchantId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cashierRouteSyncService.syncAndNotify(merchantId);
            }
        });
    }
}
