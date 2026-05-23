package com.payflow.cashier.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.constant.MerchantNotifyConstants;
import com.payflow.cashier.dto.MerchantNotifyDeliveryResult;
import com.payflow.cashier.dto.MqMessage;
import com.payflow.cashier.entity.MerchantNotify;
import com.payflow.cashier.entity.MerchantNotifyAttempt;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.MerchantNotifyAttemptMapper;
import com.payflow.cashier.mapper.MerchantNotifyMapper;
import com.payflow.cashier.service.MerchantNotifyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 商户回调汇总与明细写库实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantNotifyRecordServiceImpl implements MerchantNotifyRecordService {

    private final MerchantNotifyMapper merchantNotifyMapper;
    private final MerchantNotifyAttemptMapper merchantNotifyAttemptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantNotify recordNotConfigured(Order order, MqMessage message) {
        String notifyType = resolveNotifyType(message);
        MerchantNotify existing = findByOrderAndType(order.getOrderId(), notifyType);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setSummaryStatus(MerchantNotifyConstants.SUMMARY_NOT_CONFIGURED);
            existing.setNotifyUrl(null);
            existing.setAttemptCount(0);
            existing.setLastAttemptAt(null);
            existing.setLastFailReason("未配置商户回调地址");
            existing.setLastResponsePreview(null);
            existing.setOrderStatusSnapshot(order.getStatus());
            existing.setNotifyPayloadStatus(message.getExt1());
            existing.setUpdatedAt(now);
            merchantNotifyMapper.updateById(existing);
            return existing;
        }
        MerchantNotify created = MerchantNotify.builder()
                .notifyId(generateNotifyId())
                .orderId(order.getOrderId())
                .merchantId(order.getMerchantId())
                .merchantOrderNo(order.getMerchantOrderNo())
                .notifyType(notifyType)
                .notifyUrl(null)
                .summaryStatus(MerchantNotifyConstants.SUMMARY_NOT_CONFIGURED)
                .attemptCount(0)
                .lastFailReason("未配置商户回调地址")
                .orderStatusSnapshot(order.getStatus())
                .notifyPayloadStatus(message.getExt1())
                .createdAt(now)
                .updatedAt(now)
                .build();
        merchantNotifyMapper.insert(created);
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttemptContext beginAttempt(Order order, MqMessage message, String notifyUrl,
                                       Map<String, Object> requestParams, boolean signSkipped) {
        String notifyType = resolveNotifyType(message);
        MerchantNotify summary = upsertSummaryForAttempt(order, message, notifyType, notifyUrl);
        int attemptNo = (summary.getAttemptCount() == null ? 0 : summary.getAttemptCount()) + 1;
        LocalDateTime now = LocalDateTime.now();

        TruncatedPayload requestPayload = truncate(JSONUtil.toJsonStr(requestParams));
        MerchantNotifyAttempt attempt = MerchantNotifyAttempt.builder()
                .notifyId(summary.getNotifyId())
                .attemptNo(attemptNo)
                .requestParams(requestPayload.content())
                .resultStatus(MerchantNotifyConstants.RESULT_IN_PROGRESS)
                .failReasonType(signSkipped ? MerchantNotifyConstants.FAIL_SIGN_SKIPPED : null)
                .truncated(requestPayload.truncated())
                .createdAt(now)
                .build();
        merchantNotifyAttemptMapper.insert(attempt);

        summary.setAttemptCount(attemptNo);
        summary.setSummaryStatus(MerchantNotifyConstants.SUMMARY_IN_PROGRESS);
        summary.setLastAttemptAt(now);
        summary.setUpdatedAt(now);
        merchantNotifyMapper.updateById(summary);

        return new AttemptContext(summary, attemptNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishAttempt(MerchantNotify summary, int attemptNo, Map<String, Object> requestParams,
                              MerchantNotifyDeliveryResult delivery, boolean willRetry) {
        LocalDateTime now = LocalDateTime.now();
        TruncatedPayload responsePayload = truncate(delivery.getResponseBody());

        MerchantNotifyAttempt attempt = merchantNotifyAttemptMapper.selectOne(
                new LambdaQueryWrapper<MerchantNotifyAttempt>()
                        .eq(MerchantNotifyAttempt::getNotifyId, summary.getNotifyId())
                        .eq(MerchantNotifyAttempt::getAttemptNo, attemptNo));
        if (attempt != null) {
            TruncatedPayload requestPayload = truncate(JSONUtil.toJsonStr(requestParams));
            attempt.setRequestParams(requestPayload.content());
            attempt.setResponseBody(responsePayload.content());
            attempt.setHttpStatus(delivery.getHttpStatus());
            attempt.setDurationMs((int) delivery.getDurationMs());
            attempt.setTruncated(requestPayload.truncated() || responsePayload.truncated());
            attempt.setResultStatus(delivery.isSuccess()
                    ? MerchantNotifyConstants.RESULT_SUCCESS
                    : MerchantNotifyConstants.RESULT_FAILED);
            attempt.setFailReasonType(delivery.getFailReasonType());
            attempt.setFailReasonDetail(delivery.getFailReasonDetail());
            merchantNotifyAttemptMapper.updateById(attempt);
        }

        summary.setLastAttemptAt(now);
        summary.setLastResponsePreview(preview(responsePayload.content()));
        summary.setUpdatedAt(now);
        if (delivery.isSuccess()) {
            summary.setSummaryStatus(MerchantNotifyConstants.SUMMARY_SUCCESS);
            summary.setLastFailReason(null);
        } else if (willRetry) {
            summary.setSummaryStatus(MerchantNotifyConstants.SUMMARY_IN_PROGRESS);
            summary.setLastFailReason(delivery.getFailReasonDetail());
        } else {
            summary.setSummaryStatus(MerchantNotifyConstants.SUMMARY_FAILED);
            summary.setLastFailReason(delivery.getFailReasonDetail());
        }
        merchantNotifyMapper.updateById(summary);
    }

    private MerchantNotify upsertSummaryForAttempt(Order order, MqMessage message,
                                                   String notifyType, String notifyUrl) {
        MerchantNotify existing = findByOrderAndType(order.getOrderId(), notifyType);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setNotifyUrl(notifyUrl);
            existing.setOrderStatusSnapshot(order.getStatus());
            existing.setNotifyPayloadStatus(message.getExt1());
            existing.setUpdatedAt(now);
            merchantNotifyMapper.updateById(existing);
            return existing;
        }
        MerchantNotify created = MerchantNotify.builder()
                .notifyId(generateNotifyId())
                .orderId(order.getOrderId())
                .merchantId(order.getMerchantId())
                .merchantOrderNo(order.getMerchantOrderNo())
                .notifyType(notifyType)
                .notifyUrl(notifyUrl)
                .summaryStatus(MerchantNotifyConstants.SUMMARY_PENDING)
                .attemptCount(0)
                .orderStatusSnapshot(order.getStatus())
                .notifyPayloadStatus(message.getExt1())
                .createdAt(now)
                .updatedAt(now)
                .build();
        merchantNotifyMapper.insert(created);
        return created;
    }

    private MerchantNotify findByOrderAndType(String orderId, String notifyType) {
        return merchantNotifyMapper.selectOne(
                new LambdaQueryWrapper<MerchantNotify>()
                        .eq(MerchantNotify::getOrderId, orderId)
                        .eq(MerchantNotify::getNotifyType, notifyType));
    }

    static String resolveNotifyType(MqMessage message) {
        return message.getRefundId() != null
                ? MerchantNotifyConstants.NOTIFY_TYPE_REFUND
                : MerchantNotifyConstants.NOTIFY_TYPE_PAYMENT;
    }

    private static String generateNotifyId() {
        return "MN" + IdUtil.getSnowflakeNextId();
    }

    private static TruncatedPayload truncate(String raw) {
        if (raw == null) {
            return new TruncatedPayload(null, false);
        }
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MerchantNotifyConstants.MAX_PAYLOAD_BYTES) {
            return new TruncatedPayload(raw, false);
        }
        String clipped = new String(bytes, 0, MerchantNotifyConstants.MAX_PAYLOAD_BYTES, StandardCharsets.UTF_8);
        return new TruncatedPayload(clipped + "...[truncated]", true);
    }

    private static String preview(String body) {
        if (body == null) {
            return null;
        }
        return body.length() <= 512 ? body : body.substring(0, 512);
    }

    private record TruncatedPayload(String content, boolean truncated) {
    }
}
