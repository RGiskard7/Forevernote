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
    private static final String createTableNotes = 
        "CREATE TABLE IF NOT EXISTS notes("
        + "note_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "title TEXT NOT NULL, "
        + "content TEXT, "
        + "notebook INTEGER, "
        + "creation_date TEXT NOT NULL, "
        + "update_date TEXT"
        + ")";

    private static final String createTableNotebooks = 
        "CREATE TABLE IF NOT EXISTS notebooks("
        + "notebook_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "title TEXT NOT NULL UNIQUE, "
        + "creation_date TEXT, "
        + "update_date TEXT"
        + ")";

    private static final String createTableLabels = 
        "CREATE TABLE IF NOT EXISTS tags("
        + "tag_id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "title TEXT NOT NULL UNIQUE, "
        + "creation_date TEXT, "
        + "update_date TEXT"
        + ")";

    private static final String notebooksNotes = 
        "CREATE TABLE IF NOT EXISTS notebooksNotes("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "notebook_id INTEGER, "
        + "note_id INTEGER, "
        + "FOREIGN KEY (notebook_id) REFERENCES notebooks(notebook_id) "
        + "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE, "
        + "FOREIGN KEY (note_id) REFERENCES notes(note_id) "
        + "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE"
        + ")";

    private static final String tagsNotes = 
        "CREATE TABLE IF NOT EXISTS tagsNotes("
        + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "tag_id INTEGER, "
        + "note_id INTEGER, "
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
        	
            stmt.executeUpdate(createTableNotes);
            stmt.executeUpdate(createTableNotebooks);
            stmt.executeUpdate(createTableLabels);
            stmt.executeUpdate(notebooksNotes);
            stmt.executeUpdate(tagsNotes);
            
            connection.commit();
            closeConnection(connection);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error initializing database: " + e.getMessage(), e);
        }
    }
}
