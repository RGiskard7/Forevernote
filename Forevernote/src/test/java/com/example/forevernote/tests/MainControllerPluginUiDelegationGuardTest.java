package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerPluginUiDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegatePluginDynamicUiToPluginUiWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("pluginUiWorkflow.registerMenuItem("),
                "MainController should delegate plugin menu registration to PluginUiWorkflow.");
        assertTrue(source.contains("pluginUiWorkflow.registerSidePanel("),
                "MainController should delegate side panel registration to PluginUiWorkflow.");
        assertTrue(source.contains("pluginUiWorkflow.registerStatusBarItem("),
                "MainController should delegate status bar registration to PluginUiWorkflow.");
    }
}
