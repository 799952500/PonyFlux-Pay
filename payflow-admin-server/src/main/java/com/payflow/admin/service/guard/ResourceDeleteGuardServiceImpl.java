package com.payflow.admin.service.guard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.Channel;
import com.payflow.admin.entity.ChannelRoute;
import com.payflow.admin.entity.FeeRateConfig;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.entity.MerchantPaymentMethod;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.entity.SysUserRole;
import com.payflow.admin.entity.cashier.CashierChannelMerchantRoute;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.exception.ResourceDependencyException;
import com.payflow.admin.mapper.ChannelMapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 删除前跨表/跨库依赖检查实现。
 */
@Service
@RequiredArgsConstructor
public class ResourceDeleteGuardServiceImpl implements ResourceDeleteGuardService {

    private static final Set<String> TERMINAL_RECON_STATUSES = Set.of("SUCCESS", "FAIL");

    private final PaymentMethodMapper paymentMethodMapper;
    private final PaymentAccountMapper paymentAccountMapper;
    private final ChannelMapper channelMapper;
    private final ChannelRouteMapper channelRouteMapper;
    private final MerchantPaymentRouteMapper merchantPaymentRouteMapper;
    private final MerchantPaymentMethodMapper merchantPaymentMethodMapper;
    private final FeeRateConfigMapper feeRateConfigMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final CashierChannelMerchantRouteMapper cashierChannelMerchantRouteMapper;
    private final ReconTaskEntityMapper reconTaskEntityMapper;
    private final CashierAccountResolver cashierAccountResolver;

    @Override
    public ResourceDeleteCheckResult check(ResourceType type, Object resourceId) {
        return check(type, resourceId, null);
    }

    @Override
    public ResourceDeleteCheckResult check(ResourceType type, Object resourceId, List<String> merchantScopeIds) {
        if (type == null || resourceId == null) {
            return ResourceDeleteCheckResult.blocked("资源类型或 ID 无效", List.of());
        }
        List<String> scopeForGuard = usesGlobalDependencyScope(type) ? null : merchantScopeIds;
        ResourceDeleteCheckResult raw = switch (type) {
            case CHANNEL -> checkChannel(toLong(resourceId), scopeForGuard);
            case PAYMENT_METHOD -> checkPaymentMethod(toLong(resourceId), scopeForGuard);
            case PAYMENT_ACCOUNT -> checkPaymentAccount(toLong(resourceId), scopeForGuard);
            case MERCHANT_PAYMENT_METHOD -> checkMerchantPaymentMethod(toLong(resourceId), scopeForGuard);
            case MERCHANT -> checkMerchant(resourceId, scopeForGuard);
            case SYS_MENU -> checkSysMenu(toLong(resourceId));
            case SYS_ROLE -> checkSysRole(toLong(resourceId));
            case FEE_RATE, SYSTEM_CONFIG -> ResourceDeleteCheckResult.ok();
        };
        return maskRefsForMerchantScope(type, raw, merchantScopeIds);
    }

    @Override
    public void assertDeletable(ResourceType type, Object resourceId) {
        assertDeletable(type, resourceId, null);
    }

    @Override
    public void assertDeletable(ResourceType type, Object resourceId, List<String> merchantScopeIds) {
        ResourceDeleteCheckResult result = check(type, resourceId, merchantScopeIds);
        if (result.isBlocked()) {
            throw new ResourceDependencyException(result);
        }
    }

    private static boolean usesGlobalDependencyScope(ResourceType type) {
        return type == ResourceType.CHANNEL
                || type == ResourceType.PAYMENT_METHOD
                || type == ResourceType.PAYMENT_ACCOUNT;
    }

    /**
     * 全局资源配置：阻断判断看全部引用；返回给商户管理员的引用列表可按授权范围过滤。
     */
    private ResourceDeleteCheckResult maskRefsForMerchantScope(
            ResourceType type,
            ResourceDeleteCheckResult raw,
            List<String> merchantScopeIds) {
        if (!raw.isBlocked() || merchantScopeIds == null || !usesGlobalDependencyScope(type)) {
            return raw;
        }
        List<ResourceRefDTO> visible = raw.getRefs().stream()
                .filter(ref -> inMerchantScope(ref.getMerchantId(), merchantScopeIds)
                        || !StringUtils.hasText(ref.getMerchantId()))
                .collect(Collectors.toList());
        if (!visible.isEmpty()) {
            return ResourceDeleteCheckResult.blocked(raw.getSummary(), visible);
        }
        return ResourceDeleteCheckResult.blocked(
                "该资源仍被其他商户配置引用，请先联系平台管理员或解除全部关联后再删除",
                List.of());
    }

    private ResourceDeleteCheckResult checkPaymentMethod(Long methodId, List<String> merchantScopeIds) {
        if (methodId == null) {
            return ResourceDeleteCheckResult.blocked("支付方式不存在", List.of());
        }
        PaymentMethod method = paymentMethodMapper.selectById(methodId);
        if (method == null) {
            return ResourceDeleteCheckResult.blocked("支付方式不存在", List.of());
        }
        List<ResourceRefDTO> refs = new ArrayList<>();
        for (MerchantPaymentRoute route : merchantPaymentRouteMapper.selectList(
                new LambdaQueryWrapper<MerchantPaymentRoute>().eq(MerchantPaymentRoute::getPaymentMethodId, methodId))) {
            if (!inMerchantScope(route.getMerchantId(), merchantScopeIds)) {
                continue;
            }
            refs.add(ResourceRefDTO.builder()
                    .refType("MERCHANT_PAYMENT_ROUTE")
                    .refId(String.valueOf(route.getId()))
                    .merchantId(route.getMerchantId())
                    .label(String.format("商户支付路由 #%d（商户 %s）", route.getId(), route.getMerchantId()))
                    .resolveHint("/admin/merchants")
                    .build());
        }
        for (MerchantPaymentMethod binding : merchantPaymentMethodMapper.selectList(
                new LambdaQueryWrapper<MerchantPaymentMethod>().eq(MerchantPaymentMethod::getPaymentMethodId, methodId))) {
            if (!inMerchantScope(binding.getMerchantId(), merchantScopeIds)) {
                continue;
            }
            refs.add(ResourceRefDTO.builder()
                    .refType("MERCHANT_PAYMENT_METHOD")
                    .refId(String.valueOf(binding.getId()))
                    .merchantId(binding.getMerchantId())
                    .label(String.format("商户支付方式绑定 #%d（商户 %s）", binding.getId(), binding.getMerchantId()))
                    .resolveHint("/admin/merchants")
                    .build());
        }
        if (refs.isEmpty()) {
            return ResourceDeleteCheckResult.ok();
        }
        return ResourceDeleteCheckResult.blocked(
                String.format("支付方式「%s」仍被 %d 处配置引用，请先解除关联", method.getMethodName(), refs.size()),
                refs);
    }

    private ResourceDeleteCheckResult checkPaymentAccount(Long accountId, List<String> merchantScopeIds) {
        if (accountId == null) {
            return ResourceDeleteCheckResult.blocked("支付账号不存在", List.of());
        }
        PaymentAccount account = paymentAccountMapper.selectById(accountId);
        if (account == null) {
            return ResourceDeleteCheckResult.blocked("支付账号不存在", List.of());
        }
        List<ResourceRefDTO> refs = new ArrayList<>();
        collectMerchantPaymentRouteRefs(accountId, merchantScopeIds, refs);
        collectChannelRouteRefs(accountId, merchantScopeIds, refs);
        collectCashierRouteRefs(account, merchantScopeIds, refs);
        collectReconTaskRefs(account, refs);
        if (refs.isEmpty()) {
            return ResourceDeleteCheckResult.ok();
        }
        return ResourceDeleteCheckResult.blocked(
                String.format("支付账号「%s」仍被 %d 处配置引用，请先解除关联", account.getAccountName(), refs.size()),
                refs);
    }

    private void collectMerchantPaymentRouteRefs(Long accountId, List<String> merchantScopeIds, List<ResourceRefDTO> refs) {
        for (MerchantPaymentRoute route : merchantPaymentRouteMapper.selectList(
                new LambdaQueryWrapper<MerchantPaymentRoute>().eq(MerchantPaymentRoute::getPaymentAccountId, accountId))) {
            if (!inMerchantScope(route.getMerchantId(), merchantScopeIds)) {
                continue;
            }
            refs.add(ResourceRefDTO.builder()
                    .refType("MERCHANT_PAYMENT_ROUTE")
                    .refId(String.valueOf(route.getId()))
                    .merchantId(route.getMerchantId())
                    .label(String.format("商户支付路由 #%d（商户 %s）", route.getId(), route.getMerchantId()))
                    .resolveHint("/admin/merchants")
                    .build());
        }
    }

    private void collectChannelRouteRefs(Long accountId, List<String> merchantScopeIds, List<ResourceRefDTO> refs) {
        for (ChannelRoute route : channelRouteMapper.selectList(
                new LambdaQueryWrapper<ChannelRoute>().eq(ChannelRoute::getPaymentAccountId, accountId))) {
            if (!inMerchantScope(route.getMerchantId(), merchantScopeIds)) {
                continue;
            }
            refs.add(ResourceRefDTO.builder()
                    .refType("CHANNEL_ROUTE")
                    .refId(String.valueOf(route.getId()))
                    .merchantId(route.getMerchantId())
                    .label(String.format("渠道路由 #%d（商户 %s）", route.getId(), route.getMerchantId()))
                    .resolveHint("/admin/channel-routes")
                    .build());
        }
    }

    private void collectCashierRouteRefs(PaymentAccount account, List<String> merchantScopeIds, List<ResourceRefDTO> refs) {
        List<Long> cashierAccountIds = cashierAccountResolver.resolveCashierAccountIds(account);
        if (cashierAccountIds.isEmpty()) {
            return;
        }
        for (CashierChannelMerchantRoute route : cashierChannelMerchantRouteMapper.selectList(
                new LambdaQueryWrapper<CashierChannelMerchantRoute>()
                        .in(CashierChannelMerchantRoute::getChannelAccountId, cashierAccountIds))) {
            if (!inMerchantScope(route.getMerchantId(), merchantScopeIds)) {
                continue;
            }
            refs.add(ResourceRefDTO.builder()
                    .refType("CASHIER_MERCHANT_ROUTE")
                    .refId(String.valueOf(route.getId()))
                    .merchantId(route.getMerchantId())
                    .label(String.format("收银台商户路由 #%d（商户 %s）", route.getId(), route.getMerchantId()))
                    .resolveHint("/admin/merchants")
                    .build());
        }
    }

    private void collectReconTaskRefs(PaymentAccount account, List<ResourceRefDTO> refs) {
        if (!StringUtils.hasText(account.getAccountCode())) {
            return;
        }
        List<ReconTaskEntity> tasks = reconTaskEntityMapper.selectList(
                new LambdaQueryWrapper<ReconTaskEntity>()
                        .eq(ReconTaskEntity::getAccountCode, account.getAccountCode()));
        for (ReconTaskEntity task : tasks) {
            String status = task.getStatus() == null ? "" : task.getStatus().toUpperCase();
            if (TERMINAL_RECON_STATUSES.contains(status)) {
                continue;
            }
            refs.add(ResourceRefDTO.builder()
                    .refType("RECON_TASK")
                    .refId(task.getTaskId() != null ? task.getTaskId() : String.valueOf(task.getId()))
                    .merchantId(task.getMerchantId())
                    .label(String.format("对账任务 %s（状态 %s）", task.getTaskId(), task.getStatus()))
                    .resolveHint("/admin/reconcile/results")
                    .build());
        }
    }

    private ResourceDeleteCheckResult checkChannel(Long channelId, List<String> merchantScopeIds) {
        if (channelId == null) {
            return ResourceDeleteCheckResult.blocked("渠道不存在", List.of());
        }
        Channel channel = channelMapper.selectById(channelId);
        if (channel == null) {
            return ResourceDeleteCheckResult.blocked("渠道不存在", List.of());
        }
        List<ResourceRefDTO> refs = new ArrayList<>();
        long methodCount = paymentMethodMapper.selectCount(
                new LambdaQueryWrapper<PaymentMethod>().eq(PaymentMethod::getChannelId, channelId));
        if (methodCount > 0) {
            refs.add(ResourceRefDTO.builder()
                    .refType("PAYMENT_METHOD")
                    .refId(String.valueOf(channelId))
                    .label(String.format("下属支付方式 %d 条", methodCount))
                    .resolveHint("/admin/payment-methods?channelId=" + channelId)
                    .build());
        }
        long accountCount = paymentAccountMapper.selectCount(
                new LambdaQueryWrapper<PaymentAccount>().eq(PaymentAccount::getChannelId, channelId));
        if (accountCount > 0) {
            refs.add(ResourceRefDTO.builder()
                    .refType("PAYMENT_ACCOUNT")
                    .refId(String.valueOf(channelId))
                    .label(String.format("下属支付账号 %d 条", accountCount))
                    .resolveHint("/admin/payment-accounts")
                    .build());
        }
        for (ChannelRoute route : channelRouteMapper.selectList(
                new LambdaQueryWrapper<ChannelRoute>().eq(ChannelRoute::getChannelId, channelId))) {
            if (!inMerchantScope(route.getMerchantId(), merchantScopeIds)) {
                continue;
            }
            refs.add(ResourceRefDTO.builder()
                    .refType("CHANNEL_ROUTE")
                    .refId(String.valueOf(route.getId()))
                    .merchantId(route.getMerchantId())
                    .label(String.format("渠道路由 #%d（商户 %s）", route.getId(), route.getMerchantId()))
                    .resolveHint("/admin/channel-routes")
                    .build());
        }
        if (StringUtils.hasText(channel.getChannelCode())) {
            String code = channel.getChannelCode().trim();
            List<FeeRateConfig> feeRules = feeRateConfigMapper.selectList(
                    new LambdaQueryWrapper<FeeRateConfig>().eq(FeeRateConfig::getChannelCode, code));
            if (!feeRules.isEmpty()) {
                refs.add(ResourceRefDTO.builder()
                        .refType("FEE_RATE_CONFIG")
                        .refId(code)
                        .label(String.format("费率规则 %d 条引用渠道 %s", feeRules.size(), code))
                        .resolveHint("/admin/fee-rates")
                        .build());
            }
        }
        if (refs.isEmpty()) {
            return ResourceDeleteCheckResult.ok();
        }
        return ResourceDeleteCheckResult.blocked(
                String.format("渠道「%s」仍被 %d 处配置引用，请先解除关联", channel.getChannelName(), refs.size()),
                refs);
    }

    private ResourceDeleteCheckResult checkMerchantPaymentMethod(Long bindingId, List<String> merchantScopeIds) {
        if (bindingId == null) {
            return ResourceDeleteCheckResult.blocked("商户支付方式绑定不存在", List.of());
        }
        MerchantPaymentMethod binding = merchantPaymentMethodMapper.selectById(bindingId);
        if (binding == null) {
            return ResourceDeleteCheckResult.blocked("商户支付方式绑定不存在", List.of());
        }
        if (!inMerchantScope(binding.getMerchantId(), merchantScopeIds)) {
            return ResourceDeleteCheckResult.ok();
        }
        List<ResourceRefDTO> refs = new ArrayList<>();
        if (binding.getPaymentMethodId() != null && StringUtils.hasText(binding.getMerchantId())) {
            for (MerchantPaymentRoute route : merchantPaymentRouteMapper.selectList(
                    new LambdaQueryWrapper<MerchantPaymentRoute>()
                            .eq(MerchantPaymentRoute::getMerchantId, binding.getMerchantId())
                            .eq(MerchantPaymentRoute::getPaymentMethodId, binding.getPaymentMethodId()))) {
                refs.add(ResourceRefDTO.builder()
                        .refType("MERCHANT_PAYMENT_ROUTE")
                        .refId(String.valueOf(route.getId()))
                        .merchantId(route.getMerchantId())
                        .label(String.format("商户支付路由 #%d", route.getId()))
                        .resolveHint("/admin/merchants")
                        .build());
            }
        }
        if (refs.isEmpty()) {
            return ResourceDeleteCheckResult.ok();
        }
        return ResourceDeleteCheckResult.blocked(
                String.format("商户 %s 的支付方式绑定仍被 %d 条路由引用", binding.getMerchantId(), refs.size()),
                refs);
    }

    private ResourceDeleteCheckResult checkMerchant(Object resourceId, List<String> merchantScopeIds) {
        String merchantId = resourceId instanceof String ? (String) resourceId : String.valueOf(resourceId);
        if (!StringUtils.hasText(merchantId)) {
            return ResourceDeleteCheckResult.blocked("商户号无效", List.of());
        }
        if (!inMerchantScope(merchantId, merchantScopeIds)) {
            return ResourceDeleteCheckResult.ok();
        }
        List<ResourceRefDTO> refs = new ArrayList<>();
        for (MerchantPaymentRoute route : merchantPaymentRouteMapper.selectList(
                new LambdaQueryWrapper<MerchantPaymentRoute>()
                        .eq(MerchantPaymentRoute::getMerchantId, merchantId)
                        .eq(MerchantPaymentRoute::getEnabled, true))) {
            refs.add(ResourceRefDTO.builder()
                    .refType("MERCHANT_PAYMENT_ROUTE")
                    .refId(String.valueOf(route.getId()))
                    .merchantId(route.getMerchantId())
                    .label(String.format("启用的商户支付路由 #%d", route.getId()))
                    .resolveHint("/admin/merchants")
                    .build());
        }
        for (ChannelRoute route : channelRouteMapper.selectList(
                new LambdaQueryWrapper<ChannelRoute>()
                        .eq(ChannelRoute::getMerchantId, merchantId)
                        .eq(ChannelRoute::getEnabled, true))) {
            refs.add(ResourceRefDTO.builder()
                    .refType("CHANNEL_ROUTE")
                    .refId(String.valueOf(route.getId()))
                    .merchantId(route.getMerchantId())
                    .label(String.format("启用的渠道路由 #%d", route.getId()))
                    .resolveHint("/admin/channel-routes")
                    .build());
        }
        if (refs.isEmpty()) {
            return ResourceDeleteCheckResult.ok();
        }
        return ResourceDeleteCheckResult.blocked(
                String.format("商户 %s 仍有 %d 条启用中的路由，请先停用或删除", merchantId, refs.size()),
                refs);
    }

    private ResourceDeleteCheckResult checkSysMenu(Long menuId) {
        if (menuId == null) {
            return ResourceDeleteCheckResult.blocked("菜单不存在", List.of());
        }
        long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (childCount <= 0) {
            return ResourceDeleteCheckResult.ok();
        }
        List<ResourceRefDTO> refs = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId))
                .stream()
                .map(child -> ResourceRefDTO.builder()
                        .refType("SYS_MENU_CHILD")
                        .refId(String.valueOf(child.getId()))
                        .label(String.format("子菜单「%s」", child.getMenuName()))
                        .resolveHint("/admin/menus")
                        .build())
                .collect(Collectors.toList());
        return ResourceDeleteCheckResult.blocked(
                String.format("菜单仍有 %d 个子菜单，请先删除子菜单", childCount),
                refs);
    }

    private ResourceDeleteCheckResult checkSysRole(Long roleId) {
        if (roleId == null) {
            return ResourceDeleteCheckResult.blocked("角色不存在", List.of());
        }
        List<SysUserRole> bindings = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        if (bindings.isEmpty()) {
            return ResourceDeleteCheckResult.ok();
        }
        List<ResourceRefDTO> refs = bindings.stream()
                .map(b -> ResourceRefDTO.builder()
                        .refType("SYS_USER_ROLE")
                        .refId(String.valueOf(b.getId()))
                        .label(String.format("用户 #%d 仍绑定该角色", b.getUserId()))
                        .resolveHint("/admin/users")
                        .build())
                .collect(Collectors.toList());
        return ResourceDeleteCheckResult.blocked(
                String.format("角色仍被 %d 个用户引用，请先解除用户角色绑定", bindings.size()),
                refs);
    }

    private static boolean inMerchantScope(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return true;
        }
        if (merchantScopeIds.isEmpty()) {
            return false;
        }
        return merchantId != null && merchantScopeIds.contains(merchantId);
    }

    private static Long toLong(Object resourceId) {
        if (resourceId instanceof Long l) {
            return l;
        }
        if (resourceId instanceof Number n) {
            return n.longValue();
        }
        if (resourceId instanceof String s && StringUtils.hasText(s)) {
            return Long.parseLong(s.trim());
        }
        throw new IllegalArgumentException("资源 ID 类型无效: " + resourceId);
    }
}
