package com.payflow.payment.core;

/**
 * 支付方式编码（商户传入的 payMethod）。
  * @author Lucas
 */
public enum PayMethod {

    WECHAT_NATIVE("WECHAT_NATIVE", "微信Native扫码"),
    WECHAT_APP("WECHAT_APP", "微信App支付"),
    WECHAT_H5("WECHAT_H5", "微信H5支付"),
    /** 公众号内 JSAPI 支付 */
    WECHAT_JSAPI("WECHAT_JSAPI", "微信JSAPI支付"),
    /** 小程序内支付（与 JSAPI 共用统一下单接口） */
    WECHAT_MINI("WECHAT_MINI", "微信小程序支付"),
    /** 付款码（被扫） */
    WECHAT_MICROPAY("WECHAT_MICROPAY", "微信付款码支付"),
    ALIPAY_QR("ALIPAY_QR", "支付宝扫码"),
    ALIPAY_WAP("ALIPAY_WAP", "支付宝手机网站支付"),
    ALIPAY_APP("ALIPAY_APP", "支付宝App支付"),
    /** 条码/刷脸等被扫 */
    ALIPAY_FACE("ALIPAY_FACE", "支付宝条码/当面付"),
    /** 银联/云闪付 H5（需对接银联开放平台，当前为 SPI 占位） */
    UNION_H5("UNION_H5", "银联云闪付H5");

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
