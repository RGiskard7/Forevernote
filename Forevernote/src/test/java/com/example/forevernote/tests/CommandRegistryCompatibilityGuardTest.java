package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CommandRegistryCompatibilityGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");
    private static final Path COMMAND_REGISTRY = Path
            .of("src/main/java/com/example/forevernote/ui/workflow/CommandRegistryWorkflow.java");

    @Test
    void commandRegistryShouldKeepCriticalIdsAndLegacyAlias() throws IOException {
        String mainSource = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        String registrySource = Files.readString(COMMAND_REGISTRY, StandardCharsets.UTF_8);

        assertTrue(mainSource.contains("commandRegistryWorkflow.registerDefaultRoutes("),
                "MainController should initialize command routing through CommandRegistryWorkflow.");

        assertTrue(registrySource.contains("\"cmd.new_note\""), "Missing critical command ID: cmd.new_note");
        assertTrue(registrySource.contains("\"cmd.save\""), "Missing critical command ID: cmd.save");
        assertTrue(registrySource.contains("\"cmd.refresh\""), "Missing critical command ID: cmd.refresh");
        assertTrue(registrySource.contains("\"Toggle Right Panel\""),
                "Legacy alias 'Toggle Right Panel' must remain available.");
    }
}
