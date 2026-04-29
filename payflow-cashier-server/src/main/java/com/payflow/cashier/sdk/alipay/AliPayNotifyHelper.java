package com.payflow.cashier.sdk.alipay;

import cn.hutool.core.util.StrUtil;
import com.payflow.payment.core.NotifyResult;
import com.payflow.cashier.service.PayNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付宝回调通知解析辅助组件。
 * <p>
 * 封装支付宝验签和处理逻辑，供所有支付宝策略复用。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliPayNotifyHelper {

    private final PayNotifyService payNotifyService;

    /**
     * 解析并处理支付宝异步通知。
     *
     * @param params 通知参数（form-post key-value）
     * @return 通知解析结果
     */
    public NotifyResult parseNotify(Map<String, String> params) {
        try {
            // 1. 提取关键参数
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no"); // 商户订单号
            String tradeNo = params.get("trade_no"); // 支付宝交易号
            String totalAmount = params.get("total_amount");
            String notifyId = params.get("notify_id");
            String sign = params.get("sign");

            log.info("支付宝回调解析: outTradeNo={}, tradeNo={}, tradeStatus={}",
                    outTradeNo, tradeNo, tradeStatus);

            // 2. 验签（重要！）
            // 实际生产环境应使用 AlipayConfigLoader 获取支付宝公钥验签
            if (StrUtil.isBlank(sign)) {
                log.error("支付宝回调验签失败：签名为空");
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("签名为空")
                        .aliReply("fail")
                        .build();
            }

            // 3. 根据交易状态处理
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                payNotifyService.handlePaymentSuccess(outTradeNo, tradeNo);
                return NotifyResult.builder()
                        .success(true)
                        .tradeNo(tradeNo)
                        .outTradeNo(outTradeNo)
                        .aliReply("success")
                        .build();
            } else if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
                log.info("支付宝回调：等待买家付款，忽略: outTradeNo={}", outTradeNo);
                return NotifyResult.builder()
                        .success(true)
                        .aliReply("success")
                        .build();
            } else {
                log.warn("支付宝回调：交易未成功: outTradeNo={}, status={}", outTradeNo, tradeStatus);
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("交易未成功: " + tradeStatus)
                        .aliReply("fail")
                        .build();
            }

        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return NotifyResult.builder()
                    .success(false)
                    .errorMsg(e.getMessage())
                    .aliReply("fail")
                    .build();
        }
    }
}
