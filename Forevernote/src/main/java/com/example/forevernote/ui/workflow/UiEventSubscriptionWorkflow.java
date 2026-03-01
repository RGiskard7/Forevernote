package com.example.forevernote.ui.workflow;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.FolderEvents;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.event.events.TagEvents;
import com.example.forevernote.event.events.UIEvents;

import javafx.application.Platform;

/**
 * Encapsulates UI event-bus wiring for MainController.
 */
public class UiEventSubscriptionWorkflow {

    public interface Port {
        void applyTheme(String theme);

        void updateThemeMenuSelection();

        void loadNoteInEditor(Note note);

        void handleNotesLoaded(NoteEvents.NotesLoadedEvent event);

        void updateStatus(String message);

        void showCommandPalette();

        void showQuickSwitcher();

        void handleNoteDeleted(String noteId);

        void handleFolderDeleted(String folderId);

        void handleTrashItemDeleted();

        void handleFolderSelected(Folder folder);

        void handleTagSelected(Tag tag);

        void handleNoteOpenRequest(Note note);

        void handleTrashItemSelected(Component component);

        void handleNoteModified(Note note);
    }

    public void subscribeUiEvents(EventBus eventBus, Port port) {
        if (eventBus == null || port == null) {
            return;
        }

        eventBus.subscribe(UIEvents.ThemeChangedEvent.class, event -> Platform.runLater(() -> {
            port.applyTheme(event.getTheme());
            port.updateThemeMenuSelection();
        }));

        eventBus.subscribe(NoteEvents.NoteSelectedEvent.class, event -> Platform.runLater(() -> {
            if (event.getNote() != null) {
                port.loadNoteInEditor(event.getNote());
            }
        }));

        eventBus.subscribe(NoteEvents.NotesLoadedEvent.class, event -> Platform.runLater(() -> {
            port.handleNotesLoaded(event);
            port.updateStatus(event.getStatusMessage());
        }));

        eventBus.subscribe(UIEvents.StatusUpdateEvent.class,
                event -> Platform.runLater(() -> port.updateStatus(event.getMessage())));

        eventBus.subscribe(UIEvents.ShowCommandPaletteEvent.class,
                event -> Platform.runLater(port::showCommandPalette));

        eventBus.subscribe(UIEvents.ShowQuickSwitcherEvent.class,
                event -> Platform.runLater(port::showQuickSwitcher));

        eventBus.subscribe(NoteEvents.NoteDeletedEvent.class,
                event -> Platform.runLater(() -> port.handleNoteDeleted(event.getNoteId())));

        eventBus.subscribe(FolderEvents.FolderDeletedEvent.class,
                event -> Platform.runLater(() -> port.handleFolderDeleted(event.getFolderId())));

        eventBus.subscribe(NoteEvents.TrashItemDeletedEvent.class,
                event -> Platform.runLater(port::handleTrashItemDeleted));

        eventBus.subscribe(FolderEvents.FolderSelectedEvent.class,
                event -> Platform.runLater(() -> port.handleFolderSelected(event.getFolder())));

        eventBus.subscribe(TagEvents.TagSelectedEvent.class,
                event -> Platform.runLater(() -> port.handleTagSelected(event.getTag())));

        eventBus.subscribe(NoteEvents.NoteOpenRequestEvent.class,
                event -> Platform.runLater(() -> port.handleNoteOpenRequest(event.getNote())));

        eventBus.subscribe(NoteEvents.TrashItemSelectedEvent.class,
                event -> Platform.runLater(() -> port.handleTrashItemSelected(event.getComponent())));

        eventBus.subscribe(NoteEvents.NoteModifiedEvent.class,
                event -> Platform.runLater(() -> port.handleNoteModified(event.getNote())));
    }
}
