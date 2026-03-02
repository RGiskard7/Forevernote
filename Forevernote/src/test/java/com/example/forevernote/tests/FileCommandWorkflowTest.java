package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.ui.workflow.FileCommandWorkflow;
import com.example.forevernote.ui.workflow.NoteWorkflow;

class FileCommandWorkflowTest {

    @Test
    void handleNewNoteShouldInvokeUiCallbacksOnSuccess() {
        FileCommandWorkflow workflow = new FileCommandWorkflow();
        Folder folder = new Folder("Inbox");
        folder.setId("inbox");

        AtomicInteger createNoteCalls = new AtomicInteger(0);
        AtomicInteger addToFolderCalls = new AtomicInteger(0);
        AtomicReference<String> requestedTitle = new AtomicReference<>();
        AtomicInteger onCreatedCalls = new AtomicInteger(0);
        AtomicInteger onAfterCreateCalls = new AtomicInteger(0);
        AtomicInteger onErrorCalls = new AtomicInteger(0);

        NoteService noteService = new NoteService(null, null, null) {
            @Override
            public Note createNote(Note note) {
                createNoteCalls.incrementAndGet();
                if (note.getId() == null || note.getId().isBlank()) {
                    note.setId("generated-id");
                }
                return note;
            }
        };

        FolderService folderService = new FolderService(null, null) {
            @Override
            public void addNoteToFolder(Folder targetFolder, Note note) {
                addToFolderCalls.incrementAndGet();
            }
        };

        NoteWorkflow noteWorkflow = new NoteWorkflow(null) {
            @Override
            public NoteCreationResult createNewNote(String title, Folder currentFolder, boolean isFileSystem,
                    NoteCreationPort creationPort) {
                requestedTitle.set(title);
                Note note = new Note("Created", "content");
                creationPort.createNote(note);
                creationPort.addNoteToFolder(currentFolder, note);
                return new NoteCreationResult(true, note, null);
            }
        };

        Function<String, String> i18n = key -> "action.new_note".equals(key) ? "Nueva nota" : key;

        boolean handled = workflow.handleNewNote(
                noteWorkflow,
                noteService,
                folderService,
                folder,
                i18n,
                new FileCommandWorkflow.NoteCreationUiPort() {
                    @Override
                    public void onCreated(Note note) {
                        onCreatedCalls.incrementAndGet();
                    }

                    @Override
                    public void onAfterCreate() {
                        onAfterCreateCalls.incrementAndGet();
                    }

                    @Override
                    public void onError(String statusKey) {
                        onErrorCalls.incrementAndGet();
                    }
                });

        assertTrue(handled);
        assertEquals("Nueva nota", requestedTitle.get());
        assertEquals(1, createNoteCalls.get());
        assertEquals(1, addToFolderCalls.get());
        assertEquals(1, onCreatedCalls.get());
        assertEquals(1, onAfterCreateCalls.get());
        assertEquals(0, onErrorCalls.get());
    }

    @Test
    void handleNewNoteShouldReportErrorWhenCreationFails() {
        FileCommandWorkflow workflow = new FileCommandWorkflow();
        AtomicInteger onCreatedCalls = new AtomicInteger(0);
        AtomicInteger onAfterCreateCalls = new AtomicInteger(0);
        AtomicReference<String> errorKey = new AtomicReference<>();

        NoteWorkflow failingWorkflow = new NoteWorkflow(null) {
            @Override
            public NoteCreationResult createNewNote(String title, Folder currentFolder, boolean isFileSystem,
                    NoteCreationPort creationPort) {
                return new NoteCreationResult(false, null, "boom");
            }
        };

        boolean handled = workflow.handleNewNote(
                failingWorkflow,
                new NoteService(null, null, null),
                new FolderService(null, null),
                null,
                key -> key,
                new FileCommandWorkflow.NoteCreationUiPort() {
                    @Override
                    public void onCreated(Note note) {
                        onCreatedCalls.incrementAndGet();
                    }

                    @Override
                    public void onAfterCreate() {
                        onAfterCreateCalls.incrementAndGet();
                    }

                    @Override
                    public void onError(String statusKey) {
                        errorKey.set(statusKey);
                    }
                });

        assertFalse(handled);
        assertEquals("status.error_creating_note", errorKey.get());
        assertEquals(0, onCreatedCalls.get());
        assertEquals(0, onAfterCreateCalls.get());
    }

    @Test
    void handleSaveAndDeleteAndSaveAllShouldRunProvidedActions() {
        FileCommandWorkflow workflow = new FileCommandWorkflow();

        AtomicInteger saveCalls = new AtomicInteger(0);
        AtomicInteger refreshCalls = new AtomicInteger(0);
        workflow.handleSave(v -> saveCalls.incrementAndGet(), () -> refreshCalls.incrementAndGet());
        assertEquals(1, saveCalls.get());
        assertEquals(1, refreshCalls.get());

        AtomicInteger deleteCalls = new AtomicInteger(0);
        workflow.handleDelete(v -> deleteCalls.incrementAndGet());
        assertEquals(1, deleteCalls.get());

        AtomicInteger saveAllCalls = new AtomicInteger(0);
        AtomicReference<String> saveAllStatus = new AtomicReference<>();
        workflow.handleSaveAll(
                () -> saveAllCalls.incrementAndGet(),
                key -> "status.saved_all".equals(key) ? "Guardado todo" : key,
                saveAllStatus::set);
        assertEquals(1, saveAllCalls.get());
        assertEquals("Guardado todo", saveAllStatus.get());
    }
}
