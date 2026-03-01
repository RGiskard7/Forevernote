package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.ui.workflow.NoteWorkflow;

class NoteWorkflowTest {

    @Test
    void refreshNotesListUsesFolderFilterWhenFolderIsSelected() {
        RecordingNotesPort notesPort = new RecordingNotesPort();
        RecordingSidebarPort sidebarPort = new RecordingSidebarPort();
        NoteWorkflow workflow = new NoteWorkflow(null);

        Folder folder = new Folder("f1", "Work");
        workflow.refreshNotesList(
                "folder",
                folder,
                null,
                "",
                false,
                notesPort,
                sidebarPort,
                () -> {
                });

        assertEquals(List.of("folder:f1"), notesPort.calls);
        assertEquals(0, sidebarPort.favoritesReloadCount);
    }

    @Test
    void refreshNotesListUsesSearchAndGridRefresh() {
        RecordingNotesPort notesPort = new RecordingNotesPort();
        RecordingSidebarPort sidebarPort = new RecordingSidebarPort();
        NoteWorkflow workflow = new NoteWorkflow(null);
        int[] gridRefresh = { 0 };

        workflow.refreshNotesList(
                "search",
                null,
                null,
                "roadmap",
                true,
                notesPort,
                sidebarPort,
                () -> gridRefresh[0]++);

        assertEquals(List.of("search:roadmap"), notesPort.calls);
        assertEquals(1, gridRefresh[0]);
    }

    @Test
    void refreshNotesListUsesFavoritesPathWithoutLoadingAllNotes() {
        RecordingNotesPort notesPort = new RecordingNotesPort();
        RecordingSidebarPort sidebarPort = new RecordingSidebarPort();
        NoteWorkflow workflow = new NoteWorkflow(null);

        workflow.refreshNotesList(
                "favorites",
                null,
                new Tag("t1", "fav"),
                "",
                false,
                notesPort,
                sidebarPort,
                () -> {
                });

        assertEquals(List.of(), notesPort.calls);
        assertEquals(1, sidebarPort.favoritesReloadCount);
    }

    @Test
    void createNewNoteInRootShouldNotAssignParentPath() {
        NoteWorkflow workflow = new NoteWorkflow(null);
        AtomicReference<Note> created = new AtomicReference<>();
        AtomicInteger addToFolderCalls = new AtomicInteger(0);

        NoteWorkflow.NoteCreationResult result = workflow.createNewNote(
                "My Note",
                null,
                false,
                new NoteWorkflow.NoteCreationPort() {
                    @Override
                    public Note createNote(Note note) {
                        created.set(note);
                        note.setId("n-1");
                        return note;
                    }

                    @Override
                    public void addNoteToFolder(Folder folder, Note note) {
                        addToFolderCalls.incrementAndGet();
                    }
                });

        assertTrue(result.success());
        assertNotNull(result.note());
        assertEquals("n-1", result.note().getId());
        assertEquals(0, addToFolderCalls.get());
        assertEquals("My Note", created.get().getTitle());
    }

    @Test
    void createNewNoteInFolderForFileSystemShouldSeedPathAndAttach() {
        NoteWorkflow workflow = new NoteWorkflow(null);
        AtomicReference<Note> created = new AtomicReference<>();
        AtomicReference<Folder> attachedFolder = new AtomicReference<>();

        Folder folder = new Folder("projects", "Projects");
        NoteWorkflow.NoteCreationResult result = workflow.createNewNote(
                "Roadmap",
                folder,
                true,
                new NoteWorkflow.NoteCreationPort() {
                    @Override
                    public Note createNote(Note note) {
                        created.set(note);
                        note.setId("projects/Roadmap.md");
                        return note;
                    }

                    @Override
                    public void addNoteToFolder(Folder targetFolder, Note note) {
                        attachedFolder.set(targetFolder);
                    }
                });

        assertTrue(result.success());
        assertNotNull(result.note());
        assertEquals("projects/Roadmap.md", result.note().getId());
        assertEquals(folder, attachedFolder.get());
        assertTrue(created.get().getId().startsWith("projects"));
    }

    @Test
    void toggleFavoriteShouldPersistAndReturnFavoriteStatusKey() {
        NoteWorkflow workflow = new NoteWorkflow(null);
        AtomicInteger updateCalls = new AtomicInteger(0);
        Note note = new Note("n1", "Roadmap", "");

        NoteWorkflow.NoteToggleResult result = workflow.toggleFavorite(note, n -> updateCalls.incrementAndGet());

        assertTrue(result.success());
        assertTrue(note.isFavorite());
        assertEquals("status.note_marked_favorite", result.successStatusKey());
        assertEquals(1, updateCalls.get());
    }

    @Test
    void togglePinShouldPersistAndReturnPinStatusKey() {
        NoteWorkflow workflow = new NoteWorkflow(null);
        AtomicInteger updateCalls = new AtomicInteger(0);
        Note note = new Note("n2", "Sprint", "");

        NoteWorkflow.NoteToggleResult result = workflow.togglePin(note, n -> updateCalls.incrementAndGet());

        assertTrue(result.success());
        assertTrue(note.isPinned());
        assertEquals("status.note_pinned", result.successStatusKey());
        assertEquals(1, updateCalls.get());
    }

    private static final class RecordingNotesPort implements NoteWorkflow.NotesListPort {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void loadAllNotes() {
            calls.add("all");
        }

        @Override
        public void loadNotesForFolder(Folder folder) {
            calls.add("folder:" + folder.getId());
        }

        @Override
        public void loadNotesForTag(String tagTitle) {
            calls.add("tag:" + tagTitle);
        }

        @Override
        public void performSearch(String query) {
            calls.add("search:" + query);
        }
    }

    private static final class RecordingSidebarPort implements NoteWorkflow.SidebarPort {
        private int favoritesReloadCount = 0;

        @Override
        public void loadFavorites() {
            favoritesReloadCount++;
        }
    }
}
