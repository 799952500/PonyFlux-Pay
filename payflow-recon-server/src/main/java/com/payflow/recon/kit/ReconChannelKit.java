package com.payflow.recon.kit;

import com.payflow.common.exception.BizException;

/**
 * 收银渠道编码与对账 OpenService 渠道编码互转。
 *
 * @author PayFlow Team
 */
public final class ReconChannelKit {

    private ReconChannelKit() {
    }

    /**
     * cashier_channels.channel_code → 对账 channelCode（小写 Bean 前缀）
     */
    public static String cashierChannelToRecon(String cashierChannelCode) {
        if (cashierChannelCode == null || cashierChannelCode.isBlank()) {
            throw new BizException(7504, "渠道编码为空");
        }
        return switch (cashierChannelCode.toLowerCase()) {
            case "alipay" -> "alipay";
            case "wechat_pay" -> "wxpay";
            case "unionpay" -> "unionpay";
            default -> throw new BizException(7505, "暂不支持对账的渠道: " + cashierChannelCode);
        };
    }

    /**
     * 对账 channelCode → cashier_payments.pay_channel
     */
    public static String reconToPayChannel(String reconChannel) {
        if (reconChannel == null || reconChannel.isBlank()) {
            throw new BizException(7504, "对账渠道编码为空");
        }
        return switch (reconChannel) {
            case "alipay" -> "ALIPAY";
            case "wxpay" -> "WECHAT_PAY";
            case "unionpay" -> "UNION_PAY";
            default -> throw new BizException(7505, "未知对账渠道: " + reconChannel);
        };
    }
}
