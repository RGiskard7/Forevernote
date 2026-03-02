package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.FolderEvents;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.event.events.TagEvents;
import com.example.forevernote.event.events.UIEvents;
import com.example.forevernote.ui.workflow.UiEventSubscriptionWorkflow;

import javafx.application.Platform;

class UiEventSubscriptionWorkflowTest {

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

    @AfterEach
    void clearBus() {
        EventBus.getInstance().clear();
    }

    @Test
    void subscribeUiEventsShouldRouteThemeAndNotesLoadedAndStatusEvents() throws Exception {
        Assumptions.assumeTrue(fxRuntimeAvailable, "JavaFX runtime no disponible");

        UiEventSubscriptionWorkflow workflow = new UiEventSubscriptionWorkflow();
        EventBus bus = EventBus.getInstance();

        Note note = new Note("N1", "content");
        NoteEvents.NotesLoadedEvent loadedEvent = new NoteEvents.NotesLoadedEvent(List.of(note), "loaded-status");

        CountDownLatch latch = new CountDownLatch(5);
        AtomicReference<String> appliedTheme = new AtomicReference<>();
        AtomicInteger themeMenuUpdates = new AtomicInteger(0);
        AtomicReference<NoteEvents.NotesLoadedEvent> observedLoaded = new AtomicReference<>();
        AtomicInteger statusUpdates = new AtomicInteger(0);
        AtomicReference<String> lastStatus = new AtomicReference<>();

        workflow.subscribeUiEvents(bus, new UiEventSubscriptionWorkflow.Port() {
            @Override
            public void applyTheme(String theme) {
                appliedTheme.set(theme);
                latch.countDown();
            }

            @Override
            public void updateThemeMenuSelection() {
                themeMenuUpdates.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void loadNoteInEditor(Note note) {
            }

            @Override
            public void handleNotesLoaded(NoteEvents.NotesLoadedEvent event) {
                observedLoaded.set(event);
                latch.countDown();
            }

            @Override
            public void updateStatus(String message) {
                statusUpdates.incrementAndGet();
                lastStatus.set(message);
                latch.countDown();
            }

            @Override
            public void showCommandPalette() {
            }

            @Override
            public void showQuickSwitcher() {
            }

            @Override
            public void handleNoteDeleted(String noteId) {
            }

            @Override
            public void handleFolderDeleted(String folderId) {
            }

            @Override
            public void handleTrashItemDeleted() {
            }

            @Override
            public void handleFolderSelected(Folder folder) {
            }

            @Override
            public void handleTagSelected(Tag tag) {
            }

            @Override
            public void handleNoteOpenRequest(Note note) {
            }

            @Override
            public void handleTrashItemSelected(Component component) {
            }

            @Override
            public void handleNoteModified(Note note) {
            }
        });

        bus.publishSync(new UIEvents.ThemeChangedEvent("dark"));
        bus.publishSync(loadedEvent);
        bus.publishSync(new UIEvents.StatusUpdateEvent("manual-status"));

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals("dark", appliedTheme.get());
        assertEquals(1, themeMenuUpdates.get());
        assertSame(loadedEvent, observedLoaded.get());
        assertEquals(2, statusUpdates.get());
        assertEquals("manual-status", lastStatus.get());
    }

    @Test
    void subscribeUiEventsShouldRouteSelectionAndModificationEvents() throws Exception {
        Assumptions.assumeTrue(fxRuntimeAvailable, "JavaFX runtime no disponible");

        UiEventSubscriptionWorkflow workflow = new UiEventSubscriptionWorkflow();
        EventBus bus = EventBus.getInstance();

        Folder folder = new Folder("Inbox");
        folder.setId("inbox");
        Tag tag = new Tag("work");
        Note note = new Note("N1", "content");

        CountDownLatch latch = new CountDownLatch(4);
        AtomicReference<Folder> selectedFolder = new AtomicReference<>();
        AtomicReference<Tag> selectedTag = new AtomicReference<>();
        AtomicReference<Note> openedNote = new AtomicReference<>();
        AtomicReference<Note> modifiedNote = new AtomicReference<>();

        workflow.subscribeUiEvents(bus, new UiEventSubscriptionWorkflow.Port() {
            @Override
            public void applyTheme(String theme) {
            }

            @Override
            public void updateThemeMenuSelection() {
            }

            @Override
            public void loadNoteInEditor(Note note) {
            }

            @Override
            public void handleNotesLoaded(NoteEvents.NotesLoadedEvent event) {
            }

            @Override
            public void updateStatus(String message) {
            }

            @Override
            public void showCommandPalette() {
            }

            @Override
            public void showQuickSwitcher() {
            }

            @Override
            public void handleNoteDeleted(String noteId) {
            }

            @Override
            public void handleFolderDeleted(String folderId) {
            }

            @Override
            public void handleTrashItemDeleted() {
            }

            @Override
            public void handleFolderSelected(Folder folder) {
                selectedFolder.set(folder);
                latch.countDown();
            }

            @Override
            public void handleTagSelected(Tag tag) {
                selectedTag.set(tag);
                latch.countDown();
            }

            @Override
            public void handleNoteOpenRequest(Note note) {
                openedNote.set(note);
                latch.countDown();
            }

            @Override
            public void handleTrashItemSelected(Component component) {
            }

            @Override
            public void handleNoteModified(Note note) {
                modifiedNote.set(note);
                latch.countDown();
            }
        });

        bus.publishSync(new FolderEvents.FolderSelectedEvent(folder));
        bus.publishSync(new TagEvents.TagSelectedEvent(tag));
        bus.publishSync(new NoteEvents.NoteOpenRequestEvent(note));
        bus.publishSync(new NoteEvents.NoteModifiedEvent(note));

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertSame(folder, selectedFolder.get());
        assertSame(tag, selectedTag.get());
        assertSame(note, openedNote.get());
        assertSame(note, modifiedNote.get());
    }
}
