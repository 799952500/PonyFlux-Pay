package com.payflow.cashier.sdk;

import com.payflow.common.exception.BizException;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 支付策略定位器：按 payMethod 直接从 Spring 容器获取对应的 PayStrategy。
 * <p>
 * 约定 Bean 命名：{payMethodCodeLower}PayStrategy，例如：
 * <ul>
 *     <li>wechat_nativePayStrategy</li>
 *     <li>alipay_wapPayStrategy</li>
 * </ul>
 * </p>
 *
 * @author Lucas
 */
@Component
@RequiredArgsConstructor
public class PayStrategyLocator {

    /** Spring 容器：用于按 BeanName 获取 PayStrategy */
    private final ApplicationContext applicationContext;

    /**
     * 按商户传入的 payMethodCode 定位策略。
     *
     * @param payMethodCode 支付方式编码（例如 WECHAT_NATIVE / ALIPAY_WAP）
     * @return PayStrategy
     */
    public PayStrategy requireByPayMethodCode(String payMethodCode) {
        PayMethod payMethod = PayMethod.fromCode(payMethodCode);
        if (payMethod == null) {
            throw new BizException(6007, "不支持的支付方式: " + payMethodCode);
        }
        String beanName = toBeanName(payMethod);
        return applicationContext.getBean(beanName, PayStrategy.class);
    }

    /**
     * 将 PayMethod 转换为约定的策略 BeanName。
     *
     * @param payMethod 支付方式枚举
     * @return BeanName
     */
    private String toBeanName(PayMethod payMethod) {
        return payMethod.getCode().toLowerCase() + "PayStrategy";
    }
}

