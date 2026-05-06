package com.payflow.recon.openservice.bill;

import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 对账渠道 OpenService 定位器（与 PayChannelPaymentOpenServiceLocator 同构）。
 *
 * @author PayFlow Team
 */
@Component
@RequiredArgsConstructor
public class ReconChannelOpenServiceLocator {

    private final ApplicationContext applicationContext;

    /**
     * @param channelCode 小写 alipay / wxpay
     */
    public ReconChannelOpenService requireByChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            throw new BizException(7501, "缺少 channelCode");
        }
        if (!channelCode.equals(channelCode.toLowerCase(Locale.ROOT))) {
            throw new BizException(7502, "channelCode 必须为小写: " + channelCode);
        }
        String beanName = channelCode + "ReconChannelOpenService";
        return applicationContext.getBean(beanName, ReconChannelOpenService.class);
    }
}
