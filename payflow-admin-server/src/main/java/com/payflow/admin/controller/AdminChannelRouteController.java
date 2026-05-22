package com.payflow.admin.controller;

import com.payflow.admin.entity.ChannelRoute;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.redis.CashierConfigRefreshPublisher;
import com.payflow.admin.service.ChannelRouteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "渠道路由")
@RestController
@RequestMapping("/api/v1/admin/channels/routes")
@RequiredArgsConstructor
/**
 * 支付路由管理 Controller（商户侧：商户 → 渠道账户的支付路由配置）
 * <p>提供支付路由的 CRUD 操作，包括：
 * <ul>
 *   <li>GET    /channels/routes        - 查询所有支付路由（支持按 channelId/priority 筛选）</li>
 *   <li>GET    /channels/routes/{id}   - 根据 ID 查询支付路由详情</li>
 *   <li>POST   /channels/routes        - 新增支付路由</li>
 *   <li>PUT    /channels/routes/{id}   - 更新支付路由</li>
 *   <li>DELETE /channels/routes/{id}  - 删除支付路由</li>
 *   <li>PUT    /channels/routes/{id}/toggle - 启用/禁用支付路由</li>
 * </ul>
 *
 * @author Lucas
 * @since 2026-04-13
 */
public class AdminChannelRouteController {

    private final ChannelRouteService service;

    /** 可选依赖：Redis 未启用时为 null */
    @Autowired(required = false)
    private CashierConfigRefreshPublisher refreshPublisher;

    /**
     * 分页查询支付路由列表
     *
     * @param merchantId 商户ID（可选）
     * @param page       页码，默认1
     * @param pageSize   每页条数，默认20
     * @return 路由列表（视图形式）
     */
    @Operation(summary = "分页查询支付路由列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String merchantId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        List<String> scope = AdminRequestContext.merchantScope(request);
        String resolvedMerchant = AdminRequestContext.resolveMerchantFilter(merchantId, scope);
        if ("__NO_ACCESS__".equals(resolvedMerchant)) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("list", List.of());
            empty.put("total", 0);
            empty.put("page", page);
            empty.put("pageSize", pageSize);
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", empty));
        }

        List<Map<String, Object>> all = service.listWithDetails();

        List<Map<String, Object>> filtered = all.stream()
                .filter(m -> {
                    Object mid = m.get("merchant_id");
                    if (mid == null) {
                        mid = m.get("merchantId");
                    }
                    String rowMerchant = mid != null ? mid.toString() : "";
                    if (resolvedMerchant != null) {
                        return resolvedMerchant.equals(rowMerchant);
                    }
                    if (scope != null && !scope.isEmpty()) {
                        return scope.contains(rowMerchant);
                    }
                    return merchantId == null || merchantId.isBlank() || merchantId.equals(rowMerchant);
                })
                .collect(java.util.stream.Collectors.toList());

        int total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<Map<String, Object>> pageList = fromIndex >= total ? List.of() : filtered.subList(fromIndex, toIndex);

        List<Map<String, Object>> viewList = pageList.stream()
                .map(this::toViewMap)
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("list", viewList);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * 创建支付路由
     *
     * @param route 路由信息
     * @return 创建的路由（视图形式）
     */
    @Operation(summary = "创建支付路由")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            HttpServletRequest request,
            @RequestBody ChannelRoute route) {
        String merchantId = AdminRequestContext.resolveMerchantIdForWrite(request, route.getMerchantId());
        route.setMerchantId(merchantId);
        ChannelRoute created = service.create(route);
        if (refreshPublisher != null) {
            refreshPublisher.publish("channel_route:create");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", toViewMapFromEntity(created));
        return ResponseEntity.ok(resp);
    }

    /**
     * 启用/禁用支付路由
     *
     * @param id 路由ID
     * @return 操作结果
     */
    @Operation(summary = "启用/禁用支付路由")
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long id) {
        try {
            service.toggle(id);
            if (refreshPublisher != null) {
                refreshPublisher.publish("channel_route:toggle");
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("message", "success");
            resp.put("data", Map.of("id", id));
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 1);
            resp.put("message", e.getMessage());
            resp.put("data", Map.of());
            return ResponseEntity.ok(resp);
        }
    }

    /**
     * 删除支付路由
     *
     * @param id 路由ID
     * @return 操作结果
     */
    @Operation(summary = "删除支付路由")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        service.delete(id);
        if (refreshPublisher != null) {
            refreshPublisher.publish("channel_route:delete");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", Map.of());
        return ResponseEntity.ok(resp);
    }

    /**
     * 将数据库Map转换为视图Map
     *
     * @param m 数据库原始Map
     * @return 视图Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toViewMap(Map<String, Object> m) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", m.get("id"));
        view.put("merchantId", m.get("merchant_id"));
        view.put("merchantName", m.get("merchant_name"));
        view.put("channelId", m.get("channel_id"));
        view.put("channelName", m.get("channel_name"));
        view.put("paymentAccountId", m.get("payment_account_id"));
        view.put("accountCode", m.get("account_code"));
        view.put("accountNo", m.get("account_code"));
        view.put("accountName", m.get("account_name"));
        view.put("enabled", m.get("enabled"));
        view.put("priority", m.get("priority"));
        view.put("description", m.get("description"));
        view.put("createdAt", m.get("created_at"));
        view.put("updatedAt", m.get("updated_at"));
        return view;
    }

    /**
     * 将ChannelRoute实体转换为视图Map
     *
     * @param r 支付路由实体（ChannelRoute）
     * @return 视图Map
     */
    private Map<String, Object> toViewMapFromEntity(ChannelRoute r) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", r.getId());
        view.put("merchantId", r.getMerchantId());
        view.put("channelId", r.getChannelId());
        view.put("paymentAccountId", r.getPaymentAccountId());
        view.put("enabled", r.getEnabled());
        view.put("priority", r.getPriority());
        view.put("description", r.getDescription());
        view.put("createdAt", r.getCreatedAt());
        view.put("updatedAt", r.getUpdatedAt());
        return view;
    }
}