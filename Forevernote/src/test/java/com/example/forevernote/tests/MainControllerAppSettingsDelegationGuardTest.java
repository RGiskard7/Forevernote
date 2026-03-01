package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerAppSettingsDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateSettingsMenusAndStorageToAppSettingsWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("appSettingsWorkflow.handleSwitchStorage("),
                "MainController should delegate storage switch flow to AppSettingsWorkflow.");
        assertTrue(source.contains("appSettingsWorkflow.initializeThemeMenu("),
                "MainController should delegate theme menu init to AppSettingsWorkflow.");
        assertTrue(source.contains("appSettingsWorkflow.initializeLanguageMenu("),
                "MainController should delegate language menu init to AppSettingsWorkflow.");
        assertTrue(source.contains("appSettingsWorkflow.updateThemeMenuSelection("),
                "MainController should delegate theme menu selection updates to AppSettingsWorkflow.");
    }
}
