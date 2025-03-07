package com.example.forevernote.data.dao;

import java.sql.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.*;
import com.example.forevernote.data.models.*;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.exceptions.*;

/**
 * SQLite implementation of the FolderDAO interface.
 * This class provides methods for interacting with folders in the SQLite database,
 * including creation, retrieval, updating, deletion, and hierarchical management.
 */
public class FolderDAOSQLite implements FolderDAO {
	
	// SQL Queries
	private static final String INSERT_FOLDER_SQL = "INSERT INTO folders (title, created_date) VALUES (?, ?)";
	
	private static final String SELECT_EXIST_TITLE = "SELECT COUNT(*) FROM folders WHERE title = ?";
	
	private static final String SELECT_FOLDER_BY_ID_SQL = "SELECT * FROM folders WHERE folder_id = ?";
	
	private static final String SELECT_FOLDER_BY_NOTE_ID_SQL = "SELECT folder_id, folders.title, folders.created_date, "
			+ "folders.modified_date FROM notes INNER JOIN folders ON notes.parent_id = folders.folder_id WHERE note_id = ?";
	
	private static final String SELECT_ALL_FOLDERS_SQL = "SELECT * FROM folders";
	
	private static final String SELECT_PARENT_FOLDER_SQL = "SELECT parent_id FROM folders WHERE folder_id = ?";
	
	private static final String SELECT_SUBFOLDERS_SQL = "SELECT * FROM folders WHERE parent_id = ?";
	
	private static final String SELECT_SUBFOLDERS_ROOT_SQL = "SELECT * FROM folders WHERE parent_id IS NULL";
	
	private static final String UPDATE_FOLDER_SQL = "UPDATE folders SET title = ?, modified_date = ? WHERE folder_id = ?";
	
	private static final String UPDATE_FOLDER_MODIFIED_DATE_SQL = "UPDATE folders SET modified_date = ? WHERE folder_id = ?";
	
	private static final String UPDATE_FOLDER_ADD_NOTE_SQL = "UPDATE notes SET parent_id = ?, modified_date = ? WHERE note_id = ?";
	
	private static final String UPDATE_FOLDER_REMOVE_NOTE_SQL = "UPDATE notes SET parent_id = NULL, modified_date = ? WHERE note_id = ? AND parent_id = ?";
	
	private static final String UPDATE_FOLDER_ADD_SUBFOLDER_SQL = "UPDATE folders SET parent_id = ?, modified_date = ? WHERE folder_id = ?";
	
	private static final String UPDATE_FOLDER_REMOVE_SUBFOLDER_SQL = "UPDATE folders SET parent_id = NULL, modified_date = ? WHERE folder_id = ? AND parent_id = ?";
	
	private static final String DELETE_FOLDER_SQL = "DELETE FROM folders WHERE folder_id = ?";
	
	private static final Logger logger = LoggerConfig.getLogger(NoteDAOSQLite.class);
	private Connection connection;
	
    /**
     * Constructs a FolderDAOSQLite with the given database connection.
     *
     * @param connection The database connection to be used.
     */
	public FolderDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	// CRUD Methods
	@Override
	public int createFolder(Folder folder) {
	    int newId = -1; // Valor predeterminado para el ID generado
		
	    if (folder == null) {
	    	throw new InvalidParameterException("Folder object cannot be null");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(INSERT_FOLDER_SQL, 
	    		Statement.RETURN_GENERATED_KEYS)) {

	        pstmt.setString(1, folder.getTitle());
	        pstmt.setString(2, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.executeUpdate();

	        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {// Obtener el ID generado
		        if (generatedKeys.next()) {
		            newId = generatedKeys.getInt(1); // Asigna el ID de la nueva carpeta
		            folder.setId(newId);
		        }
	        } 
	        connection.commit();
	        
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error createFolder(): " + e.getMessage(), e);
	    } 

	    return newId; 
	}
	
	@Override
	public Folder getFolderById(int id) {
	    Folder folder = null;
		
	    if (id <= 0) {
	    	throw new IllegalArgumentException("Folder ID must be greater than zero");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(SELECT_FOLDER_BY_ID_SQL)) {
	        pstmt.setInt(1, id);

	        try (ResultSet rs = pstmt.executeQuery()) {
		        if (rs.next()) {
		            folder = mapResultSetToFolder(rs);
		        }
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error getFolderById: " + e.getMessage(), e);
	    } 

	    return folder;
	}
	
	@Override
	public void updateFolder(Folder folder) {
		if (folder == null) {
			throw new InvalidParameterException("Folder object cannot be null");
		}
		
	    try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_FOLDER_SQL)) {
	        pstmt.setString(1, folder.getTitle());
	        pstmt.setString(2, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(3, folder.getId());
	        pstmt.executeUpdate();
	        connection.commit();
	        
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error updateFolder(): " + e.getMessage(), e);
	    }
	}

	@Override
	public void deleteFolder(int id) {
		if (id <= 0) {
			throw new IllegalArgumentException("Folder ID must be greater than zero");
		}
		
		try (PreparedStatement pstmt = connection.prepareStatement(DELETE_FOLDER_SQL)){		    
	        pstmt.setInt(1, id);
	        pstmt.executeUpdate();
	        connection.commit();
	        
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error deleteFolder(): " + e.getMessage(), e);
		}
	}

	// Retrieval Methods
	@Override
	public Folder getFolderByNoteId(int noteId) {
	    Folder folder = null;
		
	    if (noteId <= 0) {
	    	throw new IllegalArgumentException("Folder ID must be greater than zero"); 
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(SELECT_FOLDER_BY_NOTE_ID_SQL)) {
	        pstmt.setInt(1, noteId);
	      
	        try (ResultSet rs = pstmt.executeQuery()) {
		        if (rs.next()) {
		        	folder = mapResultSetToFolder(rs);
		        }
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error getFolderByNoteId(): " + e.getMessage(), e);
	    } 

	    return folder;
	}
	
	@Override
	public List<Folder> fetchAllFoldersAsList() {
		List<Folder> list = new ArrayList<>();
		
		try (Statement stmt = connection.createStatement()) {		    
			try (ResultSet rs = stmt.executeQuery(SELECT_ALL_FOLDERS_SQL)) {
			    while (rs.next()) {
		            list.add(mapResultSetToFolder(rs));    
			    }
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error fetchAllFoldersAsList(): " + e.getMessage(), e);
		} 
		
		return list;
	}
	
	public Folder fetchAllFoldersAsTree() {
		Folder rootFolder = new Folder("ROOT", null, null);
		loadSubFolders(rootFolder);
		return rootFolder;
	}

	// Relationship Management Methods
	@Override
	public void addNote(Folder folder, Note note) {
	    if (folder == null || note == null) {
	    	throw new IllegalArgumentException("Folder object or note object can't be null");
	    }
	    
	    addNote(folder.getId(), note.getId());
	    folder.add(note);
	    note.setParent(folder);
	}
	
	@Override
	public void removeNote(Folder folder, Note note) {
	    if (folder == null || note == null) {
	    	throw new IllegalArgumentException("Note object and folder object can't be null");
	    }
	    
	    removeNote(folder.getId(), note.getId());
	    folder.remove(note);
	    note.setParent(null);
	}
	
	@Override
	public void addSubFolder(Folder parentFolder, Folder subFolder) {
	    if (parentFolder == null || subFolder == null || parentFolder.getId() == subFolder.getId()) {
	    	throw new IllegalArgumentException("Parent folder object or subfolder object can't be null and can't have the same ID");
	    }
	    
	    addSubFolder(parentFolder.getId(), subFolder.getId());
        subFolder.setParent(parentFolder);
        parentFolder.add(subFolder);
	}
	
	@Override
	public void removeSubFolder(Folder parent, Folder subFolder) {
	    if (parent == null || subFolder == null || parent.getId() == subFolder.getId()) {
	    	throw new IllegalArgumentException("Parent folder object or subfolder object can't be null and can't have the same ID");
	    }
	    
	    removeSubFolder(parent.getId(), subFolder.getId());
	    parent.remove(subFolder);
	    subFolder.setParent(null);
	}
	
	@Override
	public void loadSubFolders(Folder folder, int maxDepth) {
		if (folder == null) {
			throw new InvalidParameterException("Parent folder object is null");
		}
		
		if (maxDepth < 0) {
			throw new IllegalArgumentException("Maximum depth can't be negative");
		}
		
		loadSubFoldersHelper(folder, 0, maxDepth);
	}
	
	@Override
	public void loadSubFolders(Folder folder) {
		loadSubFolders(folder, Integer.MAX_VALUE);
	}
	
	@Override
	public Folder getParentFolder(int folderId) {
	    Folder parentFolder = null;
		
		if (folderId <= 0) {
			throw new InvalidParameterException("Invalid folder ID");
		}

	    try (PreparedStatement pstmt = connection.prepareStatement(SELECT_PARENT_FOLDER_SQL)) {
	        pstmt.setInt(1, folderId);
	        try (ResultSet rs = pstmt.executeQuery()) {
		        if (rs.next()) {
		            Integer parentId = rs.getInt("parent_id");
		            if (!rs.wasNull()) { // Por si está en la raiz y su padre es null
		            	parentFolder = getFolderById(parentId);
		            }
		        }
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error getParentFolder: " + e.getMessage(), e);
	    }

	    return parentFolder;
	}
	
	@Override
	public Folder getParentFolder(Folder folder) {
		if (folder == null ) {
			throw new InvalidParameterException("Folder object is null or don't have parent folder");
		}
		
		return getParentFolder(folder.getId());
	}
	
	@Override
	public String getPathFolder(int idFolder) {
	    if (idFolder <= 0) {
	        throw new InvalidParameterException("Invalid folder ID");
	    }
		
	    Folder folder = getFolderById(idFolder);
	    if (folder == null) {
	        throw new InvalidParameterException("Folder not found");
	    }

	    Folder parentFolder = getParentFolder(idFolder);
	    if (parentFolder == null) {
	        return "/" + folder.getTitle();
	    } else {
	        return getPathFolder(parentFolder.getId()) + "/" + folder.getTitle();
	    }
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
	protected void addNote(int folderId, int noteId) {
	    if (folderId <= 0 || noteId <= 0) {
	    	throw new IllegalArgumentException("Folder ID and note ID must be greater than zero");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_FOLDER_ADD_NOTE_SQL)) { 
	        pstmt.setInt(1, folderId);
	        pstmt.setString(2, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(3, noteId);
	        pstmt.executeUpdate();
	        connection.commit();
	        
	        updateModifiedDateFolder(folderId);
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error addNote(): " + e.getMessage(), e);
	    }
	}
	
	@Override
	public void loadNotes(Folder folder) {
		NoteDAOSQLite noteDAO = null;
		
		if (folder == null) {
			throw new InvalidParameterException("Parent folder object is null");
		}
		
		if (!folder.isEmpty()) {
			for (Component subFolder : folder.getChildren()) {
				if (subFolder instanceof Folder) {
					loadNotes((Folder)subFolder);
				}
			}
		}
		
		noteDAO = new NoteDAOSQLite(connection);
		noteDAO.fetchNotesByFolderId(folder);
	}

	protected void removeNote(int folderId, int noteId) {
	    if (folderId <= 0 || noteId <= 0) {
	    	throw new IllegalArgumentException("Note ID and folder ID must be greater than zero");
	    }
		
		try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_FOLDER_REMOVE_NOTE_SQL)) { 	    
	        pstmt.setString(1, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(1, noteId);
	        pstmt.setInt(2, folderId);
	        pstmt.executeUpdate();
	        connection.commit();
	        
	        updateModifiedDateFolder(folderId);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Error removeNote(): " + e.getMessage(), e);
		}	
	}
	
	protected void addSubFolder(int parentId, int subFolderId) {
	    if (parentId <= 0 || subFolderId <= 0 || parentId == subFolderId) {
	    	throw new IllegalArgumentException("Parent folder ID and subfolder ID must be greater than zero and can't be the same");
	    }

	    try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_FOLDER_ADD_SUBFOLDER_SQL)) {
	        pstmt.setInt(1, parentId);
	        pstmt.setString(2, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(3, subFolderId);
	        pstmt.executeUpdate();
	        connection.commit();
	        
	        updateModifiedDateFolder(parentId);
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error addSubFolder(): " + e.getMessage(), e);
	    }
	}
	
	protected void removeSubFolder(int parentId, int subFolderId) {
		if (parentId <= 0 || subFolderId <= 0 || parentId == subFolderId) {
			throw new IllegalArgumentException("Parent folder ID and subfolder ID must be greater than zero and can't be the same");
		}
		
		try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_FOLDER_REMOVE_SUBFOLDER_SQL)) { 	    
	        pstmt.setInt(1, subFolderId);
	        pstmt.setInt(2, parentId);
	        pstmt.executeUpdate();
	        connection.commit();
	        
	        updateModifiedDateFolder(parentId);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "removeSubFolder(): " + e.getMessage(), e);
		}
	}
	
	private void loadSubFoldersHelper(Folder folder, int currentDepth, int maxDepth) {
	    if (currentDepth > maxDepth) return;
	
	    String query = folder.getId() != null ? SELECT_SUBFOLDERS_SQL : SELECT_SUBFOLDERS_ROOT_SQL;
	    
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        if (folder.getId() != null) {
	            pstmt.setInt(1, folder.getId());
	        }
	        
	        try (ResultSet rs = pstmt.executeQuery()) {
	            while (rs.next()) {
	                Folder subFolder = mapResultSetToFolder(rs);
	                folder.add(subFolder);
	                subFolder.setParent(folder);
	                loadSubFoldersHelper(subFolder, currentDepth + 1, maxDepth);
	            }
	        }
	    } catch (SQLException e) {
	        logger.log(Level.SEVERE, "Error loadSubFoldersHelper(): " + e.getMessage(), e);
	        throw new DataAccessException("Failed to retrieve subfolders", e);
	    }
	}
		
	private void loadParentFoldersHelper(Folder folder, int currentDepth, int maxDepth) {
		if (currentDepth > maxDepth) return;
		
	    try (PreparedStatement pstmt = connection.prepareStatement(SELECT_PARENT_FOLDER_SQL)) {
	        pstmt.setInt(1, folder.getId());
	        try (ResultSet rs = pstmt.executeQuery()) {
		        if (rs.next()) {
		            Integer parentId = rs.getInt("parent_id");
		            if (!rs.wasNull()) { // Por si está en la raiz y su padre es null
		            	Folder parentFolder = getFolderById(parentId);
		            	folder.setParent(parentFolder);
		            	loadParentFoldersHelper(parentFolder, currentDepth + 1, maxDepth);
		            }
		        }
	        }
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error loadParentFoldersHelper: " + e.getMessage(), e);
	    }
	}
	
	public void loadParentFolders(Folder folder, int maxDepth) {
		if (folder == null) {
			throw new InvalidParameterException("Parent folder object is null");
		}
		
		if (maxDepth < 0) {
			throw new IllegalArgumentException("Maximum depth can't be negative");
		}
		
		loadParentFoldersHelper(folder, 0, maxDepth);
	}
	
	public void loadParentFolders(Folder folder) {
		loadParentFolders(folder, Integer.MAX_VALUE);
	}
	
	public void loadParentFolder(Folder folder) {
		loadParentFolders(folder, 1);
	}
			
	protected Folder mapResultSetToFolder(ResultSet rs) throws SQLException {
		Folder folder = null;
		
		if (rs != null) {
            int folderId = rs.getInt("folder_id");
            String title = rs.getString("title");
            String cratedDate = rs.getString("created_date");
            String modifiedDate = rs.getString("modified_date");
            //Integer parentId = (rs.getObject("parent_id") != null) ? rs.getInt("parent_id") : null;

            folder = new Folder(folderId, title, cratedDate, modifiedDate);
		}
		
		return folder;
	}
	
	private void updateModifiedDateFolder(int idFolder) {		
		if (idFolder <= 0) {
			throw new InvalidParameterException("Invalid folder ID, it must be greater than zero");
		}
		
	    try (PreparedStatement pstmt = connection.prepareStatement(UPDATE_FOLDER_MODIFIED_DATE_SQL)) {
	        pstmt.setString(1, DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
	        pstmt.setInt(2, idFolder);
	        pstmt.executeUpdate();
	        connection.commit();
	        
	    } catch (SQLException e) {
	    	logger.log(Level.SEVERE, "Error updateModifiedDateFolder(): " + e.getMessage(), e);
	    } 
	}
}
