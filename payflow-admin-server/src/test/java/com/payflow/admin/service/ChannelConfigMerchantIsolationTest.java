package com.payflow.admin.service;

import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.service.impl.MerchantPaymentRouteServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("渠道账号与路由商户隔离测试")
class ChannelConfigMerchantIsolationTest {

    @Mock
    private MerchantPaymentRouteMapper merchantPaymentRouteMapper;

    @Mock
    private MerchantCashierRouteSyncService cashierRouteSyncService;

    @Test
    @DisplayName("按商户查询路由仅返回目标商户配置")
    void listByMerchantReturnsOnlyRequestedMerchantRoutes() {
        MerchantPaymentRouteServiceImpl service = new MerchantPaymentRouteServiceImpl(
                merchantPaymentRouteMapper, cashierRouteSyncService);
        when(merchantPaymentRouteMapper.selectList(any())).thenReturn(List.of(route(1L, "M100001")));

        List<MerchantPaymentRoute> result = service.listByMerchantId("M100001");

        assertEquals(1, result.size());
        assertEquals("M100001", result.get(0).getMerchantId());
        verify(merchantPaymentRouteMapper).selectList(any());
    }

    @Test
    @DisplayName("创建路由必须绑定商户号")
    void createRouteRequiresMerchantId() {
        MerchantPaymentRouteServiceImpl service = new MerchantPaymentRouteServiceImpl(
                merchantPaymentRouteMapper, cashierRouteSyncService);
        MerchantPaymentRoute route = route(null, "");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createRoute(route));

        assertEquals("商户号不能为空", ex.getMessage());
        verify(merchantPaymentRouteMapper, never()).insert(org.mockito.ArgumentMatchers.<MerchantPaymentRoute>any());
    }

    @Test
    @DisplayName("创建路由后只同步当前商户路由")
    void createRouteSyncsCurrentMerchantOnly() {
        MerchantPaymentRouteServiceImpl service = new MerchantPaymentRouteServiceImpl(
                merchantPaymentRouteMapper, cashierRouteSyncService);
        MerchantPaymentRoute route = route(null, "M100001");
        route.setPaymentMethodId(11L);
        route.setPaymentAccountId(22L);

        service.createRoute(route);

        verify(merchantPaymentRouteMapper).insert(route);
        verify(cashierRouteSyncService).syncAndNotify("M100001");
    }

    @Test
    @DisplayName("删除路由时按路由归属商户同步")
    void deleteRouteSyncsExistingRouteMerchant() {
        MerchantPaymentRouteServiceImpl service = new MerchantPaymentRouteServiceImpl(
                merchantPaymentRouteMapper, cashierRouteSyncService);
        when(merchantPaymentRouteMapper.selectById(9L)).thenReturn(route(9L, "M100002"));

        service.deleteRoute(9L);

        verify(merchantPaymentRouteMapper).deleteById(9L);
        verify(cashierRouteSyncService).syncAndNotify("M100002");
    }

    @Test
    @DisplayName("替换路由时强制使用路径商户号覆盖每条路由归属")
    void replaceRoutesForcesMerchantIdFromRequest() {
        MerchantPaymentRouteServiceImpl service = new MerchantPaymentRouteServiceImpl(
                merchantPaymentRouteMapper, cashierRouteSyncService);
        MerchantPaymentRoute route = route(null, "M100002");
        route.setPaymentMethodId(11L);
        route.setPaymentAccountId(22L);

        service.replaceRoutes("M100001", List.of(route));

        assertEquals("M100001", route.getMerchantId());
        verify(merchantPaymentRouteMapper).delete(any());
        verify(merchantPaymentRouteMapper).insert(route);
        verify(cashierRouteSyncService).syncAndNotify("M100001");
    }

    private static MerchantPaymentRoute route(Long id, String merchantId) {
        MerchantPaymentRoute route = new MerchantPaymentRoute();
        route.setId(id);
        route.setMerchantId(merchantId);
        route.setPaymentMethodId(1L);
        route.setPaymentAccountId(2L);
        route.setEnabled(true);
        route.setPriority(10);
        return route;
    }
}
