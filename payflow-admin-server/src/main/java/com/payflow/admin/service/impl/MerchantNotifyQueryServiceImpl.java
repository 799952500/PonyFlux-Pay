package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.cashier.MerchantNotify;
import com.payflow.admin.entity.cashier.MerchantNotifyAttempt;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.kit.MerchantNotifyMaskKit;
import com.payflow.admin.mapper.cashier.MerchantNotifyAttemptMapper;
import com.payflow.admin.mapper.cashier.MerchantNotifyMapper;
import com.payflow.admin.service.MerchantNotifyQueryService;
import com.payflow.admin.service.NotificationService;
import com.payflow.admin.service.OrderService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantNotifyQueryServiceImpl implements MerchantNotifyQueryService {

    private static final int WEBHOOK_FAIL_THRESHOLD = 5;

    private final MerchantNotifyMapper merchantNotifyMapper;
    private final MerchantNotifyAttemptMapper merchantNotifyAttemptMapper;
    private final OrderService orderService;
    private final NotificationService notificationService;

    @Override
    public IPage<Map<String, Object>> page(int pageNum, int pageSize,
                                           String merchantId, String orderId, String merchantOrderNo,
                                           String notifyType, String summaryStatus,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           List<String> merchantScopeIds) {
        int size = Math.min(Math.max(pageSize, 1), 100);
        if (merchantScopeIds != null && merchantScopeIds.isEmpty()) {
            return emptyPage(pageNum, size);
        }

        LambdaQueryWrapper<MerchantNotify> wrapper = buildScopeWrapper(
                merchantId, orderId, merchantOrderNo, notifyType, summaryStatus,
                startTime, endTime, merchantScopeIds);
        if (wrapper == null) {
            return emptyPage(pageNum, size);
        }

        Page<MerchantNotify> page = new Page<>(pageNum, size);
        wrapper.orderByDesc(MerchantNotify::getLastAttemptAt).orderByDesc(MerchantNotify::getUpdatedAt);
        IPage<MerchantNotify> raw = merchantNotifyMapper.selectPage(page, wrapper);
        List<String> orderIds = raw.getRecords().stream()
                .map(MerchantNotify::getOrderId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        Map<String, Order> orderMap = orderService.mapByOrderIds(orderIds);
        IPage<Map<String, Object>> converted = raw.convert(n -> {
            Order order = orderMap.get(n.getOrderId());
            return toListItem(n, order != null ? order.getStatus() : n.getOrderStatusSnapshot());
        });
        for (MerchantNotify notify : raw.getRecords()) {
            notifyWebhookFailureIfNeeded(notify);
        }
        return converted;
    }

    @Override
    public Map<String, Object> getDetail(String notifyId, List<String> merchantScopeIds) {
        MerchantNotify summary = requireSummaryInScope(notifyId, merchantScopeIds);
        Order order = orderService.getByOrderId(summary.getOrderId());
        List<MerchantNotifyAttempt> attempts = merchantNotifyAttemptMapper.selectList(
                new LambdaQueryWrapper<MerchantNotifyAttempt>()
                        .eq(MerchantNotifyAttempt::getNotifyId, notifyId)
                        .orderByAsc(MerchantNotifyAttempt::getAttemptNo));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", toSummaryMap(summary, order != null ? order.getStatus() : summary.getOrderStatusSnapshot()));
        List<Map<String, Object>> attemptRows = new ArrayList<>();
        for (MerchantNotifyAttempt attempt : attempts) {
            attemptRows.add(toAttemptMap(attempt));
        }
        data.put("attempts", attemptRows);
        return data;
    }

    @Override
    public Map<String, Object> getByOrder(String orderId, String notifyType, List<String> merchantScopeIds) {
        Order order = getOrderIfAllowed(orderId, merchantScopeIds);
        LambdaQueryWrapper<MerchantNotify> wrapper = new LambdaQueryWrapper<MerchantNotify>()
                .eq(MerchantNotify::getOrderId, orderId);
        if (StringUtils.hasText(notifyType)) {
            wrapper.eq(MerchantNotify::getNotifyType, notifyType.trim());
        }
        wrapper.orderByAsc(MerchantNotify::getNotifyType);
        List<MerchantNotify> summaries = merchantNotifyMapper.selectList(wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", orderId);
        data.put("orderStatus", order.getStatus());
        List<Map<String, Object>> items = summaries.stream()
                .map(s -> toListItem(s, order.getStatus()))
                .toList();
        data.put("summaries", items);
        return data;
    }

    @Override
    public MerchantNotify getSummaryEntity(String notifyId) {
        return merchantNotifyMapper.selectOne(
                new LambdaQueryWrapper<MerchantNotify>().eq(MerchantNotify::getNotifyId, notifyId));
    }

    @Override
    public Order getOrderIfAllowed(String orderId, List<String> merchantScopeIds) {
        Order order = orderService.getByOrderId(orderId);
        if (order == null) {
            throw new BizException(6101, "无权访问该资源");
        }
        AdminRequestContext.assertMerchantAllowed(order.getMerchantId(), merchantScopeIds);
        return order;
    }

    private MerchantNotify requireSummaryInScope(String notifyId, List<String> merchantScopeIds) {
        MerchantNotify summary = getSummaryEntity(notifyId);
        if (summary == null) {
            throw new BizException(6101, "无权访问该资源");
        }
        AdminRequestContext.assertMerchantAllowed(summary.getMerchantId(), merchantScopeIds);
        return summary;
    }

    private LambdaQueryWrapper<MerchantNotify> buildScopeWrapper(
            String merchantId, String orderId, String merchantOrderNo,
            String notifyType, String summaryStatus,
            LocalDateTime startTime, LocalDateTime endTime,
            List<String> merchantScopeIds) {
        String merchantFilter = AdminRequestContext.resolveMerchantFilter(merchantId, merchantScopeIds);
        if ("__NO_ACCESS__".equals(merchantFilter)) {
            return null;
        }

        LambdaQueryWrapper<MerchantNotify> wrapper = new LambdaQueryWrapper<>();
        if (merchantFilter != null) {
            wrapper.eq(MerchantNotify::getMerchantId, merchantFilter);
        } else if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            wrapper.in(MerchantNotify::getMerchantId, merchantScopeIds);
        }
        if (StringUtils.hasText(orderId)) {
            wrapper.eq(MerchantNotify::getOrderId, orderId.trim());
        }
        if (StringUtils.hasText(merchantOrderNo)) {
            wrapper.eq(MerchantNotify::getMerchantOrderNo, merchantOrderNo.trim());
        }
        if (StringUtils.hasText(notifyType)) {
            wrapper.eq(MerchantNotify::getNotifyType, notifyType.trim());
        }
        if (StringUtils.hasText(summaryStatus)) {
            wrapper.eq(MerchantNotify::getSummaryStatus, summaryStatus.trim());
        }
        if (startTime != null) {
            wrapper.ge(MerchantNotify::getLastAttemptAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(MerchantNotify::getLastAttemptAt, endTime);
        }
        return wrapper;
    }

    private Map<String, Object> toListItem(MerchantNotify summary, String orderStatus) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("notifyId", summary.getNotifyId());
        row.put("orderId", summary.getOrderId());
        row.put("merchantId", summary.getMerchantId());
        row.put("merchantOrderNo", summary.getMerchantOrderNo());
        row.put("notifyType", summary.getNotifyType());
        row.put("notifyUrl", summary.getNotifyUrl());
        row.put("summaryStatus", summary.getSummaryStatus());
        row.put("attemptCount", summary.getAttemptCount());
        row.put("lastAttemptAt", summary.getLastAttemptAt());
        row.put("lastFailReason", summary.getLastFailReason());
        row.put("lastResponsePreview", summary.getLastResponsePreview());
        row.put("orderStatus", orderStatus);
        row.put("notifyPayloadStatus", summary.getNotifyPayloadStatus());
        return row;
    }

    private Map<String, Object> toSummaryMap(MerchantNotify summary, String orderStatus) {
        Map<String, Object> row = toListItem(summary, orderStatus);
        row.put("createdAt", summary.getCreatedAt());
        row.put("updatedAt", summary.getUpdatedAt());
        return row;
    }

    private static Page<Map<String, Object>> emptyPage(int pageNum, int pageSize) {
        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        page.setTotal(0);
        page.setRecords(List.of());
        return page;
    }

    private Map<String, Object> toAttemptMap(MerchantNotifyAttempt attempt) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("attemptNo", attempt.getAttemptNo());
        row.put("resultStatus", attempt.getResultStatus());
        row.put("failReasonType", attempt.getFailReasonType());
        row.put("failReasonDetail", attempt.getFailReasonDetail());
        row.put("httpStatus", attempt.getHttpStatus());
        row.put("durationMs", attempt.getDurationMs());
        row.put("requestParams", MerchantNotifyMaskKit.maskRequestParams(attempt.getRequestParams()));
        row.put("responseBody", attempt.getResponseBody());
        row.put("truncated", Boolean.TRUE.equals(attempt.getTruncated()));
        row.put("createdAt", attempt.getCreatedAt());
        return row;
    }

    /**
     * 连续失败次数达到阈值时发送站内通知（幂等）。
     */
    private void notifyWebhookFailureIfNeeded(MerchantNotify notify) {
        if (notify.getAttemptCount() == null || notify.getAttemptCount() < WEBHOOK_FAIL_THRESHOLD) {
            return;
        }
        if (!"FAILED".equals(notify.getSummaryStatus())) {
            return;
        }
        try {
            String bizKey = "WEBHOOK-" + notify.getNotifyId();
            String title = "商户回调连续失败";
            String summary = "商户 " + notify.getMerchantId() + " 的回调通知（订单 "
                    + notify.getOrderId() + "）已连续失败 " + notify.getAttemptCount() + " 次";
            String link = "/admin/merchant-notifies?merchantId=" + notify.getMerchantId();
            notificationService.sendToRole(
                    NotificationTypeEnum.WEBHOOK_FAILURE,
                    bizKey, title, summary, link,
                    notify.getMerchantId(),
                    "merchant:manage");
        } catch (Exception e) {
            log.error("发送 Webhook 失败通知异常: notifyId={}", notify.getNotifyId(), e);
        }
    }
}
