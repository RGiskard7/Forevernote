package com.example.forevernote.data.dao;

import java.sql.Connection;

import com.example.forevernote.data.dao.abstractLayers.FactoryDAO;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;

/**
 * Concrete implementation of FactoryDAO for the SQLite storage system.
 * This class provides instances of specific DAO implementations for SQLite.
 */
public class FactoryDAOSQLite extends FactoryDAO {
	
    /**
     * Database connection to SQLite.
     */
	private Connection connection;
	
    /**
     * Constructor for FactoryDAOSQLite.
     *
     * @param connection Connection object representing the database connection.
     */
	public FactoryDAOSQLite(Connection connection) {
		this.connection = connection;
	}

    /**
     * Retrieves an instance of NoteDAO specific to SQLite.
     *
     * @return An instance of NoteDAOSQLite.
     */
	@Override
	public NoteDAO getNoteDAO() {
		return new NoteDAOSQLite(connection);
	}

    /**
     * Retrieves an instance of FolderDAO specific to SQLite.
     *
     * @return An instance of FolderDAOSQLite.
     */
	@Override
	public FolderDAO getFolderDAO() {
		return new FolderDAOSQLite(connection);
	}

    /**
     * Retrieves an instance of TagDAO specific to SQLite.
     *
     * @return An instance of TagDAOSQLite.
     */
	@Override
	public TagDAO getLabelDAO() {
		return new TagDAOSQLite(connection);
	}

}
