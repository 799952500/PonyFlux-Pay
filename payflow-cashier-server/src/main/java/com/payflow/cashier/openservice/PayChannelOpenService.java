package com.payflow.cashier.openservice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付渠道开放服务：按渠道维度封装下单/回调等能力。
 * <p>
 * 约定 Bean 命名：{channelCode}OpenService，例如 alipayOpenService、wxpayOpenService。
 * </p>
 *
 * @author Lucas
 */
public interface PayChannelOpenService {

    /**
     * 当前服务对应的渠道编码（用于诊断/校验）。
     *
     * @return 渠道编码（例如 alipay / wxpay）
     */
    String channelCode();

    /**
     * 解析并处理渠道异步通知。
     *
     * @param request 回调请求
     * @return 通知处理结果（包含渠道编码、outTradeNo、reply）
     */
    NotifyHandleResult parseAndHandleNotify(HttpServletRequest request);

    /**
     * 处理渠道异步通知并返回应答内容（兼容调用）。
     *
     * @param request 回调请求
     * @return 渠道应答内容（微信 SUCCESS/FAIL；支付宝 success/fail）
     */
    default String handleNotify(HttpServletRequest request) {
        NotifyHandleResult r = parseAndHandleNotify(request);
        return r == null || r.reply() == null ? "" : r.reply();
    }

    /**
     * 通知处理结果（用于统一回调入口先定位订单，再路由到对应 OpenService）。
     *
     * @param channelCode 渠道编码（例如 alipay / wxpay）
     * @param outTradeNo  平台订单号（用作 out_trade_no）
     * @param reply       渠道应答内容
     */
    record NotifyHandleResult(String channelCode, String outTradeNo, String reply) {
    }
}

