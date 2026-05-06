package com.payflow.recon.parser;

import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 账单解析器定位器（与 PayStrategyLocator 同构）。
 *
 * @author PayFlow Team
 */
@Component
@RequiredArgsConstructor
public class BillParserStrategyLocator {

    private final ApplicationContext applicationContext;

    /**
     * @param channelCode 小写 alipay / wxpay
     */
    public BillParser requireByChannelCode(String channelCode) {
        if (channelCode == null || channelCode.isBlank()) {
            throw new BizException(7503, "缺少 channelCode");
        }
        return applicationContext.getBean(channelCode + "BillParser", BillParser.class);
    }
}
