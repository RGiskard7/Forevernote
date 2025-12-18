package com.example.forevernote.tests;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.SQLiteDB;
import com.example.forevernote.data.dao.abstractLayers.FactoryDAO;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Folder;

public class Test {
	private static final Logger logger = LoggerConfig.getLogger(Test.class);
	private static final SimpleDateFormat dtf = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
	
	public static void main(String[] args) {
		logger.info("INICIO TEST - " + dtf.format(Calendar.getInstance().getTime()));
		
		SQLiteDB.configure("data/database.db");
		SQLiteDB db = SQLiteDB.getInstance();
		db.initDatabase();
		
		Connection connection = db.openConnection(); // meter dentro de NoteDAO
		
		FactoryDAO dao = FactoryDAO.getFactory(FactoryDAO.SQLITE_FACTORY, connection);
		NoteDAO noteDAO = dao.getNoteDAO();
		FolderDAO folderDAO = dao.getFolderDAO();
		
		// CREACIÓN DE LA LIBRETA DEFAULT
		Folder folder_1 = new Folder("Carpeta 1", null, null);
		folderDAO.createFolder(folder_1);
		
		Folder folder_2 = new Folder("Carpeta 2", null, null);
		folderDAO.createFolder(folder_2);
		
		Folder folder_3 = new Folder("Carpeta 3", null, null);
		folderDAO.createFolder(folder_3);
		
		Folder folder_4 = new Folder("Carpeta 4", null, null);
		folderDAO.createFolder(folder_4);
		
		/*folderDAO.addSubFolderToFolder(folder_1.getId(), folder_2.getId());
		folderDAO.addSubFolderToFolder(folder_1.getId(), folder_3.getId());
		folderDAO.addSubFolderToFolder(folder_1.getId(), folder_4.getId());*/
		
		folderDAO.addSubFolder(folder_1, folder_2);
		folderDAO.addSubFolder(folder_2, folder_3);
		folderDAO.addSubFolder(folder_3, folder_4);
		
		
		//folderDAO.fetchSubFolders(folder_1);
		System.out.println(folder_1);
		
		//Folder rootFolder = Folder.getRoot();
		//folderDAO.fetchSubFolders(rootFolder);
		//System.out.println("Root folder: " + rootFolder);
		System.out.println("Root folder: " + folderDAO.fetchAllFoldersAsTree());
		
		Folder folder_1_2 = folderDAO.getFolderById(1);
		folderDAO.loadSubFolders(folder_1_2);
		System.out.println("Root folder_1_2: " + folder_1_2);
		
		// CREACIÓN DE LA NOTA 1
		Note note_1 = new Note("Nota 1", "Esto es la nota número 1", null, null);
		noteDAO.createNote(note_1);
		
		Note note_2 = new Note("Nota 2", "Esto es la nota número 2", null, null);
		noteDAO.createNote(note_2);
		
		Note note_3 = new Note("Nota 3", "Esto es la nota número 3", null, null);
		noteDAO.createNote(note_3);
		
		Note note_4 = new Note("Nota 4", "Esto es la nota número 4", null, null);
		noteDAO.createNote(note_4);
		
		folderDAO.addNote(folder_1, note_1);
		folderDAO.addNote(folder_2, note_2);
		folderDAO.addNote(folder_3, note_3);
		folderDAO.addNote(folder_4, note_4);
		
		System.out.println(folder_1);
		
		folderDAO.loadNotes(folder_1_2);
		
		System.out.println(folder_1_2);
		
		/*System.out.println("PATH carpeta 4:" + folderDAO.getPathFolder(folder_4.getId()));
		System.out.println("PATH carpeta 3:" + folderDAO.getPathFolder(folder_3.getId()));
		System.out.println("PATH carpeta 2:" + folderDAO.getPathFolder(folder_2.getId()));
		System.out.println("PATH carpeta 1:" + folderDAO.getPathFolder(folder_1.getId()));*/
		
		System.out.println("PATH carpeta 4:" + folder_4.getPath());
		System.out.println("PATH carpeta 3:" + folder_3.getPath());
		System.out.println("PATH carpeta 2:" + folder_2.getPath());
		System.out.println("PATH carpeta 1:" + folder_1.getPath());
		
		System.out.println("PATH nota 4:" + note_4.getPath());
		
		Folder folder_prueba = folderDAO.getFolderById(4);
		System.out.println(folder_prueba.getParent());
		System.out.println(folderDAO.getParentFolder(folder_prueba));
	
		/*folderDAO.addNoteToFolder(folder_1.getId(), note.getId());
		folder_1.add(note);
		note.setParent(note);
		folderDAO.addNote(folder_1, note_1);
		
		System.out.println(folder_1);
		System.out.println(note_1);
		
		System.out.println(noteDAO.getFolderOfNote(note_1.getId()));
		System.out.println(note_1.getParent());
		
		List<Note> notes = new ArrayList<>();
		noteDAO.fetchAllNotes(notes);
		System.out.println(notes);*/
	
		
		db.closeConnection(connection);
	}
}
