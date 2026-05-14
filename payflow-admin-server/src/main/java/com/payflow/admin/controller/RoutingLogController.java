package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.RoutingDecisionLog;
import com.payflow.admin.mapper.RoutingDecisionLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    @Operation(summary = "路由决策日志列表")
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tradeNo,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String selectedChannel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        LambdaQueryWrapper<RoutingDecisionLog> wrapper = new LambdaQueryWrapper<>();
        if (tradeNo != null && !tradeNo.isEmpty()) {
            wrapper.eq(RoutingDecisionLog::getTradeNo, tradeNo);
        }
        if (merchantId != null) {
            wrapper.eq(RoutingDecisionLog::getMerchantId, merchantId);
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        LambdaQueryWrapper<RoutingDecisionLog> wrapper = new LambdaQueryWrapper<>();
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
}
