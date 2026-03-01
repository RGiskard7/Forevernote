package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

class I18nBundleFallbackGuardTest {

    @Test
    void baseBundleShouldResolveCriticalKeysForUnsupportedLocale() {
        ResourceBundle bundle = ResourceBundle.getBundle(
                "com.example.forevernote.i18n.messages",
                Locale.forLanguageTag("fr-FR"));

        assertNotNull(bundle);
        assertTrue(bundle.containsKey("app.all_notes"));
        assertTrue(bundle.containsKey("app.my_notes"));
        assertNotEquals("app.all_notes", bundle.getString("app.all_notes"));
    }
}
