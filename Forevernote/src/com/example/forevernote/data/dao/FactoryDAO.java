package com.example.forevernote.data.dao;

import java.sql.Connection;

public abstract class FactoryDAO {
	public static final int SQLITE_FACTORY = 1;
	
    public abstract INoteDAO getNotaDao();

    public abstract  INotebookDAO getLibretaDao();

    public abstract ITagDAO getEtiquetaDao();

    public static FactoryDAO getFactory(int keyFactory, Connection connection) {
        switch(keyFactory) {
            case SQLITE_FACTORY:
                return new FactoryDAOSQLite(connection);
            default:
                throw new IllegalArgumentException("Unsupported factory type");
        }
    }
}
