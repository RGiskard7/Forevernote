package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerNotesGridDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateGridViewLogicToNotesGridWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("notesGridWorkflow.applyNotesViewMode("),
                "MainController should delegate applyNotesViewMode logic to NotesGridWorkflow.");
        assertTrue(source.contains("notesGridWorkflow.refreshGridView("),
                "MainController should delegate grid refresh logic to NotesGridWorkflow.");
    }
}
