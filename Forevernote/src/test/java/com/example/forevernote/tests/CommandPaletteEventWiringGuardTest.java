package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CommandPaletteEventWiringGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldSubscribeToToolbarPaletteEvents() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("eventBus.subscribe(UIEvents.ShowCommandPaletteEvent.class"),
                "MainController must handle ShowCommandPaletteEvent.");
        assertTrue(source.contains("eventBus.subscribe(UIEvents.ShowQuickSwitcherEvent.class"),
                "MainController must handle ShowQuickSwitcherEvent.");
    }
}
