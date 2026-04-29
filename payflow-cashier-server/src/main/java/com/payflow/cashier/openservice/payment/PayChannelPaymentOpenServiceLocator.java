package com.payflow.cashier.openservice.payment;

import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 支付下单 OpenService 定位器：按渠道编码从 Spring 容器获取对应实现。
 *
 * @author Lucas
 */
@Component
@RequiredArgsConstructor
public class PayChannelPaymentOpenServiceLocator {

    /** Spring 容器：用于按 BeanName 获取 PaymentOpenService */
    private final ApplicationContext applicationContext;

    /**
     * 根据渠道编码定位 PaymentOpenService。
     * <p>
     * 约定：{channelCode}PaymentOpenService，例如 alipayPaymentOpenService、wxpayPaymentOpenService。
     * </p>
     *
     * @param channelCode 渠道编码（必须小写，例如 alipay / wxpay）
     * @return PaymentOpenService
     */
    public PayChannelPaymentOpenService requireByChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            throw new BizException(7101, "缺少 channelCode");
        }
        if (!channelCode.equals(channelCode.toLowerCase(Locale.ROOT))) {
            throw new BizException(7102, "channelCode 必须为小写: " + channelCode);
        }
        String beanName = channelCode + "PaymentOpenService";
        return applicationContext.getBean(beanName, PayChannelPaymentOpenService.class);
    }
}

