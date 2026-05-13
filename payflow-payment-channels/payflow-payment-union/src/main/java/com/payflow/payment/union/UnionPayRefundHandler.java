package com.payflow.payment.union;

import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 银联退款处理器：构建退款请求，调用后台交易发起退款。
 *
 * @author PayFlow Team
 */
@Slf4j
public class UnionPayRefundHandler {

    private static final DateTimeFormatter TXN_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 退款结果。
     */
    public record RefundResult(String queryId) {}

    /**
     * 发起银联退款（退货交易）。
     *
     * @param orderId       商户订单号
     * @param origQryId     原交易流水号（银联 queryId）
     * @param refundAmount  退款金额（分）
     * @param reason        退款原因
     * @param config        银联账号配置
     * @return 退款结果（含银联退款流水号）
     */
    public RefundResult refund(String orderId, String origQryId,
                               Long refundAmount, String reason,
                               UnionPayAccountConfig config) {
        UnionPayHttpClient client = new UnionPayHttpClient(config);

        Map<String, String> bizParams = new HashMap<>();
        bizParams.put("txnType", UnionPayApiConstants.TXN_TYPE_REFUND);
        bizParams.put("txnSubType", UnionPayApiConstants.TXN_SUB_TYPE_DEFAULT);
        bizParams.put("bizType", UnionPayApiConstants.BIZ_TYPE_DEFAULT);
        bizParams.put("orderId", orderId);
        bizParams.put("origQryId", origQryId);
        bizParams.put("txnAmt", refundAmount != null ? refundAmount.toString() : "0");
        bizParams.put("txnTime", LocalDateTime.now().format(TXN_TIME_FMT));

        Map<String, String> resp = client.backTrans(bizParams);

        String respCode = resp.get("respCode");
        String respMsg = resp.get("respMsg");

        if (!UnionPayApiConstants.RESP_CODE_SUCCESS.equals(respCode)) {
            log.error("银联退款失败: orderId={}, origQryId={}, respCode={}, respMsg={}",
                    orderId, origQryId, respCode, respMsg);
            throw new BizException(6102, "银联退款失败: " + respMsg);
        }

        String queryId = resp.get("queryId");
        log.info("银联退款成功: orderId={}, origQryId={}, refundQueryId={}", orderId, origQryId, queryId);
        return new RefundResult(queryId);
    }
}
