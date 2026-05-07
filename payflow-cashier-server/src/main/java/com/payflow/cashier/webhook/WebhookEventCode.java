package com.payflow.cashier.webhook;

/**
 * Webhook 事件类型（与订阅端 event_codes 对应）。
 */
public enum WebhookEventCode {

    PAYMENT_SUCCESS("payment.success"),
    PAYMENT_FAILED("payment.failed"),
    REFUND_SUCCESS("refund.success"),
    RECON_COMPLETED("recon.completed");

    private final String code;

    WebhookEventCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
