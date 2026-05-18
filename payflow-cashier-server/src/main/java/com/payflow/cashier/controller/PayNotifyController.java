package com.payflow.cashier.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.context.MerchantScopeHolder;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.openservice.PayChannelOpenService;
import com.payflow.cashier.openservice.PayChannelOpenServiceLocator;
import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 支付回调统一入口：按订单渠道路由到 {channel}OpenService（如 alipayOpenService、wxpayOpenService）。
 *
 * @author Lucas
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/callbacks", "/notify"})
@RequiredArgsConstructor
public class PayNotifyController {

    /** 订单数据访问：用于根据 outTradeNo 定位订单与渠道 */
    private final OrderMapper orderMapper;

    /** 渠道开放服务定位器：根据订单渠道获取对应的 OpenService */
    private final PayChannelOpenServiceLocator openServiceLocator;

    /**
     * 统一支付回调入口。
     * <p>
     * 兼容旧路径：当支付机构后台无法配置“带渠道编码”的回调路径时才使用。
     * </p>
     *
     * @param request 回调请求
     * @return 渠道应答（微信 SUCCESS/FAIL；支付宝 success/fail）
     */
    @PostMapping
    public String handleNotify(HttpServletRequest request) {
        throw new BizException(7000, "不支持的回调路径：请使用 /notify/{channelCode} 或 /notify/order/{outTradeNo}");
    }

    /**
     * 按“渠道编码”接收回调（推荐）。
     * <p>
     * 例子：/notify/wxpay、/notify/alipay
     * </p>
     *
     * @param request 回调请求
     * @param channelCode 渠道编码（wxpay/alipay）
     * @return 渠道应答（微信 SUCCESS/FAIL；支付宝 success/fail）
     */
    @PostMapping("/{channelCode}")
    public String handleNotifyByChannel(HttpServletRequest request, @PathVariable String channelCode) {
        return MerchantScopeHolder.callInSystemMode(() -> handleNotifyByChannelInternal(request, channelCode));
    }

    private String handleNotifyByChannelInternal(HttpServletRequest request, String channelCode) {
        // 0) 约束：channelCode 必须为小写（避免出现 WxPay/AliPay 等多种写法导致配置混乱）
        if (channelCode == null || channelCode.isBlank()) {
            throw new BizException(7004, "回调路径缺少 channelCode");
        }
        if (!channelCode.equals(channelCode.toLowerCase())) {
            throw new BizException(7005, "channelCode 必须为小写: " + channelCode);
        }

        // 1) 渠道编码 -> OpenService（无需轮询解析）
        PayChannelOpenService svc = openServiceLocator.requireByChannelCode(channelCode);

        // 2) 由渠道服务解析并处理回调，拿到 outTradeNo 与 reply
        PayChannelOpenService.NotifyHandleResult result = svc.parseAndHandleNotify(request);
        if (result == null || result.outTradeNo() == null || result.outTradeNo().isBlank()) {
            throw new BizException(7001, "回调解析失败：无法提取 outTradeNo");
        }

        // 3) outTradeNo -> 订单 -> 渠道（用于一致性校验，避免配置错误把 wxpay 回调打到 alipay 路径）
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, result.outTradeNo()));
        if (order == null) {
            throw new BizException(7002, "订单不存在: " + result.outTradeNo());
        }
        PayChannelOpenService expectedSvc = openServiceLocator.requireByOrderChannel(order.getChannel());
        if (!Objects.equals(expectedSvc.channelCode().toLowerCase(), svc.channelCode().toLowerCase())) {
            throw new BizException(7003, "回调渠道与订单渠道不一致: orderChannel=" + order.getChannel()
                    + ", notifyChannel=" + channelCode);
        }

        // 4) 返回 reply（已由渠道服务生成，符合各渠道协议）
        log.info("按渠道回调处理完成: channelCode={}, orderId={}, reply={}", channelCode, result.outTradeNo(), result.reply());
        return result.reply();
    }

    /**
     * 按“订单号”接收回调（可选）。
     * <p>
     * 例子：/notify/order/ORD20260420003
     * </p>
     *
     * @param request 回调请求
     * @param outTradeNo 平台订单号（即 out_trade_no）
     * @return 渠道应答（微信 SUCCESS/FAIL；支付宝 success/fail）
     */
    @PostMapping("/order/{outTradeNo}")
    public String handleNotifyByOrder(HttpServletRequest request, @PathVariable String outTradeNo) {
        return MerchantScopeHolder.callInSystemMode(() -> handleNotifyByOrderInternal(request, outTradeNo));
    }

    private String handleNotifyByOrderInternal(HttpServletRequest request, String outTradeNo) {
        // 1) 订单号 -> 订单 -> 渠道
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderId, outTradeNo));
        if (order == null) {
            throw new BizException(7002, "订单不存在: " + outTradeNo);
        }

        // 2) 渠道 -> OpenService（无需轮询解析）
        PayChannelOpenService svc = openServiceLocator.requireByOrderChannel(order.getChannel());

        // 3) 处理回调并返回 reply
        String reply = svc.handleNotify(request);
        log.info("按订单回调处理完成: orderId={}, channel={}, reply={}", outTradeNo, order.getChannel(), reply);
        return reply;
    }
}
