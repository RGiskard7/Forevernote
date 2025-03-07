package com.example.forevernote.data.dao;

import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.logging.*;
import java.time.format.DateTimeFormatter;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.*;
import com.example.forevernote.data.models.*;
import com.example.forevernote.exceptions.*;

/**
 * SQLite implementation of the NoteDAO interface.
 * This class provides methods for interacting with notes in the SQLite database,
 * including creation, retrieval, updating, deletion, and tag management.
 */
public class NoteDAOSQLite implements NoteDAO {
	
	// SQL Queries
    private static final String INSERT_NOTE_SQL = "INSERT INTO notes (title, content, created_date, modified_date, "
    		+ "latitude, longitude, author, source_url, source, source_application, is_todo, todo_due, todo_completed) "
			+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_TAG_NOTE_SQL = "INSERT INTO tagsNotes (tag_id, note_id, added_date) VALUES (?, ?, ?)";
    
    private static final String SELECT_NOTE_BY_ID_SQL = "SELECT * FROM notes LEFT JOIN folders ON notes.parent_id = folders.folder_id "
    		+ "WHERE note_id = ?";
    
    private static final String SELECT_NOTES_BY_FOLDER_ID_SQL = "SELECT * FROM notes WHERE parent_id = ?";
    
    private static final String SELECT_ALL_NOTES_SQL = "SELECT * FROM notes";
    
    private static final String SELECT_ALL_TAGS_NOTE_SQL = "SELECT DISTINCT tags.tag_id, title, created_date, modified_date "
    		+ "FROM tagsNotes NATURAL JOIN tags WHERE note_id = ?";
    
    private static final String UPDATE_NOTE_SQL = "UPDATE notes SET title = ?, content = ?, modified_date = ? WHERE note_id = ?";
    
    private static final String DELETE_NOTE_SQL = "DELETE FROM notes WHERE note_id = ?";
    
    private static final String DELETE_TAG_NOTE_SQL = "DELETE FROM tagsNotes WHERE tag_id = ? AND note_id = ?";
    
	private static final Logger logger = LoggerConfig.getLogger(NoteDAOSQLite.class);
    private Connection connection;

    /**
     * Constructs a NoteDAOSQLite with the given database connection.
     *
     * @param connection The database connection to be used.
     */
	public NoteDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	// CRUD Methods
	@Override
	public int createNote(Note note) {
	    int newId = -1;
	    
	    if (note == null) {
	    	throw new InvalidParameterException("Note object cannot be null");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(INSERT_NOTE_SQL, 
	    		Statement.RETURN_GENERATED_KEYS)) {
	    	
	        /*if (note.getParent() != null) {
	        	pstmt.setInt(1, note.getParent().getId());
	        }*/
	    	
	        pstmt.setString(1, note.getTitle());
	        pstmt.setString(2, note.getContent());
	        pstmt.setString(3, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setString(4, note.getModifiedDate());
	        pstmt.setInt(5, note.getLatitude());
	        pstmt.setInt(6, note.getLongitude());
	        pstmt.setString(7, note.getAuthor());
	        pstmt.setString(8, note.getSourceUrl());
	        pstmt.setString(9, note.getSource());
	        pstmt.setString(10, note.getSourceApplication());
	        
	        if (note instanceof ToDoNote) {
	        	pstmt.setInt(11, 1); //is_todo
	        	pstmt.setString(12, ((ToDoNote)note).getToDoDue());
	        	pstmt.setString(13, ((ToDoNote)note).getToDoCompleted());
	        } else {
	        	pstmt.setInt(11, 0);
	        	pstmt.setString(12, null);
	        	pstmt.setString(13, null);
	        }
	
	        pstmt.executeUpdate();

	        // Obtener el ID generado
	        try(ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
		        if (generatedKeys.next()) {
		            newId = generatedKeys.getInt(1); // Asigna el ID de la nueva nota
		            note.setId(newId);
		        }
	        }
	        
	        connection.commit(); // Confirmar transacción
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error createNote(): " + e.getMessage(), e);
	    } 
	    
	    return newId; // Retorna el ID de la nueva nota o -1 si hubo un error
	}


	@Override
	public Note getNoteById(int id) {
	    Note note = null;

	    if (id <= 0) {
	    	throw new IllegalArgumentException("Note ID must be greater than zero");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(SELECT_NOTE_BY_ID_SQL)) {
	        pstmt.setInt(1, id);
	        
	        try(ResultSet rs = pstmt.executeQuery()) {
		        // Process the result set
		        if (rs.next()) {
		        	note = mapResultSetToNote(rs);
		        }
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error getNoteById(): " + e.getMessage(), e);
	    }

	    return note;
	}
		
	@Override
	public void updateNote(Note note) {
		if (note == null) {
			throw new IllegalArgumentException("Note object cannot be null");
		}
				
	    try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_NOTE_SQL)) {
	        pstmt.setString(1, note.getTitle());
	        pstmt.setString(2, note.getContent());
	        pstmt.setString(3, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(4, note.getId());
	        pstmt.executeUpdate();
	        connection.commit();
	        
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error updateNote(): " + e.getMessage(), e);
	    }
	}

	@Override
	public void deleteNote(int id) {
		if (id <= 0) {
			throw new IllegalArgumentException("Note ID must be greater than zero");
		}
		
		try (PreparedStatement pstmt = connection.prepareStatement(DELETE_NOTE_SQL)) {
	        pstmt.setInt(1, id);
	        pstmt.executeUpdate();
	        connection.commit();
	        
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error deleteNote(): " + e.getMessage(), e);
		}	
	}
	
	// Retrieval Methods
	@Override
	public List<Note> fetchNotesByFolderId(int folderId) {
		if (folderId <= 0) {
			throw new InvalidParameterException("Invalid folder ID or provided list is null");
		}
		
		List<Note> list = new ArrayList<>();

		try (PreparedStatement pstmt = connection.prepareStatement(SELECT_NOTES_BY_FOLDER_ID_SQL)) {		    
            pstmt.setInt(1, folderId);        	

            try(ResultSet rs = pstmt.executeQuery()) {
    		    while (rs.next()) {
    		        list.add(mapResultSetToNote(rs));
    		    }
            }
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error fetchNotesByFolderId(): " + e.getMessage(), e);
		}
		
		return list;
	}
	
	@Override
	public void fetchNotesByFolderId(Folder folder) {
	    if (folder == null) {
	    	throw new IllegalArgumentException("Folder object can't be null");
	    }

		try (PreparedStatement pstmt = connection.prepareStatement(SELECT_NOTES_BY_FOLDER_ID_SQL)) {		    
            pstmt.setInt(1, folder.getId());        	

            try(ResultSet rs = pstmt.executeQuery()) {
    		    while (rs.next()) {
    		    	Note note = mapResultSetToNote(rs);
    		    	folder.add(note);
    		    	note.setParent(folder);
    		    }
            }
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error fetchNotesByFolderId(): " + e.getMessage(), e);
		}
	}

	@Override
	public List<Note> fetchAllNotes() {		
		List<Note> list = new ArrayList<>();

		try (Statement stmt = connection.createStatement()) {		    
			try (ResultSet rs = stmt.executeQuery(SELECT_ALL_NOTES_SQL)) {
			    while (rs.next()) {
		            list.add(mapResultSetToNote(rs));
			    }
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error fetchAllNotes(): " + e.getMessage(), e);
		} 
		
		return list;
	}

	@Override
	public Folder getFolderOfNote(int noteId) {
		FolderDAO folderDAO = new FolderDAOSQLite(connection);
	    return folderDAO.getFolderByNoteId(noteId);
	}
	
	// Tag Management Methods
	@Override
	public void addTag(int noteId, int tagId) {
	    if (noteId <= 0 || tagId <= 0) {
	    	throw new IllegalArgumentException("Note ID and tag ID must be greater than zero");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(INSERT_TAG_NOTE_SQL)) {
	        pstmt.setInt(1, tagId);
	        pstmt.setInt(2, noteId);
	        pstmt.setString(3, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.executeUpdate();
	        connection.commit();
	        
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error addTag(): " + e.getMessage(), e);
	    } 
	}
	
	@Override
	public void addTag(Note note, Tag tag) {
		if (note == null || tag == null) {
			throw new InvalidParameterException("Note object or tag object are null");
		}
		
		addTag(note.getId(), tag.getId());
	}
	
	@Override
	public void removeTag(int noteId, int tagId) {
		if (tagId <= 0 || noteId <= 0) {
	    	throw new IllegalArgumentException("Note ID and tag ID must be greater than zero");
	    }
		
		try (PreparedStatement pstmt = connection.prepareStatement(DELETE_TAG_NOTE_SQL)) {		    
	        pstmt.setInt(1, tagId);
	        pstmt.setInt(2, noteId);
	        pstmt.executeUpdate();
	        connection.commit();
	        
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error removeTag(): " + e.getMessage(), e);
		}
	}
	
	@Override
	public void removeTag(Note note, Tag tag) {
		if (note == null || tag == null) {
			throw new InvalidParameterException("Note object or tag object are null");
		}
		
		removeTag(note.getId(), tag.getId());
	}

	@Override
	public List<Tag> fetchTags(int noteId) {
		if (noteId <= 0) {
			throw new InvalidParameterException("Invalid note ID");
		}
		
		List<Tag> list = new ArrayList<>();

		try (PreparedStatement pstmt = connection.prepareStatement(SELECT_ALL_TAGS_NOTE_SQL)) {
            pstmt.setInt(1, noteId);        	

            try (ResultSet rs = pstmt.executeQuery()) {
    		    while (rs.next()) {
    		        int id = rs.getInt("tag_id");
    		        String title = rs.getString("title");
    		        String createdDate = rs.getString("created_date");
    		        String modifiedDate = rs.getString("modified_date");
    		        
    		        list.add(new Tag(id, title, createdDate, modifiedDate));
    		    }	
            }
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error fetchTags(): " + e.getMessage(), e);
		}
		
		return list;
	}
	
	@Override
	public void loadTags(Note note) {
	    if (note == null) {
	    	throw new InvalidParameterException("Note object cannot be null");
	    }
		
	    List<Tag> tags = fetchTags(note.getId());
	    note.addAllTags(tags);
	}
	
	// Helper Methods (protected/private)
	protected Note mapResultSetToNote(ResultSet rs) throws SQLException {
		Note note = null;
		
		if (rs != null) {     
            int noteId = rs.getInt("note_id");
            String title = rs.getString("title");
            String content = rs.getString("content");
            String createdDate = rs.getString("created_date");
            String modifiedDate = rs.getString("modified_date");
            int latitude = rs.getInt("latitude");
            int longitude = rs.getInt("longitude");
            String author = rs.getString("author");
            String sourceUrl = rs.getString("source_url");
            int isToDo = rs.getInt("is_todo");
            String toDoDue = rs.getString("todo_due");
            String toDoCompleted = rs.getString("todo_completed");
            String source = rs.getString("source");
            String sourceApplication = rs.getString("source_application");
                        
            if (isToDo == 1) {
            	note = new ToDoNote(noteId, title, content, createdDate, modifiedDate, toDoDue, toDoCompleted);
            } else {
            	note = new Note(noteId, title, content, createdDate, modifiedDate);
            }

            note.setLatitude(latitude);
            note.setLongitude(longitude);
            note.setAuthor(author);
            note.setSourceUrl(sourceUrl);
            note.setSource(source);
            note.setSourceApplication(sourceApplication);
		}
		
		return note;
	}
}
