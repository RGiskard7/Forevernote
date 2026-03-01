package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerTagManagementDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateTagInteractionsToTagManagementWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("tagManagementWorkflow.handleAddTagToNote("),
                "MainController should delegate add-tag flow to TagManagementWorkflow.");
        assertTrue(source.contains("tagManagementWorkflow.removeTagFromNote("),
                "MainController should delegate remove-tag flow to TagManagementWorkflow.");
        assertTrue(source.contains("tagManagementWorkflow.showTagsManager("),
                "MainController should delegate tags manager dialog to TagManagementWorkflow.");
    }
}
