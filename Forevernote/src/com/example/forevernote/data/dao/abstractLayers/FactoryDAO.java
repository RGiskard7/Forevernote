package com.example.forevernote.data.dao.abstractLayers;

import java.sql.Connection;

import com.example.forevernote.data.dao.FactoryDAOSQLite;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;

public abstract class FactoryDAO {
	public static final int SQLITE_FACTORY = 1;
	
    public abstract NoteDAO getNoteDAO();

    public abstract  FolderDAO getFolderDAO();

    public abstract TagDAO getLabelDAO();

    public static FactoryDAO getFactory(int keyFactory, Connection connection) {
        switch(keyFactory) {
            case SQLITE_FACTORY:
                return new FactoryDAOSQLite(connection);
            default:
                throw new IllegalArgumentException("Unsupported factory type");
        }
    }
}
