package com.payflow.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能路由健康度只读查询（与收银台写入的 Redis 键一致）。
 */
@RestController
@RequestMapping("/api/v1/admin/channel-routing")
@RequiredArgsConstructor
public class AdminChannelRoutingController {

    private static final String KEY_OK = "payflow:route:ok:";
    private static final String KEY_FAIL = "payflow:route:fail:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 查询单个渠道账户的失败率与计数。
     */
    @GetMapping("/health")
    public Map<String, Object> health(@RequestParam("accountCode") String accountCode) {
        Map<String, Object> m = new HashMap<>();
        if (accountCode == null || accountCode.isBlank()) {
            m.put("error", "accountCode 必填");
            return m;
        }
        try {
            String okStr = stringRedisTemplate.opsForValue().get(KEY_OK + accountCode);
            String failStr = stringRedisTemplate.opsForValue().get(KEY_FAIL + accountCode);
            long ok = parseLong(okStr);
            long fail = parseLong(failStr);
            long total = ok + fail;
            double rate = total <= 0 ? 0.0 : (double) fail / (double) total;
            m.put("accountCode", accountCode);
            m.put("successCount", ok);
            m.put("failCount", fail);
            m.put("failureRate", rate);
            return m;
        } catch (Exception e) {
            m.put("error", e.getMessage());
            return m;
        }
    }

    private static long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
