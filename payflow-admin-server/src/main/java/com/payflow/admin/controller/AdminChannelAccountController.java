package com.payflow.admin.controller;

import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.service.PaymentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 渠道支付账号管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/channels/accounts")
@RequiredArgsConstructor
public class AdminChannelAccountController {

    private final PaymentAccountService service;

    /**
     * 分页查询支付账号列表
     *
     * @param page      页码，默认1
     * @param pageSize  每页条数，默认20
     * @param channelId 渠道ID（可选）
     * @param keyword   关键词，匹配账号编号或名称（可选）
     * @return 账号列表（脱敏后）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String keyword) {

        List<PaymentAccount> all = service.listAll();
        List<PaymentAccount> filtered = all.stream()
                .filter(a -> channelId == null || (a.getChannelId() != null && a.getChannelId().equals(channelId)))
                .filter(a -> keyword == null || keyword.isBlank()
                        || (a.getAccountCode() != null && a.getAccountCode().contains(keyword))
                        || (a.getAccountName() != null && a.getAccountName().contains(keyword)))
                .collect(Collectors.toList());

        int total = filtered.size();
        if (total == 0) {
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "success",
                    "data", Map.of(
                            "list", List.of(),
                            "total", 0,
                            "page", page,
                            "pageSize", pageSize
                    )
            ));
        }
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<PaymentAccount> pageList = filtered.subList(fromIndex, toIndex);

        List<Map<String, Object>> safeList = pageList.stream()
                .map(this::toSafeMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of(
                        "list", safeList,
                        "total", total,
                        "page", page,
                        "pageSize", pageSize
                )
        ));
    }

    /**
     * 创建支付账号
     *
     * @param account 账号信息
     * @return 创建成功的账号（脱敏后）
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody PaymentAccount account) {
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
     * @param account 更新后的账号信息
     * @return 更新后的账号（脱敏后）
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAccount(
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
     * 启用/禁用支付账号
     *
     * @param id 账号ID
     * @return 操作后的账号（脱敏后）
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Long id) {
        PaymentAccount account = service.getById(id);
        if (account == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 1,
                    "message", "支付账号不存在",
                    "data", Map.of()
            ));
        } else {
            account.setEnabled(account.getEnabled() == null || !account.getEnabled());
            service.update(account);
        }
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", toSafeMap(account)
        ));
    }

    /**
     * 删除支付账号
     *
     * @param id 账号ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAccount(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of(
                    "code", 0,
                    "message", "success",
                    "data", Map.of()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of(
                    "code", 1,
                    "message", e.getMessage(),
                    "data", Map.of()
            ));
        }
    }

    /**
     * 将支付账号实体转换为安全Map（移除敏感字段）
     *
     * @param a 支付账号实体
     * @return 脱敏后的Map，不包含 appSecret、mchKey、certPassword 等敏感字段
     */
    private Map<String, Object> toSafeMap(PaymentAccount a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("channelId", a.getChannelId());
        m.put("channelName", a.getChannelName());
        m.put("accountCode", a.getAccountCode());
        m.put("accountName", a.getAccountName());
        m.put("appId", a.getAppId());
        m.put("mchId", a.getMchId());
        m.put("configJson", a.getConfigJson());
        m.put("enabled", a.getEnabled());
        m.put("priority", a.getPriority());
        m.put("description", a.getDescription());
        m.put("createdAt", a.getCreatedAt());
        m.put("updatedAt", a.getUpdatedAt());
        return m;
    }
}