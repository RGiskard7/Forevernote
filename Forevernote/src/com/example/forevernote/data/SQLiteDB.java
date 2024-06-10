package com.example.forevernote.data;

import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteDB {
	
	private static final String DATABASE_NAME = "data/database.db";
	private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_NAME;

    private static final String createTableNotes = 
    		"CREATE TABLE IF NOT EXISTS notes("
    				+ "note_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
    				+ "title TEXT NOT NULL, "
    				+ "content TEXT, "
    				+ "notebook INTEGER, "
    				+ "creation_date TEXT NOT NULL"
    		+ ")";

    private static final String createTableNotebooks = 
    		"CREATE TABLE IF NOT EXISTS notebooks("
    				+ "notebook_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
    				+ "title TEXT NOT NULL UNIQUE, "
    				+ "creation_date TEXT"
    		+ ")";

    private static final String createTableLabels = 
    		"CREATE TABLE IF NOT EXISTS labels("
    				+ "label_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
    				+ "title TEXT NOT NULL UNIQUE, "
    				+ "creation_date TEXT"
    		+ ")";

    private static final String notebooksNotes = 
    		"CREATE TABLE IF NOT EXISTS notebooksNotes("
    				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
    				+ "notebook_id INTEGER, " 
    				+ "note_id INTEGER, "
    				+ "FOREIGN  KEY (notebook_id) REFERENCES notebooks(notebook_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE, " 
    				+ "FOREIGN  KEY (note_id) REFERENCES notes(note_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE"
    		+ ")";

    private static final String labelsNotes = 
    		"CREATE TABLE labelsNotes("
    				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
    				+ "label_id INTEGER, " 
    				+ "note_id INTEGER, "
    				+ "FOREIGN  KEY (label_id) REFERENCES labels(label_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE, " 
    				+ "FOREIGN  KEY (note_id) REFERENCES notes(note_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE"
    		+ ")";
    
    private static Connection connection = null;
   
    
    public static Connection openConnection() {
		if (connection == null) {
			try {
				Class.forName("org.sqlite.JDBC");
				connection = DriverManager.getConnection(DATABASE_URL);
				connection.setAutoCommit(false);
			} catch (SQLException e) {
				Logger.getLogger(SQLiteDB.class.getName()).log(Level.SEVERE, "Error opening database connection: " + e.getMessage(), e);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return connection;
    }
    
    public static void closeConnection() {
    	if (connection != null) {
            try {
               connection.close();
               connection = null;
            } catch (SQLException e) {
            	Logger.getLogger(SQLiteDB.class.getName()).log(Level.SEVERE, "Error closing database connection: " + e.getMessage(), e);
            }
    	}
    }
    
    public static void initDatabase() {
    	Statement stmt = null;
    	
        try {
        	Connection connection = openConnection();
        	
        	stmt = connection.createStatement();
        	stmt.executeUpdate(createTableNotes);
        	stmt.executeUpdate(createTableNotebooks);
        	stmt.executeUpdate(createTableLabels);
        	stmt.executeUpdate(notebooksNotes);
        	stmt.executeUpdate(labelsNotes);
        	stmt.close();
        	
        	connection.commit();
        	
        	
        } catch (SQLException e) {
        	Logger.getLogger(SQLiteDB.class.getName()).log(Level.SEVERE, "Error initializing database: " + e.getMessage(), e);
        } finally {
	        if (stmt != null) {
	            try {
	            	stmt.close();
	            } catch (SQLException e) {
	                System.err.println(e.getMessage());
	            }
	        }
        	closeConnection();
        }
    }
}
