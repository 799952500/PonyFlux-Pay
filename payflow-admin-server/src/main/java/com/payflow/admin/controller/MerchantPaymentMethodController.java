package com.payflow.admin.controller;

import com.payflow.admin.dto.SavePaymentMethodRequest;
import com.payflow.admin.entity.MerchantPaymentMethod;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.MerchantPaymentMethodService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商户支付方式配置 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/merchant-payment-methods")
@RequiredArgsConstructor
public class MerchantPaymentMethodController {

    private final MerchantPaymentMethodService service;

    /**
     * 根据商户ID查询支付方式列表
     *
     * @param merchantId 商户ID（可选）
     * @return 支付方式列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listByMerchantId(
            HttpServletRequest request,
            @RequestParam(required = false) String merchantId) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        List<MerchantPaymentMethod> list;
        if (merchantId != null && !merchantId.isEmpty()) {
            if ("__NO_ACCESS__".equals(AdminRequestContext.resolveMerchantFilter(merchantId, scope))) {
                list = List.of();
            } else {
                AdminRequestContext.assertMerchantAllowed(merchantId, scope);
                list = service.listByMerchantId(merchantId);
            }
        } else {
            list = service.listAll(scope);
        }
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", list
        ));
    }

    /**
     * 新增单条商户支付方式绑定（不删除该商户已有绑定；与批量 saveBatch 区分）。
     */
    @PostMapping("/item")
    public ResponseEntity<Map<String, Object>> createOne(
            HttpServletRequest request,
            @RequestBody MerchantPaymentMethod mpm) {
        if (mpm.getPaymentMethodId() == null) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "支付方式不能为空"));
        }
        String merchantId = AdminRequestContext.resolveMerchantIdForWrite(request, mpm.getMerchantId());
        if (merchantId == null || merchantId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "商户号不能为空"));
        }
        mpm.setMerchantId(merchantId);
        if (mpm.getEnabled() == null) {
            mpm.setEnabled(true);
        }
        if (mpm.getPriority() == null) {
            mpm.setPriority(0);
        }
        service.create(mpm);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    /**
     * 批量保存商户支付方式配置
     *
     * @param request 请求体，包含 merchantId 和 paymentMethodIds
     * @return 操作结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveBatch(
            HttpServletRequest request,
            @Valid @RequestBody SavePaymentMethodRequest body) {
        String merchantId = AdminRequestContext.resolveMerchantIdForWrite(request, body.getMerchantId());
        if (merchantId == null || merchantId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "商户号不能为空"));
        }
        List<Long> methodIds = body.getPaymentMethodIds();

        service.deleteByMerchant(merchantId);

        if (methodIds != null) {
            for (Long methodId : methodIds) {
                MerchantPaymentMethod mpm = new MerchantPaymentMethod();
                mpm.setMerchantId(merchantId);
                mpm.setPaymentMethodId(methodId);
                mpm.setEnabled(true);
                service.create(mpm);
            }
        }

        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success"
        ));
    }

    /**
     * 更新商户支付方式配置
     *
     * @param id  配置ID
     * @param mpm 支付方式配置
     * @return 无
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody MerchantPaymentMethod mpm) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        MerchantPaymentMethod existing = service.getById(id, scope);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (mpm.getMerchantId() != null && !mpm.getMerchantId().isBlank()) {
            AdminRequestContext.assertMerchantAllowed(mpm.getMerchantId(), scope);
        }
        service.update(id, mpm);
        return ResponseEntity.ok().build();
    }

    /**
     * 切换商户支付方式启用状态
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            HttpServletRequest request,
            @PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400, "message", "ID不能为空"
            ));
        }
        MerchantPaymentMethod mpm = service.getById(id, AdminRequestContext.merchantScope(request));
        if (mpm == null) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404, "message", "记录不存在"
            ));
        }
        mpm.setEnabled(!mpm.getEnabled());
        service.update(id, mpm);
        return ResponseEntity.ok(Map.of(
            "code", 0,
            "message", "success",
            "data", mpm
        ));
    }

    /**
     * 删除商户支付方式配置
     *
     * @param id 配置ID
     * @return 无
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            HttpServletRequest request,
            @PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("配置ID不能为空");
        }
        if (service.getById(id, AdminRequestContext.merchantScope(request)) == null) {
            return ResponseEntity.notFound().build();
        }
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
