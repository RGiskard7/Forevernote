package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MainControllerPreviewLiveUpdateGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");
    private static final Path UI_EVENT_WORKFLOW = Path
            .of("src/main/java/com/example/forevernote/ui/workflow/UiEventSubscriptionWorkflow.java");

    @Test
    void noteModifiedEventShouldRefreshPreviewLive() throws IOException {
        String mainSource = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        String workflowSource = Files.readString(UI_EVENT_WORKFLOW, StandardCharsets.UTF_8);

        assertTrue(workflowSource.contains("eventBus.subscribe(NoteEvents.NoteModifiedEvent.class"),
                "UiEventSubscriptionWorkflow must subscribe to NoteModifiedEvent.");
        assertTrue(mainSource.contains("handleUiNoteModified("),
                "MainController must handle NoteModifiedEvent callback.");
        assertTrue(mainSource.contains("updatePreview();"),
                "MainController note-modified handler must refresh preview live.");
    }
}
