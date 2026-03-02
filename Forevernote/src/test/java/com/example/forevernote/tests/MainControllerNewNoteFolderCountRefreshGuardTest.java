package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerNewNoteFolderCountRefreshGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void newNoteFlowShouldRefreshNotesAndFoldersImmediately() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);

        assertTrue(source.contains("public void onAfterCreate()"),
                "New note flow should define post-create UI refresh callback.");
        assertTrue(source.contains("refreshNotesList();"),
                "New note flow should refresh notes list immediately after create.");
        assertTrue(source.contains("sidebarController.loadFolders();"),
                "New note flow should reload folders immediately to update counters.");
    }
}
