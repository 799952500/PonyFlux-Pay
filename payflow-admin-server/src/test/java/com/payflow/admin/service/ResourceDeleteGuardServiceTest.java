package com.payflow.admin.service;

import com.payflow.admin.entity.ChannelRoute;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.exception.ResourceDependencyException;
import com.payflow.admin.mapper.ChannelRouteMapper;
import com.payflow.admin.mapper.FeeRateConfigMapper;
import com.payflow.admin.mapper.MerchantPaymentMethodMapper;
import com.payflow.admin.mapper.MerchantPaymentRouteMapper;
import com.payflow.admin.mapper.PaymentAccountMapper;
import com.payflow.admin.mapper.PaymentMethodMapper;
import com.payflow.admin.mapper.SysMenuMapper;
import com.payflow.admin.mapper.SysUserRoleMapper;
import com.payflow.admin.mapper.cashier.CashierChannelMerchantRouteMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.service.guard.CashierAccountResolver;
import com.payflow.admin.service.guard.ResourceDeleteCheckResult;
import com.payflow.admin.service.guard.ResourceDeleteGuardServiceImpl;
import com.payflow.admin.service.guard.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("删除前资源依赖检查")
class ResourceDeleteGuardServiceTest {

    @Mock
    private PaymentMethodMapper paymentMethodMapper;
    @Mock
    private PaymentAccountMapper paymentAccountMapper;
    @Mock
    private ChannelMapper channelMapper;
    @Mock
    private ChannelRouteMapper channelRouteMapper;
    @Mock
    private MerchantPaymentRouteMapper merchantPaymentRouteMapper;
    @Mock
    private MerchantPaymentMethodMapper merchantPaymentMethodMapper;
    @Mock
    private FeeRateConfigMapper feeRateConfigMapper;
    @Mock
    private SysMenuMapper sysMenuMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private CashierChannelMerchantRouteMapper cashierChannelMerchantRouteMapper;
    @Mock
    private ReconTaskEntityMapper reconTaskEntityMapper;
    @Mock
    private CashierAccountResolver cashierAccountResolver;

    @InjectMocks
    private ResourceDeleteGuardServiceImpl guard;

    @Test
    @DisplayName("支付方式被商户路由引用时阻断删除")
    void paymentMethodBlockedByMerchantRoute() {
        PaymentMethod method = new PaymentMethod();
        method.setId(10L);
        method.setMethodName("微信扫码");
        when(paymentMethodMapper.selectById(10L)).thenReturn(method);
        MerchantPaymentRoute route = new MerchantPaymentRoute();
        route.setId(1L);
        route.setMerchantId("M100001");
        route.setPaymentMethodId(10L);
        when(merchantPaymentRouteMapper.selectList(any())).thenReturn(List.of(route));
        when(merchantPaymentMethodMapper.selectList(any())).thenReturn(List.of());

        ResourceDeleteCheckResult result = guard.check(ResourceType.PAYMENT_METHOD, 10L);

        assertTrue(result.isBlocked());
        assertEquals(1, result.getRefs().size());
        assertThrows(ResourceDependencyException.class,
                () -> guard.assertDeletable(ResourceType.PAYMENT_METHOD, 10L));
    }

    @Test
    @DisplayName("支付账号仅被渠道路由引用时阻断删除")
    void paymentAccountBlockedByChannelRoute() {
        PaymentAccount account = new PaymentAccount();
        account.setId(20L);
        account.setAccountName("主账号");
        account.setAccountCode("ACC_001");
        account.setChannelId(1L);
        when(paymentAccountMapper.selectById(20L)).thenReturn(account);
        when(merchantPaymentRouteMapper.selectList(any())).thenReturn(List.of());
        ChannelRoute channelRoute = new ChannelRoute();
        channelRoute.setId(5L);
        channelRoute.setMerchantId("M100002");
        channelRoute.setPaymentAccountId(20L);
        when(channelRouteMapper.selectList(any())).thenReturn(List.of(channelRoute));
        when(cashierAccountResolver.resolveCashierAccountIds(account)).thenReturn(List.of());
        when(reconTaskEntityMapper.selectList(any())).thenReturn(List.of());

        ResourceDeleteCheckResult result = guard.check(ResourceType.PAYMENT_ACCOUNT, 20L);

        assertTrue(result.isBlocked());
        assertEquals("CHANNEL_ROUTE", result.getRefs().get(0).getRefType());
    }

    @Test
    @DisplayName("支付账号存在非终态对账任务时阻断删除")
    void paymentAccountBlockedByActiveReconTask() {
        PaymentAccount account = new PaymentAccount();
        account.setId(30L);
        account.setAccountName("对账账号");
        account.setAccountCode("ALIPAY_ACC_001");
        when(paymentAccountMapper.selectById(30L)).thenReturn(account);
        when(merchantPaymentRouteMapper.selectList(any())).thenReturn(List.of());
        when(channelRouteMapper.selectList(any())).thenReturn(List.of());
        when(cashierAccountResolver.resolveCashierAccountIds(account)).thenReturn(List.of());
        ReconTaskEntity task = new ReconTaskEntity();
        task.setTaskId("RECON-1");
        task.setAccountCode("ALIPAY_ACC_001");
        task.setStatus("PARSING");
        when(reconTaskEntityMapper.selectList(any())).thenReturn(List.of(task));

        ResourceDeleteCheckResult result = guard.check(ResourceType.PAYMENT_ACCOUNT, 30L);

        assertTrue(result.isBlocked());
        assertEquals("RECON_TASK", result.getRefs().get(0).getRefType());
    }

    @Test
    @DisplayName("支付方式无引用时可删除")
    void paymentMethodDeletableWhenNoRefs() {
        PaymentMethod method = new PaymentMethod();
        method.setId(11L);
        method.setMethodName("支付宝");
        when(paymentMethodMapper.selectById(11L)).thenReturn(method);
        when(merchantPaymentRouteMapper.selectList(any())).thenReturn(List.of());
        when(merchantPaymentMethodMapper.selectList(any())).thenReturn(List.of());

        ResourceDeleteCheckResult result = guard.check(ResourceType.PAYMENT_METHOD, 11L);

        assertFalse(result.isBlocked());
    }
}
