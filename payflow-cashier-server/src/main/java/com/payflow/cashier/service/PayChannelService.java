package com.payflow.cashier.service;

import com.payflow.cashier.dto.ChannelAccountDTO;
import com.payflow.cashier.dto.ChannelRouteDTO;
import com.payflow.cashier.entity.PayChannel;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.PayChannelMerchantRoute;
import com.payflow.cashier.exception.R;

import java.util.List;

/**
 * 支付渠道管理服务
 *
 * @author PayFlow Team
 */
public interface PayChannelService {

    // ==================== 渠道管理 ====================

    /** 获取所有渠道（含禁用的） */
    List<PayChannel> getAllChannels();

    /** 获取已启用的渠道 */
    List<PayChannel> getEnabledChannels();

    /** 创建渠道 */
    R<PayChannel> createChannel(PayChannel channel);

    /** 更新渠道 */
    R<PayChannel> updateChannel(Long id, PayChannel channel);

    /** 启用/禁用渠道 */
    R<Void> toggleChannelStatus(Long id);

    // ==================== 渠道账户管理 ====================

    /** 获取指定渠道下的所有账户 */
    List<ChannelAccountDTO> getAccountsByChannel(Long channelId);

    /** 获取商户可用的账户列表（按渠道分组） */
    List<ChannelAccountDTO> getAccountsByMerchant(String merchantId);

    /** 创建账户 */
    R<PayChannelAccount> createAccount(PayChannelAccount account);

    /** 更新账户 */
    R<PayChannelAccount> updateAccount(Long id, PayChannelAccount account);

    /** 启用/禁用账户 */
    R<Void> toggleAccountStatus(Long id);

    /** 删除账户（软删除） */
    R<Void> deleteAccount(Long id);

    // ==================== 商户路由管理 ====================

    /** 获取商户的路由配置 */
    List<ChannelRouteDTO> getRoutesByMerchant(String merchantId);

    /** 创建路由 */
    R<PayChannelMerchantRoute> createRoute(PayChannelMerchantRoute route);

    /** 删除路由 */
    R<Void> deleteRoute(Long routeId);

    /** 启用/禁用路由 */
    R<Void> toggleRouteEnabled(Long routeId);

    // ==================== 核心业务 ====================

    /**
     * 路由到账户（收银台调用）
     *
     * @param merchantId   商户号
     * @param channelCode  渠道编码
     * @return 匹配账户，未配置则返回 null
     */
    PayChannelAccount routeToAccount(String merchantId, String channelCode);
}
