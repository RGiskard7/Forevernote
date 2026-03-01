package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.CommandUIWorkflow;
import com.example.forevernote.ui.workflow.CommandUIWorkflow.CommandUiComponents;

class CommandUIWorkflowTest {

    @Test
    void ensureComponentsShouldKeepNullWhenStageIsMissing() {
        CommandUIWorkflow workflow = new CommandUIWorkflow();
        CommandUiComponents components = workflow.ensureCommandUiComponents(
                null,
                null,
                null,
                command -> {
                },
                note -> {
                });

        assertNull(components.commandPalette());
        assertNull(components.quickSwitcher());
    }

    @Test
    void initializeKeyboardShortcutsShouldWarnWhenSceneIsMissing() {
        CommandUIWorkflow workflow = new CommandUIWorkflow();
        AtomicInteger warnings = new AtomicInteger(0);

        workflow.initializeKeyboardShortcuts(
                null,
                () -> {
                },
                () -> {
                },
                msg -> {
                },
                msg -> warnings.incrementAndGet());

        assertEquals(1, warnings.get());
    }
}
