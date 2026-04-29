package com.payflow.cashier.openservice.payment;

import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.payment.core.PayResult;

/**
 * 支付下单开放服务：按渠道维度封装支付下单能力。
 * <p>
 * 约定 Bean 命名：{channelCode}PaymentOpenService，例如：
 * <ul>
 *     <li>alipayPaymentOpenService</li>
 *     <li>wxpayPaymentOpenService</li>
 * </ul>
 * </p>
 *
 * @author Lucas
 */
public interface PayChannelPaymentOpenService {

    /**
     * 当前服务对应的渠道编码（小写）。
     *
     * @return 渠道编码（例如 alipay / wxpay）
     */
    String channelCode();

    /**
     * 发起支付下单（生成二维码/跳转链接/表单/APP调起参数等）。
     *
     * @param orderId 平台订单号
     * @param amount 支付金额（分）
     * @param subject 标题
     * @param payMethod 支付方式编码（例如 WECHAT_NATIVE / ALIPAY_WAP）
     * @param returnUrl 渠道支付完成后回跳地址（回到收银台页面）
     * @param notifyUrl 渠道异步通知地址（支付机构回调）
     * @param account 渠道账号配置
     * @return 下单结果
     */
    PayResult pay(String orderId,
                  Long amount,
                  String subject,
                  String payMethod,
                  String returnUrl,
                  String notifyUrl,
                  PayChannelAccount account);
}

