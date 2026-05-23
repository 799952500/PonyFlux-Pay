package com.payflow.cashier.constant;

/**
 * 商户回调（平台 → 商户）常量。
 */
public final class MerchantNotifyConstants {

    private MerchantNotifyConstants() {
    }

    public static final String NOTIFY_TYPE_PAYMENT = "PAYMENT";
    public static final String NOTIFY_TYPE_REFUND = "REFUND";

    public static final String SUMMARY_NOT_CONFIGURED = "NOT_CONFIGURED";
    public static final String SUMMARY_PENDING = "PENDING";
    public static final String SUMMARY_IN_PROGRESS = "IN_PROGRESS";
    public static final String SUMMARY_SUCCESS = "SUCCESS";
    public static final String SUMMARY_FAILED = "FAILED";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILED = "FAILED";
    public static final String RESULT_IN_PROGRESS = "IN_PROGRESS";

    public static final String FAIL_TIMEOUT = "TIMEOUT";
    public static final String FAIL_HTTP_ERROR = "HTTP_ERROR";
    public static final String FAIL_RESPONSE_NOT_SUCCESS = "RESPONSE_NOT_SUCCESS";
    public static final String FAIL_SIGN_SKIPPED = "SIGN_SKIPPED";
    public static final String FAIL_UNKNOWN = "UNKNOWN";

    public static final int MAX_PAYLOAD_BYTES = 32 * 1024;
}
