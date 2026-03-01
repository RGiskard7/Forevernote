package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CommandPaletteEventWiringGuardTest {

    private static final Path MAIN_CONTROLLER = Path
            .of("src/main/java/com/example/forevernote/ui/controller/MainController.java");
    private static final Path UI_EVENT_WORKFLOW = Path
            .of("src/main/java/com/example/forevernote/ui/workflow/UiEventSubscriptionWorkflow.java");

    @Test
    void uiEventWiringShouldHandleToolbarPaletteEventsViaWorkflow() throws IOException {
        String mainSource = Files.readString(MAIN_CONTROLLER, StandardCharsets.UTF_8);
        String workflowSource = Files.readString(UI_EVENT_WORKFLOW, StandardCharsets.UTF_8);

        assertTrue(mainSource.contains("uiEventSubscriptionWorkflow.subscribeUiEvents("),
                "MainController must delegate UI event wiring to UiEventSubscriptionWorkflow.");
        assertTrue(workflowSource.contains("eventBus.subscribe(UIEvents.ShowCommandPaletteEvent.class"),
                "UiEventSubscriptionWorkflow must handle ShowCommandPaletteEvent.");
        assertTrue(workflowSource.contains("eventBus.subscribe(UIEvents.ShowQuickSwitcherEvent.class"),
                "UiEventSubscriptionWorkflow must handle ShowQuickSwitcherEvent.");
    }
}
