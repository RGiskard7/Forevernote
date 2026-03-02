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
import java.util.ArrayList;
import java.util.List;

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

    public List<EventBus.Subscription> subscribeUiEvents(EventBus eventBus, Port port) {
        if (eventBus == null || port == null) {
            return List.of();
        }
        List<EventBus.Subscription> subscriptions = new ArrayList<>();

        subscriptions.add(eventBus.subscribe(UIEvents.ThemeChangedEvent.class, event -> {
            port.applyTheme(event.getTheme());
            port.updateThemeMenuSelection();
        }));

        subscriptions.add(eventBus.subscribe(NoteEvents.NoteSelectedEvent.class, event -> {
            if (event.getNote() != null) {
                port.loadNoteInEditor(event.getNote());
            }
        }));

        subscriptions.add(eventBus.subscribe(NoteEvents.NotesLoadedEvent.class, event -> {
            port.handleNotesLoaded(event);
            port.updateStatus(event.getStatusMessage());
        }));

        subscriptions.add(eventBus.subscribe(UIEvents.StatusUpdateEvent.class, event -> port.updateStatus(event.getMessage())));

        subscriptions.add(eventBus.subscribe(UIEvents.ShowCommandPaletteEvent.class, event -> port.showCommandPalette()));

        subscriptions.add(eventBus.subscribe(UIEvents.ShowQuickSwitcherEvent.class, event -> port.showQuickSwitcher()));

        subscriptions.add(eventBus.subscribe(NoteEvents.NoteDeletedEvent.class, event -> port.handleNoteDeleted(event.getNoteId())));

        subscriptions.add(eventBus.subscribe(FolderEvents.FolderDeletedEvent.class, event -> port.handleFolderDeleted(event.getFolderId())));

        subscriptions.add(eventBus.subscribe(NoteEvents.TrashItemDeletedEvent.class, event -> port.handleTrashItemDeleted()));

        subscriptions.add(eventBus.subscribe(FolderEvents.FolderSelectedEvent.class, event -> port.handleFolderSelected(event.getFolder())));

        subscriptions.add(eventBus.subscribe(TagEvents.TagSelectedEvent.class, event -> port.handleTagSelected(event.getTag())));

        subscriptions.add(eventBus.subscribe(NoteEvents.NoteOpenRequestEvent.class, event -> port.handleNoteOpenRequest(event.getNote())));

        subscriptions.add(eventBus.subscribe(NoteEvents.TrashItemSelectedEvent.class,
                event -> port.handleTrashItemSelected(event.getComponent())));

        subscriptions.add(eventBus.subscribe(NoteEvents.NoteModifiedEvent.class, event -> port.handleNoteModified(event.getNote())));
        return subscriptions;
    }
}
