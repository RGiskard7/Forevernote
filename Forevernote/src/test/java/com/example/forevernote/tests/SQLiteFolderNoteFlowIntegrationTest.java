package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.forevernote.data.dao.sqlite.FolderDAOSQLite;
import com.example.forevernote.data.dao.sqlite.NoteDAOSQLite;
import com.example.forevernote.data.dao.sqlite.TagDAOSQLite;
import com.example.forevernote.data.database.SQLiteDB;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;

class SQLiteFolderNoteFlowIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void createNoteInEmptyFolderShouldBeVisibleImmediatelyForCountQueries() throws Exception {
        Path dbFile = tempDir.resolve("flow-count.sqlite");

        resetSQLiteDbSingleton();
        SQLiteDB.configure(dbFile.toString());
        SQLiteDB db = SQLiteDB.getInstance();
        db.initDatabase();

        Connection connection = db.openConnection();
        try {
            FolderDAOSQLite folderDAO = new FolderDAOSQLite(connection);
            NoteDAOSQLite noteDAO = new NoteDAOSQLite(connection);
            TagDAOSQLite tagDAO = new TagDAOSQLite(connection);
            FolderService folderService = new FolderService(folderDAO, noteDAO);
            NoteService noteService = new NoteService(noteDAO, folderDAO, tagDAO);

            Folder folder = folderService.createFolder("Inbox");
            assertEquals(0, noteService.getNotesByFolder(folder).size(),
                    "A new folder must start with zero notes.");

            Note first = noteService.createNote("First", "content");
            folderService.addNoteToFolder(folder, first);
            assertEquals(1, noteService.getNotesByFolder(folder).size(),
                    "After first create+assign, folder note count query must update immediately.");

            Note second = noteService.createNote("Second", "content");
            folderService.addNoteToFolder(folder, second);
            assertEquals(2, noteService.getNotesByFolder(folder).size(),
                    "After second create+assign, folder note count query must update immediately.");
        } finally {
            db.closeConnection(connection);
            resetSQLiteDbSingleton();
        }
    }

    private void resetSQLiteDbSingleton() throws Exception {
        Field instanceField = SQLiteDB.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
