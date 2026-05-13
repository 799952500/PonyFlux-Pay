package com.payflow.payment.union;

import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 银联扫码支付处理器：构建 QR 支付请求，调用后台交易获取 QR 码。
 *
 * @author PayFlow Team
 */
@Slf4j
public class UnionPayQrHandler {

    private static final DateTimeFormatter TXN_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 扫码支付结果。
     */
    public record QrPayResult(String qrCode, String queryId) {}

    /**
     * 构建扫码支付请求，返回 QR 码 URL 和交易流水号。
     *
     * @param orderId    商户订单号
     * @param amount     支付金额（分）
     * @param subject    商品描述
     * @param notifyUrl  异步通知地址（backUrl）
     * @param config     银联账号配置
     * @return QR 支付结果（qrCode, queryId）
     */
    public QrPayResult pay(String orderId, Long amount, String subject,
                           String notifyUrl, UnionPayAccountConfig config) {
        UnionPayHttpClient client = new UnionPayHttpClient(config);

        Map<String, String> bizParams = new HashMap<>();
        bizParams.put("txnType", UnionPayApiConstants.TXN_TYPE_PAY);
        bizParams.put("txnSubType", UnionPayApiConstants.TXN_SUB_TYPE_QR);
        bizParams.put("bizType", UnionPayApiConstants.BIZ_TYPE_DEFAULT);
        bizParams.put("orderId", orderId);
        bizParams.put("txnAmt", amount != null ? amount.toString() : "0");
        bizParams.put("txnTime", LocalDateTime.now().format(TXN_TIME_FMT));
        bizParams.put("backUrl", notifyUrl);

        Map<String, String> resp = client.backTrans(bizParams);

        String respCode = resp.get("respCode");
        String respMsg = resp.get("respMsg");

        if (!UnionPayApiConstants.RESP_CODE_SUCCESS.equals(respCode)) {
            log.error("银联扫码下单失败: orderId={}, respCode={}, respMsg={}", orderId, respCode, respMsg);
            throw new BizException(6101, "银联扫码下单失败: " + respMsg);
        }

        String qrCode = resp.get("qrCode");
        String queryId = resp.get("queryId");

        log.info("银联扫码下单成功: orderId={}, queryId={}", orderId, queryId);
        return new QrPayResult(qrCode, queryId);
    }
}
