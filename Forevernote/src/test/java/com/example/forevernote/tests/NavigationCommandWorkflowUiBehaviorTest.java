package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.NavigationCommandWorkflow;

import javafx.application.Platform;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

class NavigationCommandWorkflowUiBehaviorTest {

    private static boolean fxRuntimeAvailable = false;

    @BeforeAll
    static void initFxRuntime() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
            fxRuntimeAvailable = latch.await(2, TimeUnit.SECONDS);
        } catch (IllegalStateException e) {
            fxRuntimeAvailable = true;
        } catch (Exception e) {
            fxRuntimeAvailable = false;
        }
    }

    @Test
    void toggleSidebarShouldHideAndShowPanelInColumnLayout() {
        Assumptions.assumeTrue(fxRuntimeAvailable, "JavaFX runtime no disponible");

        NavigationCommandWorkflow workflow = new NavigationCommandWorkflow();
        SplitPane mainSplitPane = new SplitPane();
        VBox sidebarPane = new VBox();
        sidebarPane.setMaxWidth(250);
        sidebarPane.setPrefWidth(250);
        sidebarPane.setMinWidth(200);

        AtomicReference<String> status = new AtomicReference<>();

        boolean first = workflow.toggleSidebar(
                false,
                null,
                sidebarPane,
                mainSplitPane,
                null,
                key -> key,
                status::set);
        assertTrue(first);
        assertEquals("status.sidebar_hidden", status.get());
        assertEquals(0.0, sidebarPane.getMaxWidth());

        boolean second = workflow.toggleSidebar(
                false,
                null,
                sidebarPane,
                mainSplitPane,
                null,
                key -> key,
                status::set);
        assertTrue(second);
        assertEquals("status.sidebar_shown", status.get());
        assertTrue(sidebarPane.getMaxWidth() > 10.0);
    }

    @Test
    void toggleNotesPanelShouldUseFallbackInStackedLayout() {
        NavigationCommandWorkflow workflow = new NavigationCommandWorkflow();
        AtomicInteger fallbackCalls = new AtomicInteger(0);

        boolean handled = workflow.toggleNotesPanel(
                true,
                null,
                null,
                null,
                fallbackCalls::incrementAndGet,
                key -> key,
                message -> {
                });

        assertTrue(handled);
        assertEquals(1, fallbackCalls.get());
    }

    @Test
    void zoomOperationsShouldRespectLowerBoundAndDefaults() {
        NavigationCommandWorkflow workflow = new NavigationCommandWorkflow();

        assertEquals(14.0, workflow.zoomIn(13.0));
        assertEquals(12.0, workflow.zoomOut(13.0));
        assertEquals(8.0, workflow.zoomOut(8.0));
        assertEquals(13.0, workflow.resetUiZoom());
    }
}
