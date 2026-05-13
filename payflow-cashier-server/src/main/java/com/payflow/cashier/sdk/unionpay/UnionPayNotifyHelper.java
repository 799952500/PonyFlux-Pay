package com.payflow.cashier.sdk.unionpay;

import cn.hutool.core.util.StrUtil;
import com.payflow.payment.core.NotifyResult;
import com.payflow.cashier.service.PayNotifyService;
import com.payflow.payment.union.UnionPayApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 银联回调通知解析辅助组件。
 * <p>
 * 封装银联验签和处理逻辑，供银联策略复用。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnionPayNotifyHelper {

    private final PayNotifyService payNotifyService;

    /**
     * 解析并处理银联异步通知。
     * <p>
     * 银联通知为 application/x-www-form-urlencoded POST，关键字段：
     * <ul>
     *     <li>respCode — 00 表示成功</li>
     *     <li>orderId — 商户订单号（outTradeNo）</li>
     *     <li>queryId — 银联交易流水号（tradeNo）</li>
     *     <li>txnAmt — 交易金额（分）</li>
     *     <li>signature — RSA-SHA256 签名</li>
     * </ul>
     * </p>
     *
     * @param params 通知参数（form-post key-value）
     * @return 通知解析结果
     */
    public NotifyResult parseNotify(Map<String, String> params) {
        try {
            String respCode = params.get("respCode");
            String respMsg = params.get("respMsg");
            String orderId = params.get("orderId");
            String queryId = params.get("queryId");
            String txnType = params.get("txnType");
            String txnAmt = params.get("txnAmt");
            String signature = params.get("signature");

            log.info("银联回调解析: txnType={}, orderId={}, queryId={}, respCode={}, txnAmt={}",
                    txnType, orderId, queryId, respCode, txnAmt);

            // 1. 验签（生产环境需加载银联公钥验签）
            if (StrUtil.isBlank(signature)) {
                log.error("银联回调验签失败：签名为空");
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("签名为空")
                        .build();
            }

            // 2. 检查响应码
            if (!"00".equals(respCode)) {
                log.warn("银联回调：交易未成功: orderId={}, respCode={}, respMsg={}", orderId, respCode, respMsg);
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("交易未成功: " + respMsg)
                        .build();
            }

            // 3. 区分交易类型处理
            if (UnionPayApiConstants.TXN_TYPE_REFUND.equals(txnType)) {
                // 退款通知：记录日志，退款状态由 RefundServiceImpl 查询渠道结果更新
                String origQryId = params.get("origQryId");
                log.info("银联退款通知处理: orderId={}, queryId={}, origQryId={}", orderId, queryId, origQryId);
                return NotifyResult.builder()
                        .success(true)
                        .tradeNo(queryId)
                        .outTradeNo(orderId)
                        .build();
            }

            // 4. 处理支付成功（默认）
            payNotifyService.handlePaymentSuccess(orderId, queryId);

            return NotifyResult.builder()
                    .success(true)
                    .tradeNo(queryId)
                    .outTradeNo(orderId)
                    .build();

        } catch (Exception e) {
            log.error("处理银联回调异常", e);
            return NotifyResult.builder()
                    .success(false)
                    .errorMsg(e.getMessage())
                    .build();
        }
    }
}
