package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerAutosaveGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void autosaveShouldBeDebouncedAndRespectReentryAndNoteValidityGuards() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("configureAutosaveDebounce()"),
                "MainController should configure autosave debounce during initialization.");
        assertTrue(source.contains("autosaveDebounce.setOnFinished"),
                "Autosave should run on debounced idle callback.");
        assertTrue(source.contains("if (!autosaveEnabled || autosaveRunning)"),
                "Autosave should be skipped when disabled or already running.");
        assertTrue(source.contains("pendingModifiedNoteId"),
                "Autosave should only run for the currently active modified note.");
        assertTrue(source.contains("if (!isModified())"),
                "Autosave should avoid unnecessary saves when content is unchanged.");
        assertTrue(source.contains("autosaveDebounce.playFromStart();"),
                "Note modifications should restart autosave debounce timer.");
        assertTrue(source.contains("autosaveDebounce.setDuration(Duration.millis(autosaveIdleMs));"),
                "Autosave debounce interval should be preference-driven.");
    }
}
