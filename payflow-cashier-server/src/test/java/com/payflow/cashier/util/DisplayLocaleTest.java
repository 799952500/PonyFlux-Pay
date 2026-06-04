package com.payflow.cashier.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayLocaleTest {

    @Test
    void normalizeDefaultsToZhCnWhenBlank() {
        assertEquals("zh-CN", DisplayLocale.normalize(null));
        assertEquals("zh-CN", DisplayLocale.normalize(""));
    }

    @Test
    void normalizeAcceptsSupportedLocales() {
        assertEquals("zh-TW", DisplayLocale.normalize("zh-TW"));
        assertEquals("en-US", DisplayLocale.normalize("en-US"));
    }

    @Test
    void normalizeRejectsUnknownToZhCn() {
        assertEquals("zh-CN", DisplayLocale.normalize("fr-FR"));
    }
}
