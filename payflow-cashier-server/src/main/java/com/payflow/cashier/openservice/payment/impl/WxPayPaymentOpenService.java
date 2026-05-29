package com.payflow.cashier.openservice.payment.impl;

import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.openservice.payment.ChannelOrderQueryResult;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenService;
import com.payflow.cashier.sdk.PayStrategyLocator;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import com.payflow.payment.wechat.WxPayNativeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 微信支付下单开放服务。
 *
 * @author Lucas
 */
@Slf4j
@Service("wxpayPaymentOpenService")
@RequiredArgsConstructor
public class WxPayPaymentOpenService implements PayChannelPaymentOpenService {

    /** 支付策略定位器：按 payMethod 定位具体策略（WECHAT_NATIVE/WECHAT_H5/WECHAT_APP） */
    private final PayStrategyLocator payStrategyLocator;
    private final WxPayNativeHandler wxPayNativeHandler;

    /**
     * 微信渠道编码（小写）。
     *
     * @return wxpay
     */
    @Override
    public String channelCode() {
        return "wxpay";
    }

    /**
     * 发起微信下单。
     *
     * @param orderId 平台订单号
     * @param amount 支付金额（分）
     * @param subject 标题
     * @param payMethod 支付方式编码
     * @param returnUrl 回跳地址
     * @param notifyUrl 异步通知地址
     * @param account 渠道账号配置
     * @return 下单结果
     */
    @Override
    public PayResult pay(String orderId,
                         Long amount,
                         String subject,
                         String payMethod,
                         String returnUrl,
                         String notifyUrl,
                         PayChannelAccount account,
                         java.util.Map<String, String> channelExtras) {
        PayMethod methodEnum = PayMethod.fromCode(payMethod);
        if (methodEnum == null) {
            throw new BizException(6007, "不支持的支付方式: " + payMethod);
        }
        if (!methodEnum.getCode().startsWith("WECHAT_")) {
            throw new BizException(7103, "支付方式与渠道不匹配: channel=WECHAT_PAY, payMethod=" + payMethod);
        }

        PayStrategy strategy = payStrategyLocator.requireByPayMethodCode(payMethod);
        PayResult result = strategy.pay(orderId, amount, subject, returnUrl, notifyUrl, account, channelExtras);
        log.info("微信下单完成: orderId={}, payMethod={}, action={}", orderId, payMethod, result.getAction());
        return result;
    }

    @Override
    public ChannelOrderQueryResult queryOrder(String orderId, PayChannelAccount account) {
        boolean paid = wxPayNativeHandler.queryOutTradeNoSuccess(orderId, account);
        return ChannelOrderQueryResult.of(paid, null,
                paid ? "微信侧订单已支付" : "微信侧订单未支付或查单失败");
    }

    /**
     * 微信渠道退款：委托默认策略（WECHAT_NATIVE）执行退款。
     *
     * @param orderId      平台订单号
     * @param refundId     商户侧退款单号
     * @param refundAmount 退款金额（分）
     * @param totalAmount  原支付总金额（分）
     * @param reason       退款原因
     * @param account      渠道账号配置
     * @return 退款结果
     */
    @Override
    public RefundResult refund(String orderId, String refundId,
                               Long refundAmount, Long totalAmount,
                               String reason, PayChannelAccount account) {
        PayStrategy strategy = payStrategyLocator.requireByPayMethodCode(PayMethod.WECHAT_NATIVE.getCode());
        RefundResult result = strategy.refund(orderId, refundAmount, reason, account);
        log.info("微信退款完成: orderId={}, refundId={}, success={}", orderId, refundId, result.isSuccess());
        return result;
    }
}

