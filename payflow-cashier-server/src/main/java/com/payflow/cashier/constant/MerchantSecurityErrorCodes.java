package com.payflow.cashier.constant;

/**
 * 商户安全相关错误码（5xxx 段）。
 *
 * @author PayFlow Team
 */
public final class MerchantSecurityErrorCodes {

    private MerchantSecurityErrorCodes() {
    }

    /** 请求体/query 中 merchantId 与认证上下文不一致 */
    public static final int MERCHANT_ID_MISMATCH = 5101;

    /** 资源不存在或无权限（对外统一文案） */
    public static final int RESOURCE_NOT_FOUND = 5102;

    /** 资源越权（仅审计内部使用，对外仍返回 5102 文案） */
    public static final int RESOURCE_FORBIDDEN_INTERNAL = 5103;

    public static final String MSG_MERCHANT_ID_MISMATCH = "商户身份与请求不匹配";

    public static final String MSG_RESOURCE_NOT_FOUND = "请求的资源不存在";
}
