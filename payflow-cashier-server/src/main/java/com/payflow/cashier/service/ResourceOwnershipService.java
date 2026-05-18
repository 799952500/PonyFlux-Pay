package com.payflow.cashier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.constant.MerchantSecurityErrorCodes;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.entity.PaymentLinkEntity;
import com.payflow.cashier.entity.Refund;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentLinkEntityMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.mapper.RefundMapper;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 资源所有权校验。
 *
 * @author PayFlow Team
 */
@Service
@RequiredArgsConstructor
public class ResourceOwnershipService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final RefundMapper refundMapper;
    private final PaymentLinkEntityMapper paymentLinkEntityMapper;
    private final SecurityAuditService securityAuditService;

    public void assertOrderOwned(String orderId, String httpMethod, String requestPath, String clientIp, String userAgent) {
        assertOwned(resolveOrderMerchantId(orderId), "ORDER", orderId, httpMethod, requestPath, clientIp, userAgent);
    }

    public void assertPaymentOwned(String paymentId, String httpMethod, String requestPath, String clientIp, String userAgent) {
        assertOwned(resolvePaymentMerchantId(paymentId), "PAYMENT", paymentId, httpMethod, requestPath, clientIp, userAgent);
    }

    public void assertRefundOwned(String refundId, String httpMethod, String requestPath, String clientIp, String userAgent) {
        assertOwned(resolveRefundMerchantId(refundId), "REFUND", refundId, httpMethod, requestPath, clientIp, userAgent);
    }

    public void assertPaymentLinkOwned(String linkId, String httpMethod, String requestPath, String clientIp, String userAgent) {
        assertOwned(resolveLinkMerchantId(linkId), "LINK", linkId, httpMethod, requestPath, clientIp, userAgent);
    }

    private void assertOwned(String resourceMerchantId, String resourceType, String resourceId,
                             String httpMethod, String requestPath, String clientIp, String userAgent) {
        String contextMerchantId = MerchantContext.getMerchantId();
        if (contextMerchantId == null || contextMerchantId.isBlank()) {
            deny(resourceType, resourceId, httpMethod, requestPath, clientIp, userAgent, "无商户上下文");
        }
        if (resourceMerchantId == null) {
            deny(resourceType, resourceId, httpMethod, requestPath, clientIp, userAgent, "资源不存在");
        }
        if (!contextMerchantId.equals(resourceMerchantId)) {
            deny(resourceType, resourceId, httpMethod, requestPath, clientIp, userAgent, "资源不属于当前商户");
        }
    }

    private void deny(String resourceType, String resourceId, String httpMethod, String requestPath,
                      String clientIp, String userAgent, String detail) {
        securityAuditService.recordDenied(
                MerchantContext.getMerchantId(),
                null,
                MerchantContext.getAuthMode(),
                httpMethod,
                requestPath,
                resourceType,
                resourceId,
                clientIp,
                userAgent,
                MerchantSecurityErrorCodes.RESOURCE_FORBIDDEN_INTERNAL,
                detail);
        throw new BizException(MerchantSecurityErrorCodes.RESOURCE_NOT_FOUND,
                MerchantSecurityErrorCodes.MSG_RESOURCE_NOT_FOUND);
    }

    private String resolveOrderMerchantId(String orderId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId).last("LIMIT 1"));
        return order != null ? order.getMerchantId() : null;
    }

    private String resolvePaymentMerchantId(String paymentId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, paymentId).last("LIMIT 1"));
        if (payment == null || payment.getOrderId() == null) {
            return null;
        }
        return resolveOrderMerchantId(payment.getOrderId());
    }

    private String resolveRefundMerchantId(String refundId) {
        Refund refund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>().eq(Refund::getRefundId, refundId).last("LIMIT 1"));
        if (refund == null) {
            return null;
        }
        if (refund.getOrderId() != null) {
            return resolveOrderMerchantId(refund.getOrderId());
        }
        if (refund.getPaymentId() != null) {
            return resolvePaymentMerchantId(refund.getPaymentId());
        }
        return null;
    }

    private String resolveLinkMerchantId(String linkId) {
        PaymentLinkEntity link = paymentLinkEntityMapper.selectOne(
                new LambdaQueryWrapper<PaymentLinkEntity>().eq(PaymentLinkEntity::getLinkId, linkId).last("LIMIT 1"));
        return link != null ? link.getMerchantId() : null;
    }
}
