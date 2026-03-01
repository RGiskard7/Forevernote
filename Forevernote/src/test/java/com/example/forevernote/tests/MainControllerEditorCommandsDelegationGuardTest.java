package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerEditorCommandsDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateEditorCommandsToEditorWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("editorCommandWorkflow.handleUndo("),
                "MainController should delegate undo to EditorCommandWorkflow.");
        assertTrue(source.contains("editorCommandWorkflow.handleReplace("),
                "MainController should delegate replace to EditorCommandWorkflow.");
        assertTrue(source.contains("editorCommandWorkflow.publishAction("),
                "MainController should delegate formatting actions to EditorCommandWorkflow.");
    }
}
