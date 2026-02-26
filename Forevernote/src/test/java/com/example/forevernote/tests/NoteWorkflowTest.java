package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.forevernote.data.models.Folder;
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
