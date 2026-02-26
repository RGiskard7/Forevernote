package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.forevernote.data.dao.filesystem.FolderDAOFileSystem;
import com.example.forevernote.data.dao.filesystem.NoteDAOFileSystem;
import com.example.forevernote.data.dao.filesystem.TagDAOFileSystem;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;

class FileSystemDAOContractTest {

    @TempDir
    Path tempDir;

    private NoteDAOFileSystem noteDAO;
    private FolderDAOFileSystem folderDAO;
    private TagDAOFileSystem tagDAO;

    @BeforeEach
    void setUp() {
        String root = tempDir.toString();
        noteDAO = new NoteDAOFileSystem(root);
        folderDAO = new FolderDAOFileSystem(root);
        tagDAO = new TagDAOFileSystem(noteDAO);
    }

    @Test
    void moveNoteBetweenRootAndFolderKeepsConsistentIdsAndParent() {
        Folder folder = new Folder("Work");
        folderDAO.createFolder(folder);

        Note note = new Note("Task", "content");
        noteDAO.createNote(note);
        String rootId = note.getId();
        assertFalse(rootId.contains("/"));

        folderDAO.addNote(folder, note);
        assertTrue(note.getId().startsWith(folder.getId() + "/"));

        Folder detected = noteDAO.getFolderOfNote(note.getId());
        assertNotNull(detected);
        assertEquals(folder.getId(), detected.getId());

        folderDAO.removeNote(folder, note);
        assertFalse(note.getId().contains("/"));
        assertNotNull(note.getParent());
        assertEquals("ROOT", note.getParent().getId());
    }

    @Test
    void addAndRemoveTagsByIdAndRenameTagWorksInFilesystemMode() {
        Note note = new Note("Tagged", "content");
        noteDAO.createNote(note);

        noteDAO.addTag(note.getId(), "Work");
        List<Tag> tags = noteDAO.fetchTags(note.getId());
        assertEquals(1, tags.size());
        assertEquals("Work", tags.get(0).getTitle());

        Tag renameTag = new Tag("Work", "Office");
        tagDAO.updateTag(renameTag);

        Note reloaded = noteDAO.getNoteById(note.getId());
        assertNotNull(reloaded);
        assertTrue(reloaded.getTags().stream().anyMatch(t -> "Office".equals(t.getTitle())));

        noteDAO.removeTag(note.getId(), "Office");
        List<Tag> afterRemove = noteDAO.fetchTags(note.getId());
        assertEquals(0, afterRemove.size());
    }

    @Test
    void rootFolderContractIsConsistent() {
        Note note = new Note("Root note", "content");
        noteDAO.createNote(note);

        Folder rootById = folderDAO.getFolderById("ROOT");
        assertNotNull(rootById);
        assertEquals("ROOT", rootById.getId());

        Folder rootByNote = folderDAO.getFolderByNoteId(note.getId());
        assertNotNull(rootByNote);
        assertEquals("ROOT", rootByNote.getId());
    }
}
