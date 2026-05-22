package com.payflow.admin.controller;

import com.payflow.admin.dto.PaymentRouteRequest;
import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.kit.ClientScopesKit;
import com.payflow.admin.service.MerchantPaymentRouteService;
import jakarta.servlet.http.HttpServletRequest;
import com.payflow.admin.service.PaymentAccountService;
import com.payflow.admin.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商户支付路由：支付方式 + 收款账号 + 终端可见范围（PC/H5/APP）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/merchant-payment-routes")
@RequiredArgsConstructor
public class MerchantPaymentRouteController {

    private final MerchantPaymentRouteService routeService;
    private final PaymentMethodService paymentMethodService;
    private final PaymentAccountService paymentAccountService;

    /**
     * 根据商户ID查询支付路由列表（不传 merchantId 则查全部）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listByMerchantId(
            HttpServletRequest request,
            @RequestParam(required = false) String merchantId) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        List<MerchantPaymentRoute> routes;
        if (merchantId != null && !merchantId.isEmpty()) {
            routes = routeService.listByMerchantId(merchantId, scope);
        } else {
            routes = routeService.listAll(scope);
        }

        Map<Long, PaymentMethod> methodMap = paymentMethodService.listAll().stream()
                .collect(Collectors.toMap(PaymentMethod::getId, Function.identity(),
                        (a, b) -> a));

        Map<Long, PaymentAccount> accountMap = paymentAccountService.listAll(scope).stream()
                .collect(Collectors.toMap(PaymentAccount::getId, Function.identity(),
                        (a, b) -> a));

        List<Map<String, Object>> viewList = routes.stream()
                .map(r -> toViewRow(r, methodMap, accountMap))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", viewList
        ));
    }

    /**
     * 新增单条路由（不替换整商户配置）。
     */
    @PostMapping("/item")
    public ResponseEntity<Map<String, Object>> createOne(
            HttpServletRequest request,
            @Valid @RequestBody PaymentRouteRequest body) {
        String merchantId = AdminRequestContext.resolveMerchantIdForWrite(request, body.getMerchantId());
        if (merchantId == null || merchantId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "商户号不能为空"));
        }
        MerchantPaymentRoute route = new MerchantPaymentRoute();
        route.setMerchantId(merchantId);
        route.setPaymentMethodId(body.getPaymentMethodId());
        route.setPaymentAccountId(body.getPaymentAccountId());
        route.setEnabled(body.getEnabled() != null ? body.getEnabled() : Boolean.TRUE);
        route.setPriority(body.getPriority() != null ? body.getPriority() : 0);
        route.setClientScopes(ClientScopesKit.normalizeToDb(body.getClientScopes()));
        assertAccountMatchesMethod(route.getPaymentMethodId(), route.getPaymentAccountId(), scope(request));
        routeService.createRoute(route);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody PaymentRouteRequest body) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        MerchantPaymentRoute patch = new MerchantPaymentRoute();
        if (body.getPaymentMethodId() != null) {
            patch.setPaymentMethodId(body.getPaymentMethodId());
        }
        if (body.getPaymentAccountId() != null) {
            patch.setPaymentAccountId(body.getPaymentAccountId());
        }
        if (body.getEnabled() != null) {
            patch.setEnabled(body.getEnabled());
        }
        if (body.getPriority() != null) {
            patch.setPriority(body.getPriority());
        }
        patch.setClientScopes(ClientScopesKit.normalizeToDb(body.getClientScopes()));

        Long methodId = patch.getPaymentMethodId();
        Long accountId = patch.getPaymentAccountId();
        if (methodId != null && accountId != null) {
            assertAccountMatchesMethod(methodId, accountId, scope);
        } else if (methodId != null || accountId != null) {
            MerchantPaymentRoute exist = routeService.getById(id, scope);
            if (exist == null) {
                return ResponseEntity.status(404).body(Map.of("code", 404, "message", "记录不存在"));
            }
            long mid = methodId != null ? methodId : exist.getPaymentMethodId();
            long aid = accountId != null ? accountId : exist.getPaymentAccountId();
            assertAccountMatchesMethod(mid, aid, scope);
        }
        routeService.updateRoute(id, patch, scope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success"));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            HttpServletRequest request,
            @PathVariable Long id) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        routeService.toggleRoute(id, scope);
        MerchantPaymentRoute updated = routeService.getById(id, scope);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", updated != null ? updated : Map.of()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            HttpServletRequest request,
            @PathVariable Long id) {
        routeService.deleteRoute(id, AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success"));
    }

    /**
     * 替换商户的完整路由配置（先删后增）
     */
    @PostMapping("/replace")
    public ResponseEntity<Map<String, Object>> replace(
            HttpServletRequest httpRequest,
            @RequestBody Map<String, Object> request) {
        String merchantId = AdminRequestContext.resolveMerchantIdForWrite(
                httpRequest, (String) request.get("merchantId"));
        List<String> scope = AdminRequestContext.merchantScope(httpRequest);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> routesRaw = (List<Map<String, Object>>) request.get("routes");

        List<MerchantPaymentRoute> routes;
        if (routesRaw == null) {
            routes = List.of();
        } else {
            routes = routesRaw.stream()
                    .map(m -> {
                        MerchantPaymentRoute r = new MerchantPaymentRoute();
                        Object methodId = m.get("paymentMethodId");
                        Object accountId = m.get("paymentAccountId");
                        Object enabled = m.get("enabled");
                        Object priority = m.get("priority");
                        r.setPaymentMethodId(methodId == null ? null : ((Number) methodId).longValue());
                        r.setPaymentAccountId(accountId == null ? null : ((Number) accountId).longValue());
                        r.setEnabled(enabled == null ? Boolean.TRUE : (Boolean) enabled);
                        r.setPriority(priority == null ? 0 : ((Number) priority).intValue());
                        r.setClientScopes(ClientScopesKit.normalizeToDb(m.get("clientScopes")));
                        assertAccountMatchesMethod(r.getPaymentMethodId(), r.getPaymentAccountId(), scope);
                        return r;
                    })
                    .collect(Collectors.toList());
        }

        routeService.replaceRoutes(merchantId, routes, scope);

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success"
        ));
    }

    private Map<String, Object> toViewRow(MerchantPaymentRoute r,
                                          Map<Long, PaymentMethod> methodMap,
                                          Map<Long, PaymentAccount> accountMap) {
        PaymentMethod method = methodMap.get(r.getPaymentMethodId());
        PaymentAccount account = accountMap.get(r.getPaymentAccountId());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getId());
        row.put("merchantId", r.getMerchantId());
        row.put("paymentMethodId", r.getPaymentMethodId());
        row.put("paymentAccountId", r.getPaymentAccountId());
        row.put("enabled", r.getEnabled());
        row.put("priority", r.getPriority());
        row.put("clientScopes", ClientScopesKit.parseToList(r.getClientScopes()));
        row.put("paymentMethod", method == null ? null : paymentMethodToMap(method));
        row.put("paymentAccount", account == null ? null : paymentAccountToMap(account));
        return row;
    }

    private Map<String, Object> paymentMethodToMap(PaymentMethod method) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", method.getId());
        m.put("methodCode", method.getMethodCode());
        m.put("methodName", method.getMethodName());
        m.put("channelId", method.getChannelId());
        m.put("channelName", method.getChannelName());
        m.put("enabled", method.getEnabled());
        m.put("status", method.getStatus());
        return m;
    }

    private Map<String, Object> paymentAccountToMap(PaymentAccount account) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", account.getId());
        m.put("accountCode", account.getAccountCode());
        m.put("accountName", account.getAccountName());
        m.put("channelId", account.getChannelId());
        m.put("channelName", account.getChannelName());
        m.put("enabled", account.getEnabled());
        m.put("priority", account.getPriority());
        return m;
    }

    private static List<String> scope(HttpServletRequest request) {
        return AdminRequestContext.merchantScope(request);
    }

    private void assertAccountMatchesMethod(Long paymentMethodId, Long paymentAccountId, List<String> merchantScopeIds) {
        if (paymentMethodId == null || paymentAccountId == null) {
            throw new IllegalArgumentException("支付方式与支付账号不能为空");
        }
        PaymentMethod method = paymentMethodService.getById(paymentMethodId);
        PaymentAccount account = paymentAccountService.getById(paymentAccountId, merchantScopeIds);
        if (method == null) {
            throw new IllegalArgumentException("支付方式不存在: " + paymentMethodId);
        }
        if (account == null) {
            throw new IllegalArgumentException("支付账号不存在: " + paymentAccountId);
        }
        if (!method.getChannelId().equals(account.getChannelId())) {
            throw new IllegalArgumentException("支付账号与支付方式归属渠道不一致，请选择与方式相同渠道的账号");
        }
    }
}
