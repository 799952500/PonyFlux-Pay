package com.payflow.cashier.middleware;

import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * 支付创建幂等：请求头 Idempotency-Key + 商户号，在 TTL 内禁止重复提交。
 */
@Component
@RequiredArgsConstructor
public class PaymentIdempotencyInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            return true;
        }
        String merchantId = (String) request.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
        if (merchantId == null || merchantId.isBlank()) {
            return true;
        }
        String redisKey = "payflow:idempotency:payment:" + merchantId + ":" + key.trim();
        try {
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofMinutes(15));
            if (Boolean.FALSE.equals(first)) {
                throw new BizException(4291, "重复的 Idempotency-Key，请勿重复提交支付");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            return true;
        }
        return true;
    }
}
