package com.example.forevernote.ui.workflow;

import java.util.prefs.Preferences;

/**
 * Centralizes UI preferences keys/defaults and persistence.
 */
public class UiPreferencesWorkflow {

    public static final String SIDEBAR_TABS_MODE_KEY = "ui.sidebar.tabs.mode";
    public static final String EDITOR_VIEWMODE_BUTTONS_MODE_KEY = "ui.editor.viewmode.buttons.mode";
    public static final String AUTOSAVE_ENABLED_KEY = "ui.autosave.enabled";
    public static final String AUTOSAVE_IDLE_MS_KEY = "ui.autosave.idle_ms";
    public static final String THEME_SOURCE_KEY = "ui.theme.source";
    public static final String THEME_EXTERNAL_ID_KEY = "ui.theme.external.id";
    public static final String ACCENT_ENABLED_KEY = "ui.accent.enabled";
    public static final String ACCENT_COLOR_KEY = "ui.accent.color";

    public static final String MODE_TEXT = "text";
    public static final String MODE_ICONS = "icons";
    public static final String MODE_AUTO = "auto";

    public static final String THEME_SOURCE_BUILTIN = "builtin";
    public static final String THEME_SOURCE_EXTERNAL = "external";

    public static final int DEFAULT_AUTOSAVE_IDLE_MS = 2000;

    public record UiPreferences(
            String sidebarTabsMode,
            String editorViewModeButtonsMode,
            boolean autosaveEnabled,
            int autosaveIdleMs,
            String themeSource,
            String externalThemeId,
            boolean accentEnabled,
            String accentColor) {
    }

    public UiPreferences load(Preferences prefs) {
        String sidebarMode = sanitizeMode(prefs != null ? prefs.get(SIDEBAR_TABS_MODE_KEY, MODE_TEXT) : MODE_TEXT, MODE_TEXT);
        String viewModeButtons = sanitizeMode(
                prefs != null ? prefs.get(EDITOR_VIEWMODE_BUTTONS_MODE_KEY, MODE_TEXT) : MODE_TEXT,
                MODE_TEXT,
                MODE_ICONS,
                MODE_AUTO);
        boolean autosaveEnabled = prefs == null || prefs.getBoolean(AUTOSAVE_ENABLED_KEY, true);
        int autosaveIdleMs = DEFAULT_AUTOSAVE_IDLE_MS;
        if (prefs != null) {
            int saved = prefs.getInt(AUTOSAVE_IDLE_MS_KEY, DEFAULT_AUTOSAVE_IDLE_MS);
            autosaveIdleMs = Math.max(500, Math.min(10000, saved));
        }
        String sourceRaw = prefs != null ? prefs.get(THEME_SOURCE_KEY, null) : null;
        String source = (sourceRaw == null || sourceRaw.isBlank()) ? THEME_SOURCE_EXTERNAL : sourceRaw;
        if (!THEME_SOURCE_EXTERNAL.equals(source)) {
            source = THEME_SOURCE_BUILTIN;
        }
        String externalId = prefs != null ? prefs.get(THEME_EXTERNAL_ID_KEY, "") : "";
        if (THEME_SOURCE_EXTERNAL.equals(source) && (externalId == null || externalId.isBlank())) {
            externalId = "retro-phosphor";
        }
        boolean accentEnabled = prefs != null && prefs.getBoolean(ACCENT_ENABLED_KEY, false);
        String accentColor = sanitizeHexColor(prefs != null ? prefs.get(ACCENT_COLOR_KEY, "#7c3aed") : "#7c3aed");
        return new UiPreferences(
                sidebarMode,
                viewModeButtons,
                autosaveEnabled,
                autosaveIdleMs,
                source,
                externalId,
                accentEnabled,
                accentColor);
    }

    public void save(Preferences prefs, UiPreferences value) {
        if (prefs == null || value == null) {
            return;
        }
        prefs.put(SIDEBAR_TABS_MODE_KEY, sanitizeMode(value.sidebarTabsMode(), MODE_TEXT));
        prefs.put(EDITOR_VIEWMODE_BUTTONS_MODE_KEY, sanitizeMode(value.editorViewModeButtonsMode(), MODE_TEXT, MODE_ICONS, MODE_AUTO));
        prefs.putBoolean(AUTOSAVE_ENABLED_KEY, value.autosaveEnabled());
        prefs.putInt(AUTOSAVE_IDLE_MS_KEY, Math.max(500, Math.min(10000, value.autosaveIdleMs())));

        String source = THEME_SOURCE_EXTERNAL.equals(value.themeSource()) ? THEME_SOURCE_EXTERNAL : THEME_SOURCE_BUILTIN;
        prefs.put(THEME_SOURCE_KEY, source);
        prefs.put(THEME_EXTERNAL_ID_KEY, value.externalThemeId() != null ? value.externalThemeId().trim() : "");
        prefs.putBoolean(ACCENT_ENABLED_KEY, value.accentEnabled());
        prefs.put(ACCENT_COLOR_KEY, sanitizeHexColor(value.accentColor()));
    }

    private String sanitizeMode(String mode, String fallback, String... allowedExtra) {
        if (MODE_TEXT.equals(mode) || MODE_ICONS.equals(mode) || MODE_AUTO.equals(mode)) {
            if (allowedExtra.length == 0) {
                return (MODE_TEXT.equals(mode) || MODE_ICONS.equals(mode)) ? mode : fallback;
            }
            for (String allowed : allowedExtra) {
                if (allowed != null && allowed.equals(mode)) {
                    return mode;
                }
            }
            if (MODE_TEXT.equals(mode)) {
                return MODE_TEXT;
            }
        }
        for (String allowed : allowedExtra) {
            if (allowed != null && allowed.equals(mode)) {
                return mode;
            }
        }
        return fallback;
    }

    private String sanitizeHexColor(String color) {
        if (color == null) {
            return "#7c3aed";
        }
        String candidate = color.trim();
        if (!candidate.startsWith("#")) {
            candidate = "#" + candidate;
        }
        if (candidate.matches("^#[0-9a-fA-F]{6}$")) {
            return candidate.toLowerCase();
        }
        return "#7c3aed";
    }
}
