package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.dto.UpdateMerchantRequest;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商户管理 Controller
  * @author Lucas
 */
@Tag(name = "商户管理")
@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final MerchantService merchantService;

    /**
     * 分页查询商户列表
     *
     * @param status    商户状态（可选）
     * @param page      页码（默认1）
     * @param keyword   搜索关键词（可选）
     * @param pageSize  每页条数（默认20）
     * @param size      每页条数备选参数（可选）
     * @return 分页后的商户列表
     */
    @Operation(summary = "分页查询商户列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listMerchants(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer size) {

        int resolvedPageSize;
        if (pageSize != null) {
            resolvedPageSize = pageSize;
        } else if (size != null) {
            resolvedPageSize = size;
        } else {
            resolvedPageSize = 20;
        }

        IPage<Merchant> merchantPage = merchantService.page(
                page, resolvedPageSize, keyword, status, AdminRequestContext.merchantScope(request));

        Map<String, Object> data = new HashMap<>();
        data.put("total", merchantPage.getTotal());
        data.put("page", page);
        data.put("pageSize", resolvedPageSize);
        data.put("list", merchantPage.getRecords());

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * 根据商户号查询商户详情
     *
     * @param merchantId 商户号
     * @return 商户信息
     */
    @Operation(summary = "查询商户详情")
    @GetMapping("/{merchantId}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable String merchantId) {
        Merchant merchant = merchantService.getByMerchantId(merchantId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", merchant);
        return ResponseEntity.ok(resp);
    }

    /**
     * 查询所有商户的下拉列表（精简信息）
     *
     * @return 商户号与名称的键值对列表
     */
    @Operation(summary = "查询商户下拉列表")
    @GetMapping("/simple")
    public ResponseEntity<Map<String, Object>> listSimple() {
        List<Merchant> all = merchantService.listAll();
        List<Map<String, Object>> simpleList = all.stream()
                .map(m -> Map.of(
                        "merchantId", (Object) m.getMerchantId(),
                        "merchantName", (Object) m.getMerchantName()
                ))
                .collect(Collectors.toList());
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", simpleList);
        return ResponseEntity.ok(resp);
    }

    /**
     * 更新商户信息
     *
     * @param merchantId 商户号
     * @param body       待更新的字段映射
     * @return 更新后的商户信息
     */
    @Operation(summary = "更新商户信息")
    @RequirePermission("merchant:edit")
    @PutMapping("/{merchantId}")
    public ResponseEntity<Map<String, Object>> updateMerchant(
            @PathVariable String merchantId,
            @Valid @RequestBody UpdateMerchantRequest body) {
        Merchant existing = merchantService.getByMerchantId(merchantId);
        if (existing == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 404);
            resp.put("message", "商户不存在");
            return ResponseEntity.status(404).body(resp);
        }

        if (body.getMerchantName() != null) {
            existing.setMerchantName(body.getMerchantName());
        }
        if (body.getMerchantKey() != null) {
            existing.setMerchantKey(body.getMerchantKey());
        }
        if (body.getCallbackUrl() != null) {
            existing.setCallbackUrl(body.getCallbackUrl());
        }
        if (body.getNotifyUrl() != null) {
            existing.setNotifyUrl(body.getNotifyUrl());
        }
        if (body.getCommissionRate() != null) {
            existing.setCommissionRate(body.getCommissionRate());
        }
        if (body.getStatus() != null) {
            existing.setStatus(body.getStatus());
        }

        merchantService.update(existing.getId(), existing);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", existing);
        return ResponseEntity.ok(resp);
    }

    /**
     * 删除商户（按商户号）
     *
     * @param merchantId 商户号
     * @return 操作结果
     */
    @Operation(summary = "删除商户")
    @RequirePermission("merchant:delete")
    @DeleteMapping("/{merchantId}")
    public ResponseEntity<Map<String, Object>> deleteMerchant(@PathVariable String merchantId) {
        Merchant existing = merchantService.getByMerchantId(merchantId);
        if (existing == null) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 404);
            resp.put("message", "商户不存在");
            resp.put("data", Map.of());
            return ResponseEntity.ok(resp);
        }
        merchantService.delete(existing.getId());
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", Map.of());
        return ResponseEntity.ok(resp);
    }
}
