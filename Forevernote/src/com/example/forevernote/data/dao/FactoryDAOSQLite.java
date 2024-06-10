package com.example.forevernote.data.dao;

import java.sql.Connection;

public class FactoryDAOSQLite extends FactoryDAO {
	
	private Connection connection;
	
	public FactoryDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	@Override
	public INoteDAO getNotaDao() {
		return new NoteDAOSQLite(connection);
	}

	@Override
	public INotebookDAO getLibretaDao() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ITagDAO getEtiquetaDao() {
		// TODO Auto-generated method stub
		return null;
	}

}
