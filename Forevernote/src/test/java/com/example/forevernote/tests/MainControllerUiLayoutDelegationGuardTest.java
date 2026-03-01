package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerUiLayoutDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateLayoutModeAndRightPanelLogicToUiLayoutWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("uiLayoutWorkflow.applyViewMode("),
                "MainController should delegate applyViewMode logic to UiLayoutWorkflow.");
        assertTrue(source.contains("uiLayoutWorkflow.toggleRightPanel("),
                "MainController should delegate right panel toggle logic to UiLayoutWorkflow.");
        assertTrue(source.contains("uiLayoutWorkflow.closeRightPanel("),
                "MainController should delegate right panel close logic to UiLayoutWorkflow.");
    }
}
