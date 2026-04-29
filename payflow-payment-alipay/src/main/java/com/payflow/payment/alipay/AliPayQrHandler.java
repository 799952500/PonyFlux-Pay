package com.payflow.payment.alipay;

import com.alipay.easysdk.factory.Factory;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝 Native（扫码支付）处理器。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class AliPayQrHandler {

    /**
     * 创建 Native 扫码支付订单。
     * 调用 alipay.trade.precreate 接口，生成付款二维码链接。
     *
     * @param orderId    商户订单号
     * @param amount     订单金额（分），内部转换为元
     * @param subject    订单标题
     * @param notifyUrl  异步通知地址
     * @param account    渠道账户配置
     * @return 包含商户订单号和二维码链接的响应对象
     */
    public AliPayQrResponse createQrOrder(String orderId, Long amount, String subject,
                                          String notifyUrl, ChannelConfigHolder account) {
        try {
            AliPayClientCache.configure(account);
            String totalAmount = String.valueOf(amount / 100.0);

            // FaceToFace.preCreate(subject, outTradeNo, totalAmount) + asyncNotify(url)
            var response = Factory.Payment.FaceToFace()
                    .asyncNotify(notifyUrl)
                    .preCreate(subject, orderId, totalAmount);

            // PrecreateResponse 有 code/msg/outTradeNo/qrCode 字段
            if (response.code != null && "10000".equals(response.code)) {
                log.info("支付宝Native扫码支付下单成功: orderId={}", orderId);
                return AliPayQrResponse.builder()
                        .outTradeNo(response.outTradeNo)
                        .qrCode(response.qrCode)
                        .build();
            } else {
                String errMsg = response.msg != null ? response.msg : response.httpBody;
                log.error("支付宝Native扫码支付下单失败: orderId={}, code={}, msg={}",
                        orderId, response.code, errMsg);
                throw new BizException(6006,
                        "支付宝Native扫码支付下单失败: " + errMsg);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝Native扫码支付异常: orderId={}", orderId, e);
            throw new BizException(6006,
                    "支付宝Native扫码支付异常: " + e.getMessage());
        }
    }

    /**
     * 申请支付宝退款。
     * 调用 alipay.trade.refund 接口。
     *
     * @param tradeNo       支付宝交易号（channelTransactionId）
     * @param refundAmount  退款金额（分），内部转换为元
     * @param reason        退款原因（此版本 Easysdk 不支持，通过退款原因需另处理）
     * @param account       渠道账户配置
     * @return 退款响应
     */
    public AliPayRefundResponse refund(String tradeNo, Long refundAmount,
                                       String reason, ChannelConfigHolder account) {
        try {
            AliPayClientCache.configure(account);
            String refundAmountStr = String.valueOf(refundAmount / 100.0);

            // Common.refund(outTradeNo, refundAmount) — 2 参数
            var response = Factory.Payment.Common()
                    .refund(tradeNo, refundAmountStr);

            if (response.code != null && "10000".equals(response.code)) {
                log.info("支付宝退款成功: tradeNo={}, refundAmount={}", tradeNo, refundAmountStr);
                return AliPayRefundResponse.builder()
                        .outTradeNo(response.outTradeNo)
                        .tradeNo(response.tradeNo)
                        .buyerLogonId(response.buyerLogonId)
                        .refundFee(response.refundFee)
                        .gmtRefundPay(response.gmtRefundPay)
                        .build();
            } else {
                String errMsg = response.msg != null ? response.msg : response.httpBody;
                log.error("支付宝退款失败: tradeNo={}, code={}, msg={}",
                        tradeNo, response.code, errMsg);
                throw new BizException(6009, "支付宝退款失败: " + errMsg);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝退款异常: tradeNo={}", tradeNo, e);
            throw new BizException(6009, "支付宝退款异常: " + e.getMessage());
        }
    }
}
