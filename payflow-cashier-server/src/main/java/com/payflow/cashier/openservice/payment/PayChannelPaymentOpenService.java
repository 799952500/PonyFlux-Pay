package com.payflow.cashier.openservice.payment;

import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.RefundResult;

import java.util.Map;

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
    /**
     * 发起支付（无渠道扩展参数时等价于 {@link #pay(String, Long, String, String, String, String, PayChannelAccount, Map)} 且 extras 为空）。
     */
    default PayResult pay(String orderId,
                          Long amount,
                          String subject,
                          String payMethod,
                          String returnUrl,
                          String notifyUrl,
                          PayChannelAccount account) {
        return pay(orderId, amount, subject, payMethod, returnUrl, notifyUrl, account, Map.of());
    }

    /**
     * @param channelExtras 渠道扩展参数（如微信 openid、付款码 auth_code），键名由具体策略约定
     */
    PayResult pay(String orderId,
                  Long amount,
                  String subject,
                  String payMethod,
                  String returnUrl,
                  String notifyUrl,
                  PayChannelAccount account,
                  Map<String, String> channelExtras);

    /**
     * 发起渠道退款。
     *
     * @param orderId      平台订单号（微信退款需要 outTradeNo）
     * @param refundId     商户侧退款单号
     * @param refundAmount 退款金额（分）
     * @param totalAmount  原支付总金额（分）
     * @param reason       退款原因
     * @param account      渠道账号配置
     * @return 退款结果
     */
    RefundResult refund(String orderId, String refundId,
                        Long refundAmount, Long totalAmount,
                        String reason, PayChannelAccount account);

    /**
     * 向渠道查询订单支付状态（超时关单、MQ 兜底共用）。
     */
    default ChannelOrderQueryResult queryOrder(String orderId, PayChannelAccount account) {
        return ChannelOrderQueryResult.unsupported("当前渠道未实现主动查单");
    }
}

