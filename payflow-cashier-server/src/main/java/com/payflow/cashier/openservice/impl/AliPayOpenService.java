package com.payflow.cashier.openservice.impl;

import com.payflow.cashier.openservice.PayChannelOpenService;
import com.payflow.cashier.sdk.alipay.AliPayNotifyHelper;
import com.payflow.payment.core.NotifyResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝开放服务。
 * <p>
 * Bean 命名约定：alipayOpenService（用于统一回调入口按“渠道 + OpenService”动态定位）。
 * </p>
 *
 * @author Lucas
 */
@Slf4j
@Service("alipayOpenService")
@RequiredArgsConstructor
public class AliPayOpenService implements PayChannelOpenService {

    /** 支付宝通知解析与业务处理辅助组件（内部会驱动订单状态更新等逻辑） */
    private final AliPayNotifyHelper aliPayNotifyHelper;

    /**
     * 渠道编码：用于与统一入口的路由结果做一致性校验。
     *
     * @return alipay
     */
    @Override
    public String channelCode() {
        return "alipay";
    }

    /**
     * 解析并处理支付宝回调。
     * <p>
     * 说明：支付宝回调参数是 form-post，需要将 request.getParameterMap 扁平化为 key-value。
     * </p>
     *
     * @param request 回调请求
     * @return 处理结果（outTradeNo + reply）
     */
    @Override
    public NotifyHandleResult parseAndHandleNotify(HttpServletRequest request) {
        NotifyResult r = aliPayNotifyHelper.parseNotify(flattenParams(request));
        String reply = r.getAliReply() == null ? "fail" : r.getAliReply();
        return new NotifyHandleResult(channelCode(), r.getOutTradeNo(), reply);
    }

    /**
     * 将支付宝回调的多值参数扁平化为单值 Map。
     *
     * @param request 回调请求
     * @return 扁平化参数
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

