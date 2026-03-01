package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerStatusI18nGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");

    @Test
    void mainControllerShouldUseI18nKeysForFavoriteAndPinStatus() throws IOException {
        String source = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        assertTrue(source.contains("status.note_marked_favorite"));
        assertTrue(source.contains("status.note_unmarked_favorite"));
        assertTrue(source.contains("status.note_pinned"));
        assertTrue(source.contains("status.note_unpinned"));
        assertFalse(source.contains("Note marked as favorite"));
        assertFalse(source.contains("Note unmarked as favorite"));
        assertFalse(source.contains("Note pinned"));
        assertFalse(source.contains("Note unpinned"));
    }
}
