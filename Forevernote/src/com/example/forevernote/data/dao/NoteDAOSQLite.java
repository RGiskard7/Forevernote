package com.example.forevernote.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Notebook;
import com.example.forevernote.data.models.Tag;

public class NoteDAOSQLite implements INoteDAO {
	
	private static final Logger logger = LoggerConfig.getLogger(NoteDAOSQLite.class);
	
	private Connection connection;
	private final SimpleDateFormat dtf = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
	
	public NoteDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	@Override
	public int createNote(String title, String content) {
	    if (title == null || content == null) {
	        return -1; // Indica que el título o el contenido son nulos
	    }

	    String insertSQL = "INSERT INTO notes (title, content, creation_date) VALUES (?, ?, ?)";
	    PreparedStatement pstmt = null;
	    ResultSet generatedKeys = null;
	    int newId = -1; // Valor predeterminado para el ID generado

	    try {
	        pstmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
	        pstmt.setString(1, title);
	        pstmt.setString(2, content);
	        pstmt.setString(3, dtf.format(Calendar.getInstance().getTime()));
	        pstmt.executeUpdate();

	        // Obtener el ID generado
	        generatedKeys = pstmt.getGeneratedKeys();
	        if (generatedKeys.next()) {
	            newId = generatedKeys.getInt(1); // Asigna el ID de la nueva nota
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error createNote(): " + e.getMessage(), e);
	    } finally {
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
	    
	    return newId; // Retorna el ID de la nueva nota o -1 si hubo un error
	}


	@Override
	public Note getNoteById(int id) {
	    if (id <= 0) {
	        return null;
	    }

	    String selectSQL = "SELECT * FROM notes WHERE nota_id = ?";
	    PreparedStatement pstmt = null;
	    ResultSet rs = null;
	    Note note = null;

	    try {
	        pstmt = connection.prepareStatement(selectSQL);
	        pstmt.setInt(1, id);
	        rs = pstmt.executeQuery();

	        if (rs.next()) {
	            int noteId = rs.getInt("note_id");
	            String title = rs.getString("title");
	            String content = rs.getString("content");
	            int notebookId = rs.getInt("notebook");
	            String creationDate = rs.getString("creation_date");

	            List<Tag> tags = new ArrayList<>();
	            getAllTagsFromNote(id, tags);

	            note = new Note(noteId, title, content, getNotebookByNoteId(notebookId), tags, creationDate);
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

	    return note;
	}


	@Override
	public void editNote(int id, String title, String content) {
		if (id <= 0) {
			return;
		}
		
		String updateSQL = "UPDATE notes SET title = ?, content = ? WHERE note_id = ?";
		PreparedStatement pstmt = null;
		
	    try {
	        pstmt = connection.prepareStatement(updateSQL);
	        pstmt.setString(1, title);
	        pstmt.setString(2, content);
	        pstmt.setInt(3, id);
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
	public void deleteNoteById(int id) {
		if (id <= 0) {
			return;
		}
		
	    String deleteSQL = "DELETE FROM notes WHERE note_id = ?";
	    PreparedStatement pstmt = null;
		
		try {		    
	        pstmt = connection.prepareStatement(deleteSQL);
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
		if (list == null) { // Revisar esta comprobación
			return;
		}
		
		String selectSQL = "SELECT * FROM notes";

		Statement stmt = null;
		ResultSet rs = null;

		try {		    
			stmt = connection.createStatement();      	
            rs = stmt.executeQuery(selectSQL);

		    while (rs.next()) {
	            int noteId = rs.getInt("note_id");
	            String title = rs.getString("title");
	            String content = rs.getString("content");
	            int notebookId = rs.getInt("notebook");
	            String creationDate = rs.getString("creation_date");

	            List<Tag> tags = new ArrayList<>();
	            getAllTagsFromNote(noteId, tags);

	            list.add(new Note(noteId, title, content, getNotebookByNoteId(notebookId), tags, creationDate));
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
	public Notebook getNotebookByNoteId(int noteId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeNoteFromNotebook(int noteId, int notebookId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addTagsToNote(int noteId, List<Tag> tags) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getAllTagsFromNote(int noteId, List<Tag> list) {
		if (noteId <= 0 || list == null) {
			return;
		}
		
		String selectSQL = "SELECT DISTINCT tags.tag_id, tags.title, tags.creation_date FROM " +
                "tagsNotes NATURAL JOIN tags WHERE note_id = ?";

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {		    
        	pstmt = connection.prepareStatement(selectSQL);
            pstmt.setInt(1, noteId);        	
            rs = pstmt.executeQuery();

		    while (rs.next()) {
		        int id = rs.getInt("tag_id");
		        String title = rs.getString("title");
		        String creationDate = rs.getString("creation_date");
		        list.add(new Tag(id, title, creationDate));
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
	public void removeTagsFromNote(int noteId, List<Tag> tags) {
		// TODO Auto-generated method stub
		
	}

}
