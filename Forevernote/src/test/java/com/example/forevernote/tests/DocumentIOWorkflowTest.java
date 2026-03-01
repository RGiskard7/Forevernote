package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.ui.workflow.DocumentIOWorkflow;

class DocumentIOWorkflowTest {

    @TempDir
    Path tempDir;

    @Test
    void importFilesSqliteShouldCreateAndAttachToConcreteFolder() throws Exception {
        DocumentIOWorkflow workflow = new DocumentIOWorkflow();
        Path source = tempDir.resolve("roadmap.md");
        Files.writeString(source, "contenido", StandardCharsets.UTF_8);

        AtomicReference<Note> createdNote = new AtomicReference<>();
        AtomicInteger addToFolderCalls = new AtomicInteger(0);
        Folder folder = new Folder("f-1", "Work");

        DocumentIOWorkflow.ImportResult result = workflow.importFiles(
                List.of(source.toFile()),
                folder,
                false,
                new DocumentIOWorkflow.ImportPort() {
                    @Override
                    public Note createNote(Note note) {
                        createdNote.set(note);
                        note.setId("note-1");
                        return note;
                    }

                    @Override
                    public void addNoteToFolder(Folder currentFolder, Note note) {
                        addToFolderCalls.incrementAndGet();
                    }
                });

        assertEquals(1, result.importedCount());
        assertEquals(0, result.failedCount());
        assertNotNull(createdNote.get());
        assertEquals("roadmap", createdNote.get().getTitle());
        assertEquals(1, addToFolderCalls.get());
    }

    @Test
    void importFilesFileSystemShouldSeedPathAndSkipAttach() throws Exception {
        DocumentIOWorkflow workflow = new DocumentIOWorkflow();
        Path source = tempDir.resolve("plan 2026!.txt");
        Files.writeString(source, "texto", StandardCharsets.UTF_8);

        AtomicReference<Note> createdNote = new AtomicReference<>();
        AtomicInteger addToFolderCalls = new AtomicInteger(0);
        Folder folder = new Folder("projects", "Projects");

        DocumentIOWorkflow.ImportResult result = workflow.importFiles(
                List.of(source.toFile()),
                folder,
                true,
                new DocumentIOWorkflow.ImportPort() {
                    @Override
                    public Note createNote(Note note) {
                        createdNote.set(note);
                        note.setId(note.getId());
                        return note;
                    }

                    @Override
                    public void addNoteToFolder(Folder currentFolder, Note note) {
                        addToFolderCalls.incrementAndGet();
                    }
                });

        assertEquals(1, result.importedCount());
        assertEquals(0, result.failedCount());
        assertTrue(createdNote.get().getId().startsWith("projects" + File.separator));
        assertEquals(0, addToFolderCalls.get());
    }

    @Test
    void exportNoteMarkdownShouldWriteHeaderAndContent() throws Exception {
        DocumentIOWorkflow workflow = new DocumentIOWorkflow();
        Note note = new Note("Weekly", "Resumen");
        Path target = tempDir.resolve("weekly.md");

        DocumentIOWorkflow.ExportResult result = workflow.exportNote(note, target.toFile());

        assertTrue(result.success());
        String content = Files.readString(target, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("# Weekly"));
        assertTrue(content.contains("Resumen"));
    }

    @Test
    void exportNoteShouldFailWhenNoteIsNull() {
        DocumentIOWorkflow workflow = new DocumentIOWorkflow();
        DocumentIOWorkflow.ExportResult result = workflow.exportNote(null, tempDir.resolve("x.txt").toFile());
        assertFalse(result.success());
    }
}
