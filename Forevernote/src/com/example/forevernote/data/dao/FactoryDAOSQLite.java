package com.example.forevernote.data.dao;

import java.sql.Connection;

import com.example.forevernote.data.dao.abstractLayers.FactoryDAO;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;

public class FactoryDAOSQLite extends FactoryDAO {
	
	private Connection connection;
	
	public FactoryDAOSQLite(Connection connection) {
		this.connection = connection;
	}

	@Override
	public NoteDAO getNoteDAO() {
		return new NoteDAOSQLite(connection);
	}

	@Override
	public FolderDAO getFolderDAO() {
		return new FolderDAOSQLite(connection);
	}

	@Override
	public TagDAO getLabelDAO() {
		return new TagDAOSQLite(connection);
	}

}
