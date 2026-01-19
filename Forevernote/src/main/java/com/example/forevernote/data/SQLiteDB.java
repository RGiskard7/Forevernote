package com.example.forevernote.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;

/**
 * SQLiteDB is a singleton class that manages the SQLite database connection and
 * initialization. It includes methods for opening and closing connections, as well
 * as initializing the database schema.
 */
public class SQLiteDB {

    private static final Logger logger = LoggerConfig.getLogger(SQLiteDB.class);
    private String databaseUrl;
    private static SQLiteDB instance = null;

    // SQL statements for creating tables
    private static final String createTableNotes = 
		"CREATE TABLE IF NOT EXISTS notes ("
			+ "note_id INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ "parent_id INTEGER, "
			+ "title TEXT NOT NULL, "
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
		+ ");";
    
    private static final String createTableFolders = 
	    "CREATE TABLE IF NOT EXISTS folders ("
		    + "folder_id INTEGER PRIMARY KEY AUTOINCREMENT, "
		    + "parent_id INTEGER, "
		    + "title TEXT NOT NULL, "
		    + "created_date TEXT NOT NULL, "
		    + "modified_date TEXT DEFAULT NULL, "
		    + "FOREIGN KEY (parent_id) REFERENCES folders(folder_id) "
		    + "ON UPDATE CASCADE "
		    + "ON DELETE SET NULL"
	    + ");";
	    /*+ "CREATE INDEX idx_notebooks_title ON notebooks (title);";*/


    private static final String createTableTags = 
        "CREATE TABLE IF NOT EXISTS tags("
	        + "tag_id INTEGER PRIMARY KEY AUTOINCREMENT, "
	        + "title TEXT NOT NULL UNIQUE, "
	        + "created_date TEXT NOT NULL, "
	        + "modified_date TEXT DEFAULT NULL"
	        + ")";

    /*private static final String createTableNotebooksNotes = 
        "CREATE TABLE IF NOT EXISTS notebooksNotes("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "notebook_id INTEGER, "
        + "note_id INTEGER, "
        + "added_date TEXT, "
        + "FOREIGN KEY (notebook_id) REFERENCES notebooks(notebook_id) "
        + "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE, "
        + "FOREIGN KEY (note_id) REFERENCES notes(note_id) "
        + "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE"
        + ")";*/

    private static final String createTableTagsNotes = 
        "CREATE TABLE IF NOT EXISTS tagsNotes("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "tag_id INTEGER, "
        + "note_id INTEGER, "
        + "added_date TEXT NOT NULL, "
        + "FOREIGN KEY (tag_id) REFERENCES tags(tag_id) "
        + "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE, "
        + "FOREIGN KEY (note_id) REFERENCES notes(note_id) "
        + "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE"
        + ")";

    /**
     * Private constructor to prevent instantiation from other classes.
     * 
     * @param databaseUrl The URL of the SQLite database.
     */
    private SQLiteDB(String databaseUrl) {
        this.databaseUrl = "jdbc:sqlite:" + databaseUrl;
    }

    /**
     * Configures the singleton instance of SQLiteDB with the given database URL.
     * This method must be called before getInstance().
     * 
     * @param databaseUrl The URL of the SQLite database.
     */
    public static synchronized void configure(String databaseUrl) {
        if (instance == null) {
            instance = new SQLiteDB(databaseUrl);
        }
    }

    /**
     * Returns the singleton instance of SQLiteDB.
     * 
     * @return The singleton instance.
     * @throws IllegalStateException if the instance has not been configured.
     */
    public static SQLiteDB getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance not yet configured. Call configure() first.");
        }
        return instance;
    }

    /**
     * Opens a new connection to the SQLite database.
     * 
     * @return A new Connection object.
     */
    public Connection openConnection() {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(databaseUrl);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error opening database connection: " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "JDBC Driver not found: " + e.getMessage(), e);
        }
        return connection;
    }

    /**
     * Closes the given database connection.
     * 
     * @param connection The connection to close.
     */
    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Error closing database connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Initializes the database by creating the necessary tables if they do not
     * already exist.
     */
    public void initDatabase() {
        Connection connection = null;
        try {
            connection = openConnection();
            Statement stmt = connection.createStatement();
            
            // Check if folders table exists and has UNIQUE constraint on title
            // If so, recreate it without the constraint
            try {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(folders)");
                boolean tableExists = rs.next();
                if (tableExists) {
                    // Check if there's a UNIQUE constraint (SQLite doesn't expose this easily)
                    // Try to create a folder with duplicate name to test
                    // If it fails, we'll recreate the table
                    try {
                        // Try to get index info
                        ResultSet indexInfo = stmt.executeQuery("PRAGMA index_list(folders)");
                        boolean hasUniqueIndex = false;
                        while (indexInfo.next()) {
                            String indexName = indexInfo.getString("name");
                            if (indexName != null && indexName.contains("title")) {
                                ResultSet indexDetails = stmt.executeQuery("PRAGMA index_info(" + indexName + ")");
                                if (indexDetails.next()) {
                                    hasUniqueIndex = true;
                                    break;
                                }
                            }
                        }
                        
                        // If we suspect there's a UNIQUE constraint, recreate the table
                        if (hasUniqueIndex) {
                            logger.info("Detected UNIQUE constraint on folders.title, recreating table...");
                            // Create backup table
                            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS folders_backup AS SELECT * FROM folders");
                            // Drop old table
                            stmt.executeUpdate("DROP TABLE IF EXISTS folders");
                            // Recreate without UNIQUE
                            stmt.executeUpdate(createTableFolders);
                            // Copy data back
                            stmt.executeUpdate("INSERT INTO folders SELECT * FROM folders_backup");
                            // Drop backup
                            stmt.executeUpdate("DROP TABLE IF EXISTS folders_backup");
                        }
                    } catch (SQLException e) {
                        // If checking fails, try to recreate anyway (safer approach)
                        logger.warning("Could not check for UNIQUE constraint, recreating folders table: " + e.getMessage());
                        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS folders_backup AS SELECT * FROM folders");
                        stmt.executeUpdate("DROP TABLE IF EXISTS folders");
                        stmt.executeUpdate(createTableFolders);
                        stmt.executeUpdate("INSERT INTO folders SELECT * FROM folders_backup");
                        stmt.executeUpdate("DROP TABLE IF EXISTS folders_backup");
                    }
                } else {
                    // Table doesn't exist, create it
                    stmt.executeUpdate(createTableFolders);
                }
            } catch (SQLException e) {
                // Table doesn't exist, create it
                stmt.executeUpdate(createTableFolders);
            }
        	
            stmt.executeUpdate(createTableNotes);
            stmt.executeUpdate(createTableTags);
            stmt.executeUpdate(createTableTagsNotes);
            
            // Migrate existing databases: add is_favorite column if it doesn't exist
            try {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(notes)");
                boolean hasIsFavorite = false;
                while (rs.next()) {
                    if ("is_favorite".equals(rs.getString("name"))) {
                        hasIsFavorite = true;
                        break;
                    }
                }
                if (!hasIsFavorite) {
                    logger.info("Adding is_favorite column to notes table...");
                    stmt.executeUpdate("ALTER TABLE notes ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0 CHECK (is_favorite IN (0, 1))");
                }
            } catch (SQLException e) {
                logger.warning("Could not check/add is_favorite column: " + e.getMessage());
            }
            
            connection.commit();
            stmt.close();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error initializing database: " + e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.log(Level.SEVERE, "Error rolling back database initialization: " + rollbackEx.getMessage(), rollbackEx);
                }
            }
        } finally {
            if (connection != null) {
                closeConnection(connection);
            }
        }
    }
}
