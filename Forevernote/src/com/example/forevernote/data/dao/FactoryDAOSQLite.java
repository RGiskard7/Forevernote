package com.example.forevernote.data.dao;

import java.sql.Connection;

public class FactoryDAOSQLite extends FactoryDAO {
	
	private Connection connection;
	
	public FactoryDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	@Override
	public INoteDAO getNoteDAO() {
		return new NoteDAOSQLite(connection);
	}

	@Override
	public INotebookDAO getNotebookDAO() {
		return new NotebookDAOSQLite(connection);
	}

	@Override
	public ITagDAO getEtiquetaDAO() {
		// TODO Auto-generated method stub
		return null;
	}

}
