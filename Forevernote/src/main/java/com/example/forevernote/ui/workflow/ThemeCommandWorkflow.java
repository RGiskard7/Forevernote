package com.example.forevernote.ui.workflow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.scene.Scene;
import javafx.scene.web.WebView;

/**
 * Encapsulates theme selection, detection and stylesheet application.
 */
public class ThemeCommandWorkflow {

    public String setLightTheme(Preferences prefs) {
        if (prefs != null) {
            prefs.put("theme", "light");
        }
        return "light";
    }

    public String setDarkTheme(Preferences prefs) {
        if (prefs != null) {
            prefs.put("theme", "dark");
        }
        return "dark";
    }

    public record SystemThemeResult(String currentTheme, String detectedTheme) {
    }

    public SystemThemeResult setSystemTheme(Preferences prefs, Supplier<Boolean> windowsDetector,
            Consumer<Exception> warningConsumer) {
        if (prefs != null) {
            prefs.put("theme", "system");
        }
        String detected = detectSystemTheme(windowsDetector, warningConsumer);
        return new SystemThemeResult("system", detected);
    }

    public boolean detectWindowsTheme() {
        return false;
    }

    public String detectSystemTheme(Supplier<Boolean> windowsDetector, Consumer<Exception> warningConsumer) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isSystemDark = false;

        if (osName.contains("win")) {
            isSystemDark = windowsDetector != null && windowsDetector.get();
        } else if (osName.contains("mac")) {
            try {
                Process process = Runtime.getRuntime().exec("defaults read -g AppleInterfaceStyle");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                isSystemDark = "Dark".equals(line);
                process.waitFor();
            } catch (Exception e) {
                if (warningConsumer != null) {
                    warningConsumer.accept(e);
                }
            }
        }

        return isSystemDark ? "dark" : "light";
    }

    public String resolveThemeToApply(String currentTheme, Supplier<String> systemThemeDetector) {
        if ("system".equalsIgnoreCase(currentTheme)) {
            String detected = systemThemeDetector != null ? systemThemeDetector.get() : "light";
            return "dark".equalsIgnoreCase(detected) ? "dark" : "light";
        }
        return "dark".equalsIgnoreCase(currentTheme) ? "dark" : "light";
    }

    public boolean isDarkThemeActive(String currentTheme, Supplier<String> systemThemeDetector) {
        return "dark".equalsIgnoreCase(resolveThemeToApply(currentTheme, systemThemeDetector));
    }

    public void applyTheme(Scene scene, String currentTheme, Supplier<String> resolvedThemeSupplier,
            Function<String, URL> themeResourceResolver, WebView previewWebView, Runnable refreshPreview,
            Consumer<String> infoConsumer, Consumer<String> warningConsumer) {
        if (scene == null || resolvedThemeSupplier == null || themeResourceResolver == null) {
            if (warningConsumer != null) {
                warningConsumer.accept("Cannot apply theme: scene not available");
            }
            return;
        }

        scene.getStylesheets().removeIf(stylesheet -> stylesheet.contains("modern-theme.css") ||
                stylesheet.contains("dark-theme.css"));

        String themeToApply = resolvedThemeSupplier.get();
        URL themeResource = themeResourceResolver.apply(themeToApply);

        if (themeResource == null) {
            if (warningConsumer != null) {
                warningConsumer.accept("Could not load theme stylesheet for: " + currentTheme);
            }
            return;
        }

        scene.getStylesheets().add(themeResource.toExternalForm());
        if (infoConsumer != null) {
            infoConsumer.accept("Theme changed to: " + currentTheme + " (Applied: " + themeToApply + ")");
        }

        if (previewWebView != null) {
            if (!previewWebView.getStyleClass().contains("webview-theme")) {
                previewWebView.getStyleClass().add("webview-theme");
            }
            String bgColor = "dark".equalsIgnoreCase(themeToApply) ? "#1E1E1E" : "#FFFFFF";
            previewWebView.getEngine().executeScript("document.body.style.backgroundColor = '" + bgColor + "';");
        }

        if (refreshPreview != null) {
            refreshPreview.run();
        }
    }
}
