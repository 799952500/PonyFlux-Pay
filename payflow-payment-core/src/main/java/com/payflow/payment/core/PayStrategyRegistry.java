package com.payflow.payment.core;

import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.List;

/**
 * 支付策略注册表：由 Spring 注入全部 {@link PayStrategy} 后构建，避免按类名 switch。
 *
 * @author Lucas
 */
@Slf4j
public final class PayStrategyRegistry {

    private final EnumMap<PayMethod, PayStrategy> strategyMap;

    public PayStrategyRegistry(List<PayStrategy> strategies) {
        EnumMap<PayMethod, PayStrategy> map = new EnumMap<>(PayMethod.class);
        for (PayStrategy s : strategies) {
            PayMethod m = s.getPayMethod();
            if (map.put(m, s) != null) {
                throw new IllegalStateException("重复的支付方式注册: " + m);
            }
        }
        this.strategyMap = map;
    }

    public PayStrategy requireByCode(String payMethodCode) {
        PayMethod method = PayMethod.fromCode(payMethodCode);
        if (method == null) {
            throw new BizException(6007, "不支持的支付方式: " + payMethodCode);
        }
        PayStrategy strategy = strategyMap.get(method);
        if (strategy == null) {
            throw new BizException(6008, "支付方式未配置: " + payMethodCode);
        }
        return strategy;
    }

    /**
     * 识别渠道 HTTP 特征并委托对应策略解析异步通知。
     *
     * @return 微信应答 SUCCESS/FAIL，或支付宝 success/fail
     */
    public String dispatchChannelNotify(HttpServletRequest request) {
        String wechatpaySerial = request.getHeader("Wechatpay-Serial");
        String tradeStatus = request.getParameter("trade_status");

        if (wechatpaySerial != null && !wechatpaySerial.isBlank()) {
            log.debug("识别为微信支付回调: Wechatpay-Serial={}", wechatpaySerial);
            PayStrategy s = strategyMap.get(PayMethod.WECHAT_NATIVE);
            if (s == null) {
                return "FAIL";
            }
            return s.parseNotify(request).getWxReply();
        }
        if (tradeStatus != null && !tradeStatus.isBlank()) {
            log.debug("识别为支付宝回调: trade_status={}", tradeStatus);
            PayStrategy s = strategyMap.get(PayMethod.ALIPAY_QR);
            if (s == null) {
                return "fail";
            }
            return s.parseNotify(request).getAliReply();
        }
        log.warn("无法识别支付渠道回调: 既无 Wechatpay-Serial 也无 trade_status");
        return "fail";
    }
}
