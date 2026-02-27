package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CommandRoutingGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldUseCommandRoutingTableInsteadOfGiantSwitch() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("initializeCommandRouting("),
                "Expected command routing initializer to exist.");
        assertFalse(source.contains("switch (resolvedCommand)"),
                "Legacy switch-based command dispatch found. Keep map-based routing.");
    }
}
