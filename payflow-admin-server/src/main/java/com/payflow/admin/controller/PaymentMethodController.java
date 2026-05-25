package com.payflow.admin.controller;

import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.kit.GlobalResourceKit;
import com.payflow.admin.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 支付方式管理 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    /**
     * 查询所有支付方式列表（支持分页参数）
     *
     * @param page 分页页码（默认1）
     * @param size 每页条数（默认20）
     * @return 分页后的支付方式列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String channelType,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        int resolvedSize = pageSize != null ? pageSize : (size != null ? size : 20);
        String resolvedChannelType = org.springframework.util.StringUtils.hasText(channelType)
                ? channelType
                : channel;
        String resolvedKeyword = org.springframework.util.StringUtils.hasText(keyword)
                ? keyword
                : name;
        Map<String, Object> data = paymentMethodService.page(
                page, resolvedSize, channelId, resolvedChannelType, resolvedKeyword, status);
        data.put("classification", GlobalResourceKit.CLASSIFICATION_GLOBAL);
        data.put("merchantScoped", false);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", data
        ));
    }

    /**
     * 根据渠道ID查询支持的支付方式
     *
     * @param channelId 渠道ID
     * @return 该渠道支持的支付方式列表
     */
    @GetMapping("/channel/{channelId}")
    public ResponseEntity<Map<String, Object>> listByChannelId(@PathVariable Long channelId) {
        List<PaymentMethod> list = paymentMethodService.listByChannelId(channelId);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", list));
    }

    /**
     * 根据ID查询支付方式详情
     *
     * @param id 支付方式ID
     * @return 支付方式详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        PaymentMethod m = paymentMethodService.getById(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", m));
    }

    /**
     * 新增支付方式
     *
     * @param method 支付方式信息
     * @return 新增的支付方式
     */
    @RequirePermission("payment_method:create")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody PaymentMethod method) {
        paymentMethodService.create(method);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", method
        ));
    }

    /**
     * 更新支付方式信息
     *
     * @param id    支付方式ID
     * @param method 支付方式信息
     * @return 更新后的支付方式
     */
    @RequirePermission("payment_method:edit")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody PaymentMethod method) {
        paymentMethodService.update(id, method);
        method.setId(id);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", method
        ));
    }

    /**
     * 删除支付方式
     *
     * @param id 支付方式ID
     * @return 无
     */
    @RequirePermission("payment_method:delete")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        paymentMethodService.delete(id);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of()));
    }
}
