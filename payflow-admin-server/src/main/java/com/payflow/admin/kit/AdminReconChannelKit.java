package com.payflow.admin.kit;

/**
 * 对账渠道与收银 pay_channel 映射（与 payflow-recon ReconChannelKit 保持一致）。
 *
 * @author PayFlow Team
 */
public final class AdminReconChannelKit {

    private AdminReconChannelKit() {
    }

    /**
     * 对账 channel → cashier_payments.pay_channel
     */
    public static String reconToPayChannel(String reconChannel) {
        if (reconChannel == null || reconChannel.isBlank()) {
            return null;
        }
        return switch (reconChannel.trim().toLowerCase()) {
            case "alipay" -> "ALIPAY";
            case "wxpay" -> "WECHAT_PAY";
            default -> null;
        };
    }
}
