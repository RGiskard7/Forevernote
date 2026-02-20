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
 * initialization. It includes methods for opening and closing connections, as
 * well as initializing the database schema.
 */
public class SQLiteDB {

    private static final Logger logger = LoggerConfig.getLogger(SQLiteDB.class);
    private String databaseUrl;
    private static SQLiteDB instance = null;

    // SQL statements for creating tables
    // Updated to use TEXT PRIMARY KEY for UUID support
    private static final String createTableNotes = "CREATE TABLE IF NOT EXISTS notes ("
            + "note_id TEXT PRIMARY KEY, "
            + "parent_id TEXT, "
            + "title TEXT NOT NULL, "
            + "content TEXT DEFAULT NULL, "
            + "created_date TEXT NOT NULL, "
            + "modified_date TEXT DEFAULT NULL, "
            + "latitude REAL NOT NULL DEFAULT 0, "
            + "longitude REAL NOT NULL DEFAULT 0, "
            + "author TEXT DEFAULT NULL, "
            + "source_url TEXT DEFAULT NULL, "
            + "is_todo INTEGER NOT NULL DEFAULT 0, "
            + "todo_due TEXT DEFAULT NULL, "
            + "todo_completed TEXT DEFAULT NULL, "
            + "source TEXT DEFAULT NULL, "
            + "source_application TEXT DEFAULT NULL, "
            + "is_favorite INTEGER NOT NULL DEFAULT 0, "
            + "is_pinned INTEGER NOT NULL DEFAULT 0, "
            + "is_deleted INTEGER NOT NULL DEFAULT 0, "
            + "deleted_date TEXT DEFAULT NULL, "
            + "FOREIGN KEY (parent_id) REFERENCES folders(folder_id) "
            + "ON UPDATE CASCADE "
            + "ON DELETE SET NULL"
            + ");";

    private static final String createTableFolders = "CREATE TABLE IF NOT EXISTS folders ("
            + "folder_id TEXT PRIMARY KEY, "
            + "parent_id TEXT, "
            + "title TEXT NOT NULL, "
            + "created_date TEXT NOT NULL, "
            + "modified_date TEXT DEFAULT NULL, "
            + "FOREIGN KEY (parent_id) REFERENCES folders(folder_id) "
            + "ON UPDATE CASCADE "
            + "ON DELETE SET NULL"
            + ");";

    private static final String createTableTags = "CREATE TABLE IF NOT EXISTS tags("
            + "tag_id TEXT PRIMARY KEY, "
            + "title TEXT NOT NULL UNIQUE, "
            + "created_date TEXT NOT NULL, "
            + "modified_date TEXT DEFAULT NULL"
            + ")";

    private static final String createTableTagsNotes = "CREATE TABLE IF NOT EXISTS tagsNotes("
            + "id TEXT PRIMARY KEY, "
            + "tag_id TEXT, "
            + "note_id TEXT, "
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

            // Initialize or migrate tables
            checkAndInitTable(connection, "folders", "folder_id", createTableFolders);
            checkAndInitTable(connection, "notes", "note_id", createTableNotes);
            checkAndInitTable(connection, "tags", "tag_id", createTableTags);
            checkAndInitTable(connection, "tagsNotes", "id", createTableTagsNotes);

            // Migrate existing databases: add necessary columns if they don't exist
            Statement stmt = connection.createStatement();
            try {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(notes)");
                boolean hasIsFavorite = false;
                boolean hasIsPinned = false;
                boolean hasIsDeleted = false;
                boolean hasDeletedDate = false;

                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("is_favorite".equals(columnName))
                        hasIsFavorite = true;
                    if ("is_pinned".equals(columnName))
                        hasIsPinned = true;
                    if ("is_deleted".equals(columnName))
                        hasIsDeleted = true;
                    if ("deleted_date".equals(columnName))
                        hasDeletedDate = true;
                }
                rs.close();

                if (!hasIsFavorite) {
                    logger.info("Adding is_favorite column to notes table...");
                    stmt.executeUpdate(
                            "ALTER TABLE notes ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0 CHECK (is_favorite IN (0, 1))");
                }
                if (!hasIsPinned) {
                    logger.info("Adding is_pinned column to notes table...");
                    stmt.executeUpdate(
                            "ALTER TABLE notes ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0 CHECK (is_pinned IN (0, 1))");
                }
                if (!hasIsDeleted) {
                    logger.info("Adding is_deleted column to notes table...");
                    stmt.executeUpdate(
                            "ALTER TABLE notes ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0 CHECK (is_deleted IN (0, 1))");
                }
                if (!hasDeletedDate) {
                    logger.info("Adding deleted_date column to notes table...");
                    stmt.executeUpdate("ALTER TABLE notes ADD COLUMN deleted_date TEXT DEFAULT NULL");
                }

                // Check 'folders' table
                rs = stmt.executeQuery("PRAGMA table_info(folders)");
                boolean hasFolderIsDeleted = false;
                boolean hasFolderDeletedDate = false;
                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("is_deleted".equals(columnName))
                        hasFolderIsDeleted = true;
                    if ("deleted_date".equals(columnName))
                        hasFolderDeletedDate = true;
                }
                rs.close();

                if (!hasFolderIsDeleted) {
                    logger.info("Adding is_deleted column to folders table...");
                    stmt.executeUpdate("ALTER TABLE folders ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0");
                }
                if (!hasFolderDeletedDate) {
                    logger.info("Adding deleted_date column to folders table...");
                    stmt.executeUpdate("ALTER TABLE folders ADD COLUMN deleted_date TEXT DEFAULT NULL");
                }
            } catch (SQLException e) {
                logger.warning("Could not check/add columns to tables: " + e.getMessage());
            } finally {
                stmt.close();
            }

            connection.commit();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error initializing database: " + e.getMessage(), e);
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.log(Level.SEVERE, "Error rolling back database initialization: " + rollbackEx.getMessage(),
                            rollbackEx);
                }
            }
        } finally {
            if (connection != null) {
                closeConnection(connection);
            }
        }
    }

    /**
     * Checks if a table exists and creates it if not.
     * If it exists, checks if the Primary Key is of type INTEGER.
     * If it is INTEGER, it migrates the table to the new schema (TEXT PK).
     */
    private void checkAndInitTable(Connection connection, String tableName, String pkColumn, String createSql)
            throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
        boolean tableExists = false;
        boolean needsMigration = false;

        while (rs.next()) {
            tableExists = true;
            if (pkColumn.equals(rs.getString("name"))) {
                String type = rs.getString("type");
                if ("INTEGER".equalsIgnoreCase(type)) {
                    needsMigration = true;
                }
                break;
            }
        }
        rs.close();

        if (!tableExists) {
            stmt.executeUpdate(createSql);
            logger.info("Created table: " + tableName);
        } else if (needsMigration) {
            logger.info("Migrating table " + tableName + " to use TEXT IDs...");
            stmt.executeUpdate("ALTER TABLE " + tableName + " RENAME TO " + tableName + "_old");
            stmt.executeUpdate(createSql);

            // Dynamically build INSERT to match columns (Fix for column mismatch during
            // migration)
            java.util.List<String> columns = new java.util.ArrayList<>();
            try (ResultSet rsOld = stmt.executeQuery("PRAGMA table_info(" + tableName + "_old)")) {
                while (rsOld.next()) {
                    columns.add(rsOld.getString("name"));
                }
            }

            if (!columns.isEmpty()) {
                String cols = String.join(", ", columns);
                String insertSql = "INSERT INTO " + tableName + " (" + cols + ") SELECT " + cols + " FROM " + tableName
                        + "_old";
                stmt.executeUpdate(insertSql);
            }

            stmt.executeUpdate("DROP TABLE " + tableName + "_old");
            logger.info("Migration complete for table: " + tableName);
        }
        stmt.close();
    }
}
