package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.ThemeCatalogWorkflow;
import com.example.forevernote.ui.workflow.ThemeCatalogWorkflow.ThemeDescriptor;

class ThemeCatalogWorkflowTest {

    @Test
    void shouldAlwaysIncludeBuiltinThemes() {
        ThemeCatalogWorkflow workflow = new ThemeCatalogWorkflow();
        List<ThemeDescriptor> themes = workflow.getAvailableThemes();

        assertTrue(themes.stream().anyMatch(t -> "light".equals(t.id())));
        assertTrue(themes.stream().anyMatch(t -> "dark".equals(t.id())));
        assertTrue(themes.stream().anyMatch(t -> "system".equals(t.id())));
    }

    @Test
    void shouldFindThemeById() {
        ThemeCatalogWorkflow workflow = new ThemeCatalogWorkflow();
        ThemeDescriptor descriptor = workflow.findById(workflow.getAvailableThemes(), "light");
        assertNotNull(descriptor);
    }
}
