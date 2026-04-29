package com.payflow.cashier.openservice;

import com.payflow.cashier.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 按约定 Bean 命名定位渠道 OpenService。
 *
 * @author Lucas
 */
@Component
@RequiredArgsConstructor
public class PayChannelOpenServiceLocator {

    /** Spring 容器：用于按 BeanName 获取渠道 OpenService */
    private final ApplicationContext applicationContext;

    /**
     * 根据订单渠道定位 OpenService（找不到会抛出 Spring 的 NoSuchBeanDefinitionException）。
     *
     * @param orderChannel 订单渠道（例如 ALIPAY / WECHAT_PAY）
     * @return 渠道开放服务
     */
    public PayChannelOpenService requireByOrderChannel(String orderChannel) {
        String beanName = toBeanName(orderChannel);
        return applicationContext.getBean(beanName, PayChannelOpenService.class);
    }

    /**
     * 根据渠道编码定位 OpenService（用于回调路径显式传入渠道编码的场景）。
     * <p>
     * 约定：渠道编码 = BeanName 前缀，例如 wxpay -> wxpayOpenService，alipay -> alipayOpenService。
     * </p>
     *
     * @param channelCode 渠道编码（例如 wxpay / alipay）
     * @return 渠道开放服务
     */
    public PayChannelOpenService requireByChannelCode(String channelCode) {
        String safeCode = channelCode == null ? "" : channelCode.trim().toLowerCase(Locale.ROOT);
        String beanName = safeCode + "OpenService";
        return applicationContext.getBean(beanName, PayChannelOpenService.class);
    }

    /**
     * 将订单渠道转换为 OpenService 的 BeanName。
     * <p>
     * 说明：
     * <ul>
     *     <li>订单渠道是大写常量（ALIPAY/WECHAT_PAY）</li>
     *     <li>OpenService BeanName 约定为小写渠道 + OpenService（alipayOpenService/wxpayOpenService）</li>
     * </ul>
     * </p>
     *
     * @param orderChannel 订单渠道
     * @return BeanName
     */
    private String toBeanName(String orderChannel) {
        if (Order.CHANNEL_ALIPAY.equals(orderChannel)) {
            return "alipayOpenService";
        }
        if (Order.CHANNEL_WECHAT_PAY.equals(orderChannel)) {
            return "wxpayOpenService";
        }
        String fallback = orderChannel == null ? "" : orderChannel.toLowerCase(Locale.ROOT);
        return fallback + "OpenService";
    }
}

