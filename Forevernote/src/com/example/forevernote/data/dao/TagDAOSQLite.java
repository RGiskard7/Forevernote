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
import com.example.forevernote.data.dao.interfaces.TagDAO;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.exceptions.InvalidParameterException;

/**
 * SQLite implementation of the TagDAO interface.
 * This class provides methods for interacting with tags in the SQLite database,
 * including creation, retrieval, updating, deletion, and managing notes associated with tags.
 */
public class TagDAOSQLite implements TagDAO {
	
	// SQL Queries
	private static final String INSERT_TAG_SQL = "INSERT INTO tags (title, created_date) VALUES (?, ?)";
	
	private static final String SELECT_EXIST_TITLE = "SELECT COUNT(*) FROM tags WHERE title = ?";
	
	private static final String SELECT_TAG_BY_ID_SQL = "SELECT * FROM tags WHERE tag_id = ?";
	
	private static final String SELECT_ALL_TAGS_SQL = "SELECT * FROM tags";
	
	private static final String SELECT_ALL_NOTES_TAG_SQL = "SELECT DISTINCT * FROM tagsNotes NATURAL JOIN notes WHERE tag_id = ?";
	
	private static final String UPDATE_TAG_SQL = "UPDATE tags SET title = ?, modified_date = ? WHERE tag_id = ?";
	
	private static final String DELETE_TAG_SQL = "DELETE FROM tags WHERE tag_id = ?";
	
	private static final Logger logger = LoggerConfig.getLogger(NoteDAOSQLite.class);
	private Connection connection;
	
    /**
     * Constructs a TagDAOSQLite with the given database connection.
     *
     * @param connection The database connection to be used.
     */
	public TagDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	// CRUD Methods
	@Override
	public int createTag(Tag tag) {
	    int newId = -1;
		
	    if (tag == null) {
	    	throw new InvalidParameterException("Tag object cannot be null");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(INSERT_TAG_SQL, 
	    		Statement.RETURN_GENERATED_KEYS)) {

	        pstmt.setString(1, tag.getTitle());
	        pstmt.setString(2, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.executeUpdate();

	        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
		        if (generatedKeys.next()) {
		            newId = generatedKeys.getInt(1);
		            tag.setId(newId);
		        }
	        } 
	        connection.commit();
	        
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error createTag(): " + e.getMessage(), e);
	    } 

	    return newId; 
	}
	
	@Override
	public void updateTag(Tag tag) {
		if (tag == null) {
			throw new InvalidParameterException("Tag object cannot be null");
		}
		
	    try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_TAG_SQL)) {
	        pstmt.setString(1, tag.getTitle());
	        pstmt.setString(2, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(3, tag.getId());
	        pstmt.executeUpdate();
	        connection.commit();
	        
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error updateTag(): " + e.getMessage(), e);
	    }
	}

	@Override
	public void deleteTag(int id) {
		if (id <= 0) {
			throw new IllegalArgumentException("Tag ID must be greater than zero");
		}
		
		try (PreparedStatement pstmt = connection.prepareStatement(DELETE_TAG_SQL)){		    
	        pstmt.setInt(1, id);
	        pstmt.executeUpdate();
	        connection.commit();
	        
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error deleteTag(): " + e.getMessage(), e);
		}
	}

	@Override
	public Tag getTagById(int id) {
	    Tag tag = null;
		
	    if (id <= 0) {
	    	throw new IllegalArgumentException("Tag ID must be greater than zero");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(SELECT_TAG_BY_ID_SQL)) {
	        pstmt.setInt(1, id);

	        try (ResultSet rs = pstmt.executeQuery()) {
		        if (rs.next()) {
		            tag = mapResultSetToTag(rs);
		        }
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error getTagById: " + e.getMessage(), e);
	    } 

	    return tag;
	}

	// Retrieval Methods
	@Override
	public List<Tag> fetchAllTags() {
		List<Tag> list = new ArrayList<>();
		
		try (Statement stmt = connection.createStatement()) {		    
			try (ResultSet rs = stmt.executeQuery(SELECT_ALL_TAGS_SQL)) {
			    while (rs.next()) {
		            list.add(mapResultSetToTag(rs));    
			    }
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error fetchAllTags(): " + e.getMessage(), e);
		} 
		
		return list;
	}

	@Override
	public List<Note> fetchAllNotesWithTag(int tagId) {		
		if (tagId <= 0) {
			throw new InvalidParameterException("Invalid tag ID");
		}
		
		List<Note> list = new ArrayList<>();

		try (PreparedStatement pstmt = connection.prepareStatement(SELECT_ALL_NOTES_TAG_SQL)) {
            pstmt.setInt(1, tagId);        	

            try (ResultSet rs = pstmt.executeQuery()) {
            	NoteDAOSQLite noteDAO = new NoteDAOSQLite(connection);
            	
    		    while (rs.next()) {
    		        list.add(noteDAO.mapResultSetToNote(rs));
    		    }	
            }
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error fetchAllNotesWithTag(): " + e.getMessage(), e);
		}
		
		return list;
	}

	@Override
	public boolean existsByTitle(String title) {
	    if (title == null) {
	    	throw new IllegalArgumentException("Title can't be null");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(SELECT_EXIST_TITLE)) {
	        pstmt.setString(1, title);
	        
	        try (ResultSet rs = pstmt.executeQuery()) {
		        if (rs.next()) {
		        	int countTitle = rs.getInt("count(*)");
		        	if (countTitle > 0) return true;
		        }
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error existsByTitle: " + e.getMessage(), e);
	    } 

	    return false;
	}
	
	// Helper Methods (protected/private)
	protected Tag mapResultSetToTag(ResultSet rs) throws SQLException {
		Tag tag = null;
		
		if (rs != null) {
            int tagId = rs.getInt("folder_id");
            String title = rs.getString("title");
            String cratedDate = rs.getString("created_date");
            String modifiedDate = rs.getString("modified_date");

            tag = new Tag(tagId, title, cratedDate, modifiedDate);
		}
		
		return tag;
	}
}
