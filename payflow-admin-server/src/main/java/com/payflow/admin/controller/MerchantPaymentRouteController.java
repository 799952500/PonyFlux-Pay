package com.payflow.admin.controller;

import com.payflow.admin.entity.MerchantPaymentRoute;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.service.MerchantPaymentRouteService;
import com.payflow.admin.service.PaymentAccountService;
import com.payflow.admin.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商户支付路由配置 Controller
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
     *
     * @param merchantId 商户ID（可选）
     * @return 路由列表（包含支付方式和账户详情）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listByMerchantId(
            @RequestParam(required = false) String merchantId) {
        List<MerchantPaymentRoute> routes;
        if (merchantId != null && !merchantId.isEmpty()) {
            routes = routeService.listByMerchantId(merchantId);
        } else {
            routes = routeService.listAll();
        }

        Map<Long, PaymentMethod> methodMap = paymentMethodService.listAll().stream()
                .collect(Collectors.toMap(PaymentMethod::getId, Function.identity(),
                        (a, b) -> a));

        Map<Long, PaymentAccount> accountMap = paymentAccountService.listAll().stream()
                .collect(Collectors.toMap(PaymentAccount::getId, Function.identity(),
                        (a, b) -> a));

        List<Map<String, Object>> viewList = routes.stream()
                .map(r -> {
                    PaymentMethod method = methodMap.get(r.getPaymentMethodId());
                    PaymentAccount account = accountMap.get(r.getPaymentAccountId());
                    return Map.of(
                            "id", r.getId(),
                            "merchantId", r.getMerchantId(),
                            "paymentMethodId", r.getPaymentMethodId(),
                            "paymentAccountId", r.getPaymentAccountId(),
                            "enabled", r.getEnabled(),
                            "priority", r.getPriority(),
                            "paymentMethod", method == null ? null : Map.of(
                                    "id", method.getId(),
                                    "methodCode", method.getMethodCode(),
                                    "methodName", method.getMethodName(),
                                    "channelId", method.getChannelId(),
                                    "channelName", method.getChannelName(),
                                    "enabled", method.getEnabled(),
                                    "status", method.getStatus()
                            ),
                            "paymentAccount", account == null ? null : Map.of(
                                    "id", account.getId(),
                                    "accountCode", account.getAccountCode(),
                                    "accountName", account.getAccountName(),
                                    "channelId", account.getChannelId(),
                                    "channelName", account.getChannelName(),
                                    "enabled", account.getEnabled(),
                                    "priority", account.getPriority()
                            )
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", viewList
        ));
    }

    /**
     * 替换商户的完整路由配置（先删后增）
     *
     * @param request 包含 merchantId 和 routes 列表
     * @return 操作结果
     */
    @PostMapping("/replace")
    public ResponseEntity<Map<String, Object>> replace(
            @RequestBody Map<String, Object> request) {
        String merchantId = (String) request.get("merchantId");
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
                        return r;
                    })
                    .collect(Collectors.toList());
        }

        routeService.replaceRoutes(merchantId, routes);

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success"
        ));
    }
}
