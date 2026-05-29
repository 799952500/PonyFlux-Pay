package com.payflow.admin.controller;

import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.service.PaymentAccountService;
import com.payflow.common.web.PageRequest;
import com.payflow.common.web.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付账号管理 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/payment-accounts")
@RequiredArgsConstructor
public class PaymentAccountController {

    private final PaymentAccountService service;

    /**
     * 分页查询支付账号列表
     *
     * @param page      页码（默认1）
     * @param pageSize  每页条数（默认20）
     * @param channelId 渠道ID筛选（可选）
     * @param keyword   账号编码/名称关键词搜索（可选）
     * @return 分页后的账号列表（敏感字段已脱敏）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String keyword) {

        PageRequest pr = PageRequest.of(page, pageSize);
        PageResult<PaymentAccount> result = service.page(
                pr, channelId, keyword, AdminRequestContext.merchantScope(request));

        List<Map<String, Object>> safeList = result.getList().stream()
                .map(this::toSafeMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of(
                        "list", safeList,
                        "total", result.getTotal(),
                        "page", result.getPage(),
                        "pageSize", result.getSize()
                )
        ));
    }

    /**
     * 根据ID查询支付账号详情
     *
     * @param id 账号ID
     * @return 账号详情（敏感字段已脱敏）
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        PaymentAccount account = service.getById(id);
        if (account == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 1,
                    "message", "支付账号不存在",
                    "data", Map.of()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", toSafeMap(account)
        ));
    }

    /**
     * 新增支付账号
     *
     * @param account 账号信息
     * @return 新增的账号信息（敏感字段已脱敏）
     */
    @RequirePermission("payment_account:create")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody PaymentAccount account) {
        PaymentAccount created = service.create(account);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", toSafeMap(created)
        ));
    }

    /**
     * 更新支付账号
     *
     * @param id      账号ID
     * @param account 账号信息
     * @return 更新后的账号信息（敏感字段已脱敏）
     */
    @RequirePermission("payment_account:edit")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long id,
            @RequestBody PaymentAccount account) {
        account.setId(id);
        PaymentAccount updated = service.update(account);
        if (updated == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 1,
                    "message", "支付账号不存在",
                    "data", Map.of()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", toSafeMap(updated)
        ));
    }

    /**
     * 删除支付账号
     *
     * @param id 账号ID
     * @return 操作结果
     */
    @RequirePermission("payment_account:delete")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of()));
    }

    /**
     * 将实体转换为脱敏的 Map（移除敏感字段）
     *
     * @param a 支付账号实体
     * @return 脱敏后的字段映射
     */
    private Map<String, Object> toSafeMap(PaymentAccount a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("channelId", a.getChannelId());
        m.put("channelName", a.getChannelName());
        m.put("accountCode", a.getAccountCode());
        m.put("accountName", a.getAccountName());
        m.put("enabled", a.getEnabled());
        m.put("priority", a.getPriority());
        m.put("description", a.getDescription());
        m.put("createdAt", a.getCreatedAt());
        m.put("updatedAt", a.getUpdatedAt());
        return m;
    }
}

