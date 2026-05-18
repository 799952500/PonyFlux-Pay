package com.payflow.cashier.context;

/**
 * 请求级商户上下文（ThreadLocal，只读使用）。
 *
 * @author PayFlow Team
 */
public final class MerchantContext {

    private static final ThreadLocal<ContextData> HOLDER = new ThreadLocal<>();

    private MerchantContext() {
    }

    public static void set(String merchantId, AuthMode authMode, String requestPath, String clientIp) {
        HOLDER.set(new ContextData(merchantId, authMode, requestPath, clientIp));
    }

    public static String getMerchantId() {
        ContextData data = HOLDER.get();
        return data != null ? data.merchantId : null;
    }

    public static AuthMode getAuthMode() {
        ContextData data = HOLDER.get();
        return data != null ? data.authMode : null;
    }

    public static String getRequestPath() {
        ContextData data = HOLDER.get();
        return data != null ? data.requestPath : null;
    }

    public static String getClientIp() {
        ContextData data = HOLDER.get();
        return data != null ? data.clientIp : null;
    }

    public static boolean hasMerchant() {
        String merchantId = getMerchantId();
        return merchantId != null && !merchantId.isBlank();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private record ContextData(String merchantId, AuthMode authMode, String requestPath, String clientIp) {
    }
}
