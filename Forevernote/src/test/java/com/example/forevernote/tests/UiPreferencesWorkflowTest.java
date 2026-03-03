package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.UiPreferencesWorkflow;
import com.example.forevernote.ui.workflow.UiPreferencesWorkflow.UiPreferences;

class UiPreferencesWorkflowTest {

    @Test
    void shouldLoadDefaultsWhenPreferencesAreEmpty() throws Exception {
        UiPreferencesWorkflow workflow = new UiPreferencesWorkflow();
        Preferences prefs = Preferences.userRoot().node("forevernote-test-ui-prefs-defaults");
        prefs.clear();

        UiPreferences loaded = workflow.load(prefs);
        assertEquals(UiPreferencesWorkflow.MODE_TEXT, loaded.sidebarTabsMode());
        assertEquals(UiPreferencesWorkflow.MODE_TEXT, loaded.editorViewModeButtonsMode());
        assertTrue(loaded.autosaveEnabled());
        assertEquals(UiPreferencesWorkflow.DEFAULT_AUTOSAVE_IDLE_MS, loaded.autosaveIdleMs());
        assertEquals(false, loaded.accentEnabled());
        assertEquals("#7c3aed", loaded.accentColor());
    }

    @Test
    void shouldPersistAndReloadConfiguredPreferences() throws Exception {
        UiPreferencesWorkflow workflow = new UiPreferencesWorkflow();
        Preferences prefs = Preferences.userRoot().node("forevernote-test-ui-prefs-save");
        prefs.clear();

        UiPreferences value = new UiPreferences(
                UiPreferencesWorkflow.MODE_ICONS,
                UiPreferencesWorkflow.MODE_AUTO,
                false,
                5000,
                UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL,
                "retro-phosphor",
                true,
                "#00ffaa");
        workflow.save(prefs, value);

        UiPreferences loaded = workflow.load(prefs);
        assertEquals(UiPreferencesWorkflow.MODE_ICONS, loaded.sidebarTabsMode());
        assertEquals(UiPreferencesWorkflow.MODE_AUTO, loaded.editorViewModeButtonsMode());
        assertEquals(false, loaded.autosaveEnabled());
        assertEquals(5000, loaded.autosaveIdleMs());
        assertEquals(UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL, loaded.themeSource());
        assertEquals("retro-phosphor", loaded.externalThemeId());
        assertEquals(true, loaded.accentEnabled());
        assertEquals("#00ffaa", loaded.accentColor());
    }
}
