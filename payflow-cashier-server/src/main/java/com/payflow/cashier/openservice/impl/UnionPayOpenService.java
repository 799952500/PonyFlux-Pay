package com.payflow.cashier.openservice.impl;

import com.payflow.cashier.openservice.PayChannelOpenService;
import com.payflow.cashier.sdk.unionpay.UnionPayNotifyHelper;
import com.payflow.payment.core.NotifyResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 银联开放服务。
 * <p>
 * Bean 命名约定：unionpayOpenService（用于统一回调入口按"渠道 + OpenService"动态定位）。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Service("unionpayOpenService")
@RequiredArgsConstructor
public class UnionPayOpenService implements PayChannelOpenService {

    private final UnionPayNotifyHelper unionPayNotifyHelper;

    @Override
    public String channelCode() {
        return "unionpay";
    }

    @Override
    public NotifyHandleResult parseAndHandleNotify(HttpServletRequest request) {
        NotifyResult r = unionPayNotifyHelper.parseNotify(flattenParams(request));
        String reply = r.isSuccess() ? "SUCCESS" : "FAIL";
        return new NotifyHandleResult(channelCode(), r.getOutTradeNo(), reply);
    }

    /**
     * 将银联回调的多值参数扁平化为单值 Map。
     */
    private Map<String, String> flattenParams(HttpServletRequest request) {
        Map<String, String> flat = new HashMap<>();
        Map<String, String[]> pm = request.getParameterMap();
        for (Map.Entry<String, String[]> e : pm.entrySet()) {
            String[] v = e.getValue();
            if (v == null || v.length == 0) {
                continue;
            }
            flat.put(e.getKey(), v[0]);
        }
        return flat;
    }
}
