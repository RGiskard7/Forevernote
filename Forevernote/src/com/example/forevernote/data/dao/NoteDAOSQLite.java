package com.example.forevernote.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.models.*;
import com.example.forevernote.exceptions.*;

public class NoteDAOSQLite implements INoteDAO {
    private static final String INSERT_NOTE_SQL = "INSERT INTO notes (title, content, created_date) VALUES (?, ?, ?)";
    private static final String INSERT_TAG_NOTE_SQL = "INSERT INTO tagsNotes (tag_id, note_id, added_date) VALUES (?, ?, ?)";
    private static final String SELECT_NOTE_BY_ID_SQL = "SELECT * FROM notes WHERE note_id = ?";
    private static final String SELECT_ALL_NOTES_SQL = "SELECT * FROM notes";
    private static final String SELECT_NOTEBOOK_NOTE_SQL = "SELECT notebook_id FROM notebooksNotes WHERE note_id = ?";
    private static final String SELECT_ALL_TAGS_NOTE_SQL = "SELECT DISTINCT tags.tag_id, tags.title, tags.created_date, tags.modified_date "
    													+ "FROM tagsNotes NATURAL JOIN tags WHERE note_id = ?";
    private static final String DELETE_NOTE_SQL = "DELETE FROM notes WHERE note_id = ?";
    private static final String DELETE_TAG_NOTE_SQL = "DELETE FROM tagsNotes WHERE tag_id = ? AND note_id = ?";
    private static final String UPDATE_NOTE_SQL = "UPDATE notes SET title = ?, content = ?, modified_date = ? WHERE note_id = ?";
    
	private static final Logger logger = LoggerConfig.getLogger(NoteDAOSQLite.class);
    private Connection connection;

	public NoteDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	@Override
	public int createNote(String title, String content) {
	    PreparedStatement pstmt = null;
	    ResultSet generatedKeys = null;
	    int newId = -1;
	    
	    // Verificación de parámetros
	    if (title == null || content == null) {
	    	throw new InvalidParameterException("Title and content cannot be null");
	    }

	    try {
	    	// Preparar y ejecutar la declaración SQL
	    	pstmt = connection.prepareStatement(INSERT_NOTE_SQL, Statement.RETURN_GENERATED_KEYS);
	        pstmt.setString(1, title);
	        pstmt.setString(2, content);
	        pstmt.setString(3, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.executeUpdate();

	        // Obtener el ID generado
	        generatedKeys = pstmt.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            newId = generatedKeys.getInt(1); // Asigna el ID de la nueva nota
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error createNote(): " + e.getMessage(), e);
	    } finally {
	    	// Cerrar los recursos
	        if (generatedKeys != null) {
	            try {
	                generatedKeys.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error createNote(): " + e.getMessage(), e);
	            }
	        }
	        if (pstmt != null) {
	            try {
	                pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error createNote(): " + e.getMessage(), e);
	            }
	        }
	    }
	    
	    // Retorna el ID de la nueva nota o -1 si hubo un error
	    return newId; 
	}


	@Override
	public Note getNoteById(int id) {
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    Note note = null;
		
	    // Check if the provided ID is valid
	    if (id <= 0) {
	    	throw new IllegalArgumentException("Note ID must be greater than zero");
	        
	    }

	    try {
	    	// Prepare the SQL statement
	    	pstmt = connection.prepareStatement(SELECT_NOTE_BY_ID_SQL);
	        pstmt.setInt(1, id);
	        
	        // Execute the query
	        rs = pstmt.executeQuery();

	        // Process the result set
	        if (rs.next()) {
	            int noteId = rs.getInt("note_id");
	            String title = rs.getString("title");
	            String content = rs.getString("content");
	            String createdDate = rs.getString("created_date");
	            String modifiedDate = rs.getString("modified_date");

	            // Create a new Note object
	            note = new Note(noteId, title, content, createdDate, modifiedDate);
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error getNoteById(): " + e.getMessage(), e);
	    } finally {
	        if (rs != null) {
	            try {
	                rs.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getNoteById(): " + e.getMessage(), e);
	            }
	        }
	        if (pstmt != null) {
	            try {
	                pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getNoteById(): " + e.getMessage(), e);
	            }
	        }
	    }

	    // Return the Note object or null if not found
	    return note;
	}


	/*@Override
	public void updateNote(int id, String title, String content) {
		PreparedStatement pstmt = null;
		
		if (id <= 0) {
			throw new IllegalArgumentException("Note ID must be greater than zero");
		}
				
	    try {
	    	pstmt = connection.prepareStatement(UPDATE_NOTE_SQL);
	        pstmt.setString(1, title);
	        pstmt.setString(2, content);
	        pstmt.setString(3, dtf.format(Calendar.getInstance().getTime()));
	        pstmt.setInt(4, id);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error editNote(): " + e.getMessage(), e);
	    } finally {
	        if (pstmt != null) {
	            try {
	                pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error editNote(): " + e.getMessage(), e);
	            }
	        }
	    }
	}*/
	
	@Override
	public void updateNote(Note note) {
		PreparedStatement pstmt = null;
		
		if (note == null) {
			throw new IllegalArgumentException("Note object cannot be null");
		}
				
	    try {
	    	pstmt = connection.prepareStatement(UPDATE_NOTE_SQL);
	        pstmt.setString(1, note.getTitle());
	        pstmt.setString(2, note.getContent());
	        pstmt.setString(3, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(4, note.getId());
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error editNote(): " + e.getMessage(), e);
	    } finally {
	        if (pstmt != null) {
	            try {
	                pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error editNote(): " + e.getMessage(), e);
	            }
	        }
	    }
	}

	@Override
	public void deleteNote(int id) {
	    PreparedStatement pstmt = null;
		
		if (id <= 0) {
			throw new IllegalArgumentException("Note ID must be greater than zero");
		}
		
		try {
			pstmt = connection.prepareStatement(DELETE_NOTE_SQL);
	        pstmt.setInt(1, id);
	        pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error deleteNote(): " + e.getMessage(), e);
		} finally {
	        if (pstmt != null) {
	            try {
	            	pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error deleteNote(): " + e.getMessage(), e);
	            }
	        }
		}	
	}

	@Override
	public void getAllNotes(List<Note> list) {
		Statement stmt = null;
		ResultSet rs = null;
		
		if (list == null) {
			throw new InvalidParameterException("The provided list cannot be null");
		}

		try {		    
			stmt = connection.createStatement();
			rs = stmt.executeQuery(SELECT_ALL_NOTES_SQL);

		    while (rs.next()) {
	            int noteId = rs.getInt("note_id");
	            String title = rs.getString("title");
	            String content = rs.getString("content");
	            String createdDate = rs.getString("created_date");
	            String modifiedDate = rs.getString("modified_date");

	            list.add(new Note(noteId, title, content, createdDate, modifiedDate));
		    }
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error getAllNotes(): " + e.getMessage(), e);
		} finally {
	        if (rs != null) {
	            try {
	                rs.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getAllNotes(): " + e.getMessage(), e);
	            }
	        }
	        if (stmt != null) {
	            try {
	            	stmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getAllNotes(): " + e.getMessage(), e);
	            }
	        }
		}		
	}

	@Override
	public Notebook getNotebookOfNote(int noteId) {
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    
	    NotebookDAOSQLite notebookDAO = null;
	    Notebook notebook = null;
		
	    if (noteId <= 0) {
	    	throw new IllegalArgumentException("Note ID must be greater than zero"); 
	    }

	    try {
	    	pstmt = connection.prepareStatement(SELECT_NOTEBOOK_NOTE_SQL);
	        pstmt.setInt(1, noteId);
	      
	        rs = pstmt.executeQuery();

	        if (rs.next()) {
	            int notebookId = rs.getInt("notebook_id");
	            
	            notebookDAO = new NotebookDAOSQLite(connection);
	            notebook = notebookDAO.getNotebookById(notebookId);
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error getNotebookOfNote(): " + e.getMessage(), e);
	    } finally {
	        if (rs != null) {
	            try {
	                rs.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getNotebookOfNote(): " + e.getMessage(), e);
	            }
	        }
	        if (pstmt != null) {
	            try {
	                pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getNotebookOfNote(): " + e.getMessage(), e);
	            }
	        }
	    }

	    return notebook;
	}
	
	@Override
	public void addTagToNote(int noteId, int tagId) {
	    PreparedStatement pstmt = null;
		
	    if (noteId <= 0 || tagId <= 0) {
	    	throw new IllegalArgumentException("Note ID and tag ID must be greater than zero");
	    }

	    try {
	    	pstmt = connection.prepareStatement(INSERT_TAG_NOTE_SQL);
	        pstmt.setInt(1, tagId);
	        pstmt.setInt(2, noteId);
	        pstmt.setString(3, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.executeUpdate();

	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error addNoteToNotebook(): " + e.getMessage(), e);
	    } finally {
	        if (pstmt != null) {
	            try {
	                pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error addNoteToNotebook(): " + e.getMessage(), e);
	            }
	        }
	    }
	}

	@Override
	public void getAllTagsFromNote(int noteId, List<Tag> list) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		if (noteId <= 0 || list == null) {
			throw new InvalidParameterException("Invalid note ID or provided list is null");
		}

		try {
			pstmt = connection.prepareStatement(SELECT_ALL_TAGS_NOTE_SQL);
            pstmt.setInt(1, noteId);        	
            rs = pstmt.executeQuery();

		    while (rs.next()) {
		        int id = rs.getInt("tag_id");
		        String title = rs.getString("title");
		        String createdDate = rs.getString("created_date");
		        String modifiedDate = rs.getString("modified_date");
		        
		        list.add(new Tag(id, title, createdDate, modifiedDate));
		    }
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error getAllTagsFromNote(): " + e.getMessage(), e);
		} finally {
	        if (rs != null) {
	            try {
	                rs.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getAllTagsFromNote(): " + e.getMessage(), e);
	            }
	        }
	        if (pstmt != null) {
	            try {
	                pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error getAllTagsFromNote(): " + e.getMessage(), e);
	            }
	        }
		}
	}
	
	@Override
	public void removeTagFromNote(int noteId, int tagId) {
	    PreparedStatement pstmt = null;
		
	    if (tagId <= 0 || noteId <= 0) {
	    	throw new IllegalArgumentException("Note ID and tag ID must be greater than zero");
	    }
		
		try {		    
			pstmt = connection.prepareStatement(DELETE_TAG_NOTE_SQL);
	        pstmt.setInt(1, tagId);
	        pstmt.setInt(1, noteId);
	        pstmt.executeUpdate();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error removeNoteFromNotebook(): " + e.getMessage(), e);
		} finally {
	        if (pstmt != null) {
	            try {
	            	pstmt.close();
	            } catch (SQLException e) {
	            	logger.log(Level.SEVERE, "Error removeNoteFromNotebook(): " + e.getMessage(), e);
	            }
	        }
		}	
	}
	
	public Note fetchNoteWithAllDetails(int id) {
        Note note = getNoteById(id);
        if (note != null) {
        	List<Tag> tags = new ArrayList<>();
        	Notebook notebook = getNotebookOfNote(id);
            getAllTagsFromNote(id, tags);
            
            note.setTags(tags);
            note.setNotebook(notebook);
        }
        return note;
    }
}
