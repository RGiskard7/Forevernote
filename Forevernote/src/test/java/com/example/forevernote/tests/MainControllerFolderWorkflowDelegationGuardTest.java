package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerFolderWorkflowDelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateFolderCreationToFolderWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("folderWorkflow.createFolder("),
                "MainController should delegate folder creation to FolderWorkflow.");
        assertTrue(source.contains("folderWorkflow.createSubfolder("),
                "MainController should delegate subfolder creation to FolderWorkflow.");
    }
}
