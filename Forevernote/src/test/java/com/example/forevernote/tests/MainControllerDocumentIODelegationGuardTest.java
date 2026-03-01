package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerDocumentIODelegationGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldDelegateImportExportToDocumentIOWorkflow() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("fileCommandWorkflow.handleImport("),
                "MainController should delegate import command to FileCommandWorkflow.");
        assertTrue(source.contains("fileCommandWorkflow.handleExport("),
                "MainController should delegate export command to FileCommandWorkflow.");
    }
}
