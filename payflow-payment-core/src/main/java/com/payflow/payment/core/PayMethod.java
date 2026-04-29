package com.payflow.payment.core;

/**
 * 支付方式编码（商户传入的 payMethod）。
  * @author Lucas
 */
public enum PayMethod {

    WECHAT_NATIVE("WECHAT_NATIVE", "微信Native扫码"),
    WECHAT_APP("WECHAT_APP", "微信App支付"),
    WECHAT_H5("WECHAT_H5", "微信H5支付"),
    ALIPAY_QR("ALIPAY_QR", "支付宝扫码"),
    ALIPAY_WAP("ALIPAY_WAP", "支付宝手机网站支付"),
    ALIPAY_APP("ALIPAY_APP", "支付宝App支付");

    private final String code;
    private final String desc;

    PayMethod(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static PayMethod fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PayMethod m : values()) {
            if (m.code.equals(code)) {
                return m;
            }
        }
        return null;
    }
}
