package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.forevernote.data.dao.FolderDAOSQLite;
import com.example.forevernote.data.dao.NoteDAOSQLite;
import com.example.forevernote.data.dao.TagDAOSQLite;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;

class NoteDAOSQLiteTest {

    private Connection connection;
    private NoteDAOSQLite noteDAO;
    private FolderDAOSQLite folderDAO;
    private TagDAOSQLite tagFAO;

    @BeforeEach
    public void setUp() throws SQLException {
        // Configurar la conexión a la base de datos en memoria H2
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        Statement stmt = connection.createStatement();
        
        // Crear las tablas necesarias para las pruebas        
        stmt.execute("CREATE TABLE IF NOT EXISTS folders ("
    		    + "folder_id INTEGER PRIMARY KEY AUTO_INCREMENT, "
    		    + "parent_id INTEGER, "
    		    + "title TEXT NOT NULL UNIQUE, "
    		    + "created_date TEXT NOT NULL, "
    		    + "modified_date TEXT DEFAULT NULL, "
    		    + "FOREIGN KEY (parent_id) REFERENCES folders(folder_id) "
    		    + "ON UPDATE CASCADE "
    		    + "ON DELETE SET NULL"
    	    + ");");
        
        stmt.execute("CREATE TABLE IF NOT EXISTS notes ("
    			+ "note_id INTEGER PRIMARY KEY AUTO_INCREMENT, "
    			+ "parent_id INTEGER, "
    			+ "title TEXT NOT NULL UNIQUE, "
    			+ "content TEXT DEFAULT NULL, "
    			+ "created_date TEXT NOT NULL, "
    			+ "modified_date TEXT DEFAULT NULL, "
    			+ "latitude REAL NOT NULL DEFAULT 0 CHECK (latitude BETWEEN -90 AND 90), "
    			+ "longitude REAL NOT NULL DEFAULT 0 CHECK (longitude BETWEEN -180 AND 180), "
    			+ "author TEXT DEFAULT NULL, "
    			+ "source_url TEXT DEFAULT NULL, "
    			+ "is_todo INTEGER NOT NULL DEFAULT 0 CHECK (is_todo IN (0, 1)), "
    			+ "todo_due TEXT DEFAULT NULL, "
    			+ "todo_completed TEXT DEFAULT NULL, "
    			+ "source TEXT DEFAULT NULL, "
    			+ "source_application TEXT DEFAULT NULL, "
    			+ "is_favorite INTEGER NOT NULL DEFAULT 0 CHECK (is_favorite IN (0, 1)), "
    			+ "FOREIGN KEY (parent_id) REFERENCES folders(folder_id) "
    			+ "ON UPDATE CASCADE "
    			+ "ON DELETE SET NULL"
    		+ ");");
        
        stmt.execute("CREATE TABLE IF NOT EXISTS tags("
    	        + "tag_id INTEGER PRIMARY KEY AUTO_INCREMENT, "
    	        + "title TEXT NOT NULL UNIQUE, "
    	        + "created_date TEXT NOT NULL, "
    	        + "modified_date TEXT DEFAULT NULL"
    	        + ")");
        
        stmt.execute("CREATE TABLE IF NOT EXISTS tagsNotes("
                + "id INTEGER PRIMARY KEY AUTO_INCREMENT, "
                + "tag_id INTEGER, "
                + "note_id INTEGER, "
                + "added_date TEXT NOT NULL, "
                + "FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON UPDATE CASCADE ON DELETE CASCADE, "
                + "FOREIGN KEY (note_id) REFERENCES notes(note_id) ON UPDATE CASCADE ON DELETE CASCADE)");
        
        noteDAO = new NoteDAOSQLite(connection);
        folderDAO = new FolderDAOSQLite(connection);
        tagFAO = new TagDAOSQLite(connection);
    }

    @AfterEach
    public void tearDown() throws SQLException {    	
        if (connection != null) {
        	Statement stmt = connection.createStatement();
        	stmt.execute("DROP TABLE tagsNotes");
        	stmt.execute("DROP TABLE tags");
        	stmt.execute("DROP TABLE notes");
        	stmt.execute("DROP TABLE folders");
        	
            connection.close();
        }
    }

    @Test
    public void testCreateNote() throws SQLException {
        Note note = new Note("Test Title", "Test CreateNote");
        int noteId = noteDAO.createNote(note);

        assertTrue(noteId > 0);

        Note retrievedNote = noteDAO.getNoteById(noteId);
        
        assertNotNull(retrievedNote);
        assertEquals(note, retrievedNote);
        assertEquals(note.getId(), retrievedNote.getId());
        assertEquals(note.getTitle(), retrievedNote.getTitle());
        assertEquals(note.getContent(), retrievedNote.getContent());
    }

    @Test
    public void testGetNoteById() throws SQLException {
        Note note = new Note("Test Title", "Test getNoteById");
        int noteId = noteDAO.createNote(note);

        Note retrievedNote = noteDAO.getNoteById(noteId);
        assertNotNull(retrievedNote);
        assertEquals(note, retrievedNote);
        assertEquals(note.getId(), retrievedNote.getId());
        assertEquals(note.getTitle(), retrievedNote.getTitle());
        assertEquals(note.getContent(), retrievedNote.getContent());
    }

    @Test
    public void testUpdateNote() throws SQLException {
        Note note = new Note("Test Title", "Test updateNote", null, null);
        int noteId = noteDAO.createNote(note);
        
        assertNull(note.getModifiedDate());
        
        note.setTitle("Updated Title");
        note.setContent("Updated Content");
        noteDAO.updateNote(note);
        
        Note updatedNote = noteDAO.getNoteById(noteId);
        assertEquals(note.getId(), updatedNote.getId());
        assertEquals("Updated Title", updatedNote.getTitle());
        assertEquals("Updated Content", updatedNote.getContent());
        assertNotNull(updatedNote.getModifiedDate());
    }

    @Test
    public void testDeleteNote() throws SQLException {
        Note note = new Note("Test Title", "Test deleteNote");
        int noteId = noteDAO.createNote(note);
        
        noteDAO.deleteNote(noteId);
        
        Note deletedNote = noteDAO.getNoteById(noteId);
        assertNull(deletedNote);
    }

    @Test
    public void testFetchAllNotes() throws SQLException {
        Note note1 = new Note("Title 1", "Content 1");
        Note note2 = new Note("Title 2", "Content 2");
        noteDAO.createNote(note1);
        noteDAO.createNote(note2);
        
        List<Note> notes = noteDAO.fetchAllNotes();
        assertEquals(2, notes.size());
    }

    @Test
    public void testFetchNotesByFolderId() throws SQLException {
        Note note1 = new Note("Title 1", "Content 1");
        noteDAO.createNote(note1);
        
        Note note2 = new Note("Title 2", "Content 2");
        noteDAO.createNote(note2);

        Folder folder = new Folder("Folder 1");
        folderDAO.createFolder(folder);
        
        folderDAO.addNote(folder, note1);
        folderDAO.addNote(folder, note2);
        
        
        List<Note> notes = noteDAO.fetchNotesByFolderId(folder.getId());
        assertEquals(2, notes.size());
        
        assertEquals(notes.get(0), note1);
        assertEquals(notes.get(0).getId(), note1.getId());
        assertEquals(notes.get(0).getTitle(), note1.getTitle());
        assertEquals(notes.get(0).getContent(), note1.getContent());
        
        assertEquals(notes.get(1), note2);
        assertEquals(notes.get(1).getId(), note2.getId());
        assertEquals(notes.get(1).getTitle(), note2.getTitle());
        assertEquals(notes.get(1).getContent(), note2.getContent());
    }

    @Test
    public void testAddAndRemoveTag() throws SQLException {
        Note note = new Note("Test Title", "Test Content");
        int noteId = noteDAO.createNote(note);
        
        com.example.forevernote.data.models.Tag tag = new Tag("Test Tag Title");
        tagFAO.createTag(tag);
        
        noteDAO.addTag(note, tag);
        List<com.example.forevernote.data.models.Tag> tags = noteDAO.fetchTags(noteId);
        assertEquals(1, tags.size());
        
        noteDAO.removeTag(note, tag);
        tags = noteDAO.fetchTags(noteId);
        assertEquals(0, tags.size());
    }
}
