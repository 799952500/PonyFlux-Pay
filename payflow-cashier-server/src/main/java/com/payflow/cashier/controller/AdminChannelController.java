package com.payflow.cashier.controller;

import com.payflow.cashier.dto.ChannelAccountDTO;
import com.payflow.cashier.dto.ChannelRouteDTO;
import com.payflow.cashier.entity.PayChannel;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.PayChannelMerchantRoute;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.service.PayChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付渠道管理 Admin API
 *
 * @author PayFlow Team
 */
@RestController
@RequestMapping("/api/v1/admin/channels")
@RequiredArgsConstructor
public class AdminChannelController {

    private final PayChannelService payChannelService;

    // ==================== 渠道管理 ====================

    @GetMapping
    public R<List<PayChannel>> listChannels() {
        return R.ok(payChannelService.getAllChannels());
    }

    @PostMapping
    public R<PayChannel> createChannel(@RequestBody PayChannel channel) {
        return payChannelService.createChannel(channel);
    }

    @PutMapping("/{id}")
    public R<PayChannel> updateChannel(@PathVariable Long id, @RequestBody PayChannel channel) {
        return payChannelService.updateChannel(id, channel);
    }

    @PutMapping("/{id}/toggle")
    public R<Void> toggleChannel(@PathVariable Long id) {
        return payChannelService.toggleChannelStatus(id);
    }

    // ==================== 渠道账户管理 ====================

    @GetMapping("/{channelId}/accounts")
    public R<List<ChannelAccountDTO>> listAccounts(@PathVariable Long channelId) {
        return R.ok(payChannelService.getAccountsByChannel(channelId));
    }

    @PostMapping("/accounts")
    public R<PayChannelAccount> createAccount(@RequestBody PayChannelAccount account) {
        return payChannelService.createAccount(account);
    }

    @PutMapping("/accounts/{id}")
    public R<PayChannelAccount> updateAccount(@PathVariable Long id, @RequestBody PayChannelAccount account) {
        return payChannelService.updateAccount(id, account);
    }

    @PutMapping("/accounts/{id}/toggle")
    public R<Void> toggleAccount(@PathVariable Long id) {
        return payChannelService.toggleAccountStatus(id);
    }

    @DeleteMapping("/accounts/{id}")
    public R<Void> deleteAccount(@PathVariable Long id) {
        return payChannelService.deleteAccount(id);
    }

    // ==================== 商户路由管理 ====================

    @GetMapping("/routes")
    public R<List<ChannelRouteDTO>> listRoutes(@RequestParam(required = false) String merchantId) {
        return R.ok(payChannelService.getRoutesByMerchant(merchantId));
    }

    @PostMapping("/routes")
    public R<PayChannelMerchantRoute> createRoute(@RequestBody PayChannelMerchantRoute route) {
        return payChannelService.createRoute(route);
    }

    @DeleteMapping("/routes/{routeId}")
    public R<Void> deleteRoute(@PathVariable Long routeId) {
        return payChannelService.deleteRoute(routeId);
    }

    @PutMapping("/routes/{routeId}/toggle")
    public R<Void> toggleRoute(@PathVariable Long routeId) {
        return payChannelService.toggleRouteEnabled(routeId);
    }
}
