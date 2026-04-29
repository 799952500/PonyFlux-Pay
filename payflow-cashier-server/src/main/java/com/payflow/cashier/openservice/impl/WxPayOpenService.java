package com.payflow.cashier.openservice.impl;

import com.payflow.cashier.openservice.PayChannelOpenService;
import com.payflow.cashier.sdk.wxpay.WxPayNotifyHelper;
import com.payflow.payment.core.NotifyResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 微信支付开放服务。
 * <p>
 * Bean 命名约定：wxpayOpenService（用于统一回调入口按“渠道 + OpenService”动态定位）。
 * </p>
 *
 * @author Lucas
 */
@Slf4j
@Service("wxpayOpenService")
@RequiredArgsConstructor
public class WxPayOpenService implements PayChannelOpenService {

    /** 请求体缓存属性名：防止回调入口链路中重复读取 InputStream 导致 body 为空 */
    private static final String ATTR_CACHED_BODY = "PAYFLOW_REQUEST_BODY";

    /** 微信回调解析与业务处理辅助组件（内部会解密 resource 并驱动订单状态更新） */
    private final WxPayNotifyHelper wxPayNotifyHelper;

    /**
     * 渠道编码：用于与统一入口的路由结果做一致性校验。
     *
     * @return wxpay
     */
    @Override
    public String channelCode() {
        return "wxpay";
    }

    /**
     * 解析并处理微信支付回调。
     * <p>
     * 说明：
     * <ul>
     *     <li>微信回调关键头：Wechatpay-Serial / Signature / Timestamp / Nonce</li>
     *     <li>微信回调 body 需要读取一次并参与解密与验签（当前验签逻辑可后续补齐）</li>
     * </ul>
     * </p>
     *
     * @param request 回调请求
     * @return 处理结果（outTradeNo + reply）
     */
    @Override
    public NotifyHandleResult parseAndHandleNotify(HttpServletRequest request) {
        String serial = request.getHeader("Wechatpay-Serial");
        String signature = request.getHeader("Wechatpay-Signature");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");

        String body;
        try {
            body = readBodyOnce(request);
        } catch (Exception e) {
            log.error("读取微信回调 body 失败", e);
            return new NotifyHandleResult(channelCode(), null, "FAIL");
        }

        NotifyResult r = wxPayNotifyHelper.parseNotify(serial, signature, timestamp, nonce, body);
        String reply = r.getWxReply() == null ? "FAIL" : r.getWxReply();
        return new NotifyHandleResult(channelCode(), r.getOutTradeNo(), reply);
    }

    /**
     * 读取 request body（只读一次，后续从 attribute 复用）。
     *
     * @param request 回调请求
     * @return body 文本
     * @throws Exception 读取失败
     */
    private String readBodyOnce(HttpServletRequest request) throws Exception {
        Object cached = request.getAttribute(ATTR_CACHED_BODY);
        if (cached instanceof String s) {
            return s;
        }
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        request.setAttribute(ATTR_CACHED_BODY, body);
        return body;
    }
}

