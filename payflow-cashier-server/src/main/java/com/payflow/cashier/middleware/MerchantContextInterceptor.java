package com.payflow.cashier.middleware;

import com.payflow.cashier.context.AuthMode;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.context.MerchantScopeHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 将认证拦截器写入的 merchantId 同步到 {@link MerchantContext}。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class MerchantContextInterceptor implements HandlerInterceptor {

  public static final String ATTR_AUTH_MODE = "authMode";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String merchantId = (String) request.getAttribute(JwtAuthInterceptor.ATTR_MERCHANT_ID);
    if (merchantId == null) {
      merchantId = (String) request.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
    }
    AuthMode authMode = (AuthMode) request.getAttribute(ATTR_AUTH_MODE);
    if (authMode == null) {
      authMode = AuthMode.INTERNAL;
    }
    if (merchantId != null && !merchantId.isBlank()) {
      MerchantContext.set(merchantId, authMode, request.getRequestURI(), resolveClientIp(request));
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    MerchantContext.clear();
    MerchantScopeHolder.clear();
  }

  private static String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
    }
    return request.getRemoteAddr();
  }
}
