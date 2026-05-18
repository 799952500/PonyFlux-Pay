package com.payflow.cashier.security;

import com.payflow.cashier.constant.MerchantSecurityErrorCodes;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.service.SecurityAuditService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 校验请求中的 merchantId 与认证上下文一致。
 *
 * @author PayFlow Team
 */
@Component
@RequiredArgsConstructor
public class MerchantIdGuard {

    private final SecurityAuditService securityAuditService;

    /**
     * 若传入 merchantId 非空且与上下文不一致则拒绝。
     */
    public void assertMatchesContext(String bodyOrQueryMerchantId, String httpMethod, String requestPath,
                                     String clientIp, String userAgent) {
        if (bodyOrQueryMerchantId == null || bodyOrQueryMerchantId.isBlank()) {
            return;
        }
        String contextMerchantId = MerchantContext.getMerchantId();
        if (contextMerchantId == null || contextMerchantId.isBlank()) {
            throw new BizException(MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH,
                    MerchantSecurityErrorCodes.MSG_MERCHANT_ID_MISMATCH);
        }
        if (contextMerchantId.equals(bodyOrQueryMerchantId)) {
            return;
        }
        securityAuditService.recordDenied(
                contextMerchantId,
                bodyOrQueryMerchantId,
                MerchantContext.getAuthMode(),
                httpMethod,
                requestPath,
                null,
                null,
                clientIp,
                userAgent,
                MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH,
                "请求 merchantId 与认证上下文不一致");
        throw new BizException(MerchantSecurityErrorCodes.MERCHANT_ID_MISMATCH,
                MerchantSecurityErrorCodes.MSG_MERCHANT_ID_MISMATCH);
    }
}
