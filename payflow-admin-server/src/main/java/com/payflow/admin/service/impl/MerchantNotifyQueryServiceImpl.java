package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.cashier.MerchantNotify;
import com.payflow.admin.entity.cashier.MerchantNotifyAttempt;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.kit.MerchantNotifyMaskKit;
import com.payflow.admin.mapper.cashier.MerchantNotifyAttemptMapper;
import com.payflow.admin.mapper.cashier.MerchantNotifyMapper;
import com.payflow.admin.service.MerchantNotifyQueryService;
import com.payflow.admin.service.OrderService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantNotifyQueryServiceImpl implements MerchantNotifyQueryService {

    private final MerchantNotifyMapper merchantNotifyMapper;
    private final MerchantNotifyAttemptMapper merchantNotifyAttemptMapper;
    private final OrderService orderService;

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
        return raw.convert(this::toListItem);
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

    private Map<String, Object> toListItem(MerchantNotify summary) {
        Order order = orderService.getByOrderId(summary.getOrderId());
        return toListItem(summary, order != null ? order.getStatus() : summary.getOrderStatusSnapshot());
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
}
