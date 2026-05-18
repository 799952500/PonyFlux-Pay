package com.payflow.cashier.context;

import java.util.function.Supplier;

/**
 * 持久层系统模式豁免：必须在 try-finally 中使用，避免 ThreadLocal 泄漏。
 *
 * @author PayFlow Team
 */
public final class MerchantScopeHolder {

    private static final ThreadLocal<Boolean> SYSTEM_MODE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private MerchantScopeHolder() {
    }

    public static boolean isSystemMode() {
        return Boolean.TRUE.equals(SYSTEM_MODE.get());
    }

    public static void runInSystemMode(Runnable action) {
        boolean previous = isSystemMode();
        SYSTEM_MODE.set(Boolean.TRUE);
        try {
            action.run();
        } finally {
            if (previous) {
                SYSTEM_MODE.set(Boolean.TRUE);
            } else {
                SYSTEM_MODE.remove();
            }
        }
    }

    public static <T> T callInSystemMode(Supplier<T> action) {
        boolean previous = isSystemMode();
        SYSTEM_MODE.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            if (previous) {
                SYSTEM_MODE.set(Boolean.TRUE);
            } else {
                SYSTEM_MODE.remove();
            }
        }
    }

    public static void clear() {
        SYSTEM_MODE.remove();
    }
}
