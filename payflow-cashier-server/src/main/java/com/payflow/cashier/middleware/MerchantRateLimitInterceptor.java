package com.payflow.cashier.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 商户维度简单限流：按分钟滑动窗口计数，超限返回 429。
 */
@Component
@RequiredArgsConstructor
public class MerchantRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_PER_MINUTE = 600;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String merchantId = (String) request.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
        if (merchantId == null || merchantId.isBlank()) {
            return true;
        }
        String key = "payflow:ratelimit:merchant:" + merchantId + ":" + (System.currentTimeMillis() / 60_000);
        try {
            Long v = stringRedisTemplate.opsForValue().increment(key);
            if (v != null && v == 1L) {
                stringRedisTemplate.expire(key, Duration.ofMinutes(2));
            }
            if (v != null && v > MAX_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁\"}");
                return false;
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }
}
