package com.payflow.payment.alipay;

import com.payflow.payment.core.ChannelConfigHolder;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付宝支付处理器接口。
 * <p>
 * 定义支付宝 App 支付、WAP 网页支付、Native 扫码支付、退款及回调解析的
 * 统一行为规范。
 * </p>
 *
 * @author PayFlow Team
 */
public interface AliPayHandler {

    // ==================== 支付下单 ====================

    /**
     * WAP 网页支付（手机网站支付）。
     * <p>
     * 调用 alipay.trade.wap.pay 接口，生成用于 H5 页面自动提交的支付表单。
     * </p>
     *
     * @param orderId    平台订单号（用作 outTradeNo）
     * @param amount     订单金额（分），方法内部转换为元
     * @param subject    订单标题
     * @param returnUrl  支付完成跳转地址（GET 回调）
     * @param notifyUrl  异步通知地址（POST 回调）
     * @param account    渠道账户配置
     * @return 包含支付宝交易号和表单 HTML 的响应对象
     */
    AliPayOrderResponse createWapOrder(String orderId, Long amount, String subject,
                                        String returnUrl, String notifyUrl,
                                        ChannelConfigHolder account);

    /**
     * App 支付。
     * <p>
     * 调用 alipay.trade.app.pay 接口，返回App调起参数字符串。
     * </p>
     *
     * @param orderId    平台订单号（用作 outTradeNo）
     * @param amount     订单金额（分），方法内部转换为元
     * @param subject    订单标题
     * @param notifyUrl  异步通知地址（POST 回调）
     * @param account    渠道账户配置
     * @return App 调起参数字符串，格式为 "key=value&key=value..."
     */
    String createAppOrder(String orderId, Long amount, String subject,
                          String notifyUrl, ChannelConfigHolder account);

    /**
     * Native 扫码支付。
     * <p>
     * 调用 alipay.trade.precreate 接口，生成付款二维码链接。
     * </p>
     *
     * @param orderId    平台订单号（用作 outTradeNo）
     * @param amount     订单金额（分），方法内部转换为元
     * @param subject    订单标题
     * @param notifyUrl  异步通知地址（POST 回调）
     * @param account    渠道账户配置
     * @return 包含支付宝交易号和二维码链接的响应对象
     */
    AliPayQrResponse createQrOrder(String orderId, Long amount, String subject,
                                    String notifyUrl, ChannelConfigHolder account);

    // ==================== 回调与退款 ====================

    /**
     * 解析支付宝异步通知（notify_url）请求。
     * <p>
     * 从 HttpServletRequest 中提取通知参数，并做签名验证（验证签名由调用方决定是否执行）。
     * 返回原始通知参数的 JSON 字符串，便于调用方做后续业务处理。
     * </p>
     *
     * @param request HTTP 请求对象（包含支付宝通知参数）
     * @return 通知参数的 JSON 字符串
     */
    String parseNotify(HttpServletRequest request);

    /**
     * 申请退款。
     *
     * @param tradeNo       支付宝交易号（trade_no）
     * @param refundAmount  退款金额（分），方法内部转换为元
     * @param reason        退款原因
     * @param account       渠道账户配置
     * @return 退款响应对象
     */
    AliPayRefundResponse refund(String tradeNo, Long refundAmount,
                                 String reason, ChannelConfigHolder account);
}
