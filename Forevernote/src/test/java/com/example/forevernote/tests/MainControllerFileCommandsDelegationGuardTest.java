package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerFileCommandsDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateFileCommandsToFileWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("fileCommandWorkflow.handleNewNote("),
                "MainController should delegate new note command to FileCommandWorkflow.");
        assertTrue(source.contains("fileCommandWorkflow.handleNewFolder("),
                "MainController should delegate new folder command to FileCommandWorkflow.");
        assertTrue(source.contains("fileCommandWorkflow.handleSave("),
                "MainController should delegate save command to FileCommandWorkflow.");
        assertTrue(source.contains("fileCommandWorkflow.handleDelete("),
                "MainController should delegate delete command to FileCommandWorkflow.");
    }
}
