package com.example.forevernote.ui.workflow;

import java.util.function.Supplier;

/**
 * Pure theme-resolution helpers to keep controllers thin.
 */
public class ThemeWorkflow {

    public boolean isDarkTheme(String currentTheme, Supplier<String> systemThemeDetector) {
        return "dark".equalsIgnoreCase(resolveThemeToApply(currentTheme, systemThemeDetector));
    }

    public String resolveThemeToApply(String currentTheme, Supplier<String> systemThemeDetector) {
        if ("system".equalsIgnoreCase(currentTheme)) {
            String detected = systemThemeDetector != null ? systemThemeDetector.get() : "light";
            return "dark".equalsIgnoreCase(detected) ? "dark" : "light";
        }
        return "dark".equalsIgnoreCase(currentTheme) ? "dark" : "light";
    }
}
