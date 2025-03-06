package com.example.forevernote.data.dao.abstractLayers;

import java.sql.Connection;

import com.example.forevernote.data.dao.FactoryDAOSQLite;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;

/**
 * Abstract Factory class for creating DAO instances.
 * This class defines the contract for DAO factory implementations.
 */
public abstract class FactoryDAO {
	
    /**
     * Constant representing the SQLite factory type.
     */
	public static final int SQLITE_FACTORY = 1;
	
    /**
     * Retrieves an instance of NoteDAO.
     *
     * @return A NoteDAO instance.
     */
    public abstract NoteDAO getNoteDAO();

    /**
     * Retrieves an instance of FolderDAO.
     *
     * @return A FolderDAO instance.
     */
    public abstract  FolderDAO getFolderDAO();

    /**
     * Retrieves an instance of TagDAO.
     *
     * @return A TagDAO instance.
     */
    public abstract TagDAO getLabelDAO();

    /**
     * Factory method to obtain a concrete implementation of FactoryDAO based on the given key.
     *
     * @param keyFactory   The factory type identifier.
     * @param connection   The database connection to be used.
     * @return A specific implementation of FactoryDAO.
     * @throws IllegalArgumentException if an unsupported factory type is provided.
     */
    public static FactoryDAO getFactory(int keyFactory, Connection connection) {
        switch(keyFactory) {
            case SQLITE_FACTORY:
                return new FactoryDAOSQLite(connection);
            default:
                throw new IllegalArgumentException("Unsupported factory type");
        }
    }
}
