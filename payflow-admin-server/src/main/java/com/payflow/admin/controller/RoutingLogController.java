package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.entity.RoutingDecisionLog;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.mapper.RoutingDecisionLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 路由决策日志查询 Controller
 *
 * @author PayFlow Team
 */
@Tag(name = "路由决策日志")
@RestController
@RequestMapping("/api/v1/admin/routing-logs")
@RequiredArgsConstructor
public class RoutingLogController {

    private final RoutingDecisionLogMapper routingDecisionLogMapper;
    private final MerchantMapper merchantMapper;

    @Operation(summary = "路由决策日志列表")
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tradeNo,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String selectedChannel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<String> scope = AdminRequestContext.merchantScope(request);
        List<Long> scopeDbIds = resolveScopeDbIds(scope);
        if (scope != null && scopeDbIds.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "code", 0, "message", "success",
                    "data", Map.of("list", List.of(), "total", 0, "page", page, "size", size)));
        }

        LambdaQueryWrapper<RoutingDecisionLog> wrapper = new LambdaQueryWrapper<>();
        if (tradeNo != null && !tradeNo.isEmpty()) {
            wrapper.eq(RoutingDecisionLog::getTradeNo, tradeNo);
        }
        if (merchantId != null) {
            if (scopeDbIds != null && !scopeDbIds.contains(merchantId)) {
                return ResponseEntity.ok(Map.of(
                        "code", 0, "message", "success",
                        "data", Map.of("list", List.of(), "total", 0, "page", page, "size", size)));
            }
            wrapper.eq(RoutingDecisionLog::getMerchantId, merchantId);
        } else if (scopeDbIds != null) {
            wrapper.in(RoutingDecisionLog::getMerchantId, scopeDbIds);
        }
        if (selectedChannel != null && !selectedChannel.isEmpty()) {
            wrapper.eq(RoutingDecisionLog::getSelectedChannel, selectedChannel);
        }
        if (startTime != null) {
            wrapper.ge(RoutingDecisionLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(RoutingDecisionLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(RoutingDecisionLog::getCreateTime);

        IPage<RoutingDecisionLog> result = routingDecisionLogMapper.selectPage(new Page<>(page, size), wrapper);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of(
                        "list", result.getRecords(),
                        "total", result.getTotal(),
                        "page", result.getCurrent(),
                        "size", result.getSize()
                )
        ));
    }

    @Operation(summary = "导出路由决策日志")
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<Long> scopeDbIds = resolveScopeDbIds(AdminRequestContext.merchantScope(request));
        if (scopeDbIds != null && scopeDbIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", List.of()));
        }
        LambdaQueryWrapper<RoutingDecisionLog> wrapper = new LambdaQueryWrapper<>();
        if (scopeDbIds != null) {
            wrapper.in(RoutingDecisionLog::getMerchantId, scopeDbIds);
        }
        if (startTime != null) {
            wrapper.ge(RoutingDecisionLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(RoutingDecisionLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(RoutingDecisionLog::getCreateTime).last("LIMIT 10000");
        var list = routingDecisionLogMapper.selectList(wrapper);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", list));
    }

    private List<Long> resolveScopeDbIds(List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return null;
        }
        if (merchantScopeIds.isEmpty()) {
            return List.of();
        }
        return merchantMapper.selectList(new LambdaQueryWrapper<Merchant>()
                        .in(Merchant::getMerchantId, merchantScopeIds))
                .stream()
                .map(Merchant::getId)
                .toList();
    }
}
