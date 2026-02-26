package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.ThemeWorkflow;

class ThemeWorkflowTest {

    @Test
    void resolveThemeToApplySupportsLightDarkAndSystem() {
        ThemeWorkflow workflow = new ThemeWorkflow();

        assertEquals("light", workflow.resolveThemeToApply("light", () -> "dark"));
        assertEquals("dark", workflow.resolveThemeToApply("dark", () -> "light"));
        assertEquals("dark", workflow.resolveThemeToApply("system", () -> "dark"));
        assertEquals("light", workflow.resolveThemeToApply("system", () -> "light"));
    }

    @Test
    void isDarkThemeUsesResolvedTheme() {
        ThemeWorkflow workflow = new ThemeWorkflow();

        assertTrue(workflow.isDarkTheme("dark", () -> "light"));
        assertTrue(workflow.isDarkTheme("system", () -> "dark"));
        assertFalse(workflow.isDarkTheme("light", () -> "dark"));
    }
}
