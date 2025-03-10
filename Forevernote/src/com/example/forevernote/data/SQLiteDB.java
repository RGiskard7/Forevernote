package com.example.forevernote.data;

import java.sql.Connection;
import java.sql.DriverManager;
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
    /*private static final String createTableNotes = 
        "CREATE TABLE IF NOT EXISTS notes("
        + "note_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "parent_id INTEGER, "
        + "title TEXT NOT NULL, "
        + "content TEXT, "
        + "created_date TEXT NOT NULL, "
        + "modified_date TEXT, "
        + "latitude NUMERIC NOT NULL DEFAULT 0, "
        + "longitude NUMERIC NOT NULL DEFAULT 0, "
        + "author TEXT NOT NULL DEFAULT '', "
        + "source_url TEXT NOT NULL DEFAULT '', "
        + "is_todo INT NOT NULL DEFAULT 0, "
        + "todo_due INT NOT NULL DEFAULT 0, "
        + "todo_completed INT NOT NULL DEFAULT 0, "
        + "source TEXT NOT NULL DEFAULT '', "
        + "source_application` TEXT NOT NULL DEFAULT '', "
        + "FOREIGN KEY (parent_id) REFERENCES notebooks(notebook_id) "
        + "ON UPDATE CASCADE "
        + "ON DELETE NULL "
        + ")";*/
    
    /*private static final String createTableNotes = 
	    "CREATE TABLE IF NOT EXISTS notes ("
		    + "note_id INTEGER PRIMARY KEY AUTOINCREMENT, "
		    + "parent_id INTEGER, "
		    + "title TEXT NOT NULL, "
		    + "content TEXT de, "
		    + "created_date TEXT NOT NULL, "
		    + "modified_date TEXT, "
		    + "latitude REAL NOT NULL DEFAULT 0 CHECK (latitude BETWEEN -90 AND 90), "
		    + "longitude REAL NOT NULL DEFAULT 0 CHECK (longitude BETWEEN -180 AND 180), "
		    + "author TEXT NOT NULL DEFAULT '', "
		    + "source_url TEXT NOT NULL DEFAULT '', "
		    + "is_todo INTEGER NOT NULL DEFAULT 0 CHECK (is_todo IN (0, 1)), "
		    + "todo_due INTEGER NOT NULL DEFAULT 0, "
		    + "todo_completed INTEGER NOT NULL DEFAULT 0, "
		    + "source TEXT NOT NULL DEFAULT '', "
		    + "source_application TEXT NOT NULL DEFAULT '', "
		    + "FOREIGN KEY (parent_id) REFERENCES notebooks(notebook_id) "
		    + "ON UPDATE CASCADE "
		    + "ON DELETE SET NULL"
	    + ");"
	    + "CREATE INDEX idx_note_title ON notes (title);"
	    + "CREATE INDEX idx_created_date ON notes (created_date);"
	    + "CREATE INDEX idx_modified_date ON notes (modified_date);"
	    + "CREATE INDEX idx_is_todo ON notes (is_todo);";*/
    
    private static final String createTableNotes = 
		"CREATE TABLE IF NOT EXISTS notes ("
			+ "note_id INTEGER PRIMARY KEY AUTOINCREMENT, "
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
			+ "FOREIGN KEY (parent_id) REFERENCES folders(folder_id) "
			+ "ON UPDATE CASCADE "
			+ "ON DELETE SET NULL"
		+ ");";
		/*+ "CREATE INDEX idx_note_title ON notes (title);"
		+ "CREATE INDEX idx_created_date ON notes (created_date);"
		+ "CREATE INDEX idx_modified_date ON notes (modified_date);"
		+ "CREATE INDEX idx_is_todo ON notes (is_todo);";*/



    /*private static final String createTableNotebooks = 
        "CREATE TABLE IF NOT EXISTS notebooks("
        + "notebook_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "parent_id INTEGER, "
        + "title TEXT NOT NULL UNIQUE, "
        + "created_date TEXT NOT NULL, "
        + "modified_date TEXT, "
        + "FOREIGN KEY (parent_id) REFERENCES notebooks(notebook_id) "
        + "ON UPDATE CASCADE "
        + "ON DELETE NULL "
        + ")";*/
    
    private static final String createTableFolders = 
	    "CREATE TABLE IF NOT EXISTS folders ("
		    + "folder_id INTEGER PRIMARY KEY AUTOINCREMENT, "
		    + "parent_id INTEGER, "
		    + "title TEXT NOT NULL UNIQUE, "
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
        try (Connection connection = openConnection();
             Statement stmt = connection.createStatement()) {
        	
        	stmt.executeUpdate(createTableFolders);
            stmt.executeUpdate(createTableNotes);
            stmt.executeUpdate(createTableTags);
            stmt.executeUpdate(createTableTagsNotes);
            
            connection.commit();
            closeConnection(connection);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error initializing database: " + e.getMessage(), e);
        }
    }
}
