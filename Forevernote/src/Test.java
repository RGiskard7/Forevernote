import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.SQLiteDB;
import com.example.forevernote.data.dao.FactoryDAO;
import com.example.forevernote.data.dao.INoteDAO;
import com.example.forevernote.data.dao.INotebookDAO;
import com.example.forevernote.data.models.Note;

public class Test {
	private static final Logger logger = LoggerConfig.getLogger(Test.class);
	private static final SimpleDateFormat dtf = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
	
	public static void main(String[] args) {
		logger.info("INICIO TEST - " + dtf.format(Calendar.getInstance().getTime()));
		
		SQLiteDB.configure("data/database.db");
		SQLiteDB db = SQLiteDB.getInstance();
		db.initDatabase();
		
		Connection connection = db.openConnection();
		
		FactoryDAO dao = FactoryDAO.getFactory(FactoryDAO.SQLITE_FACTORY, connection);
		INoteDAO noteDAO = dao.getNoteDAO();
		INotebookDAO notebookDAO = dao.getNotebookDAO();
		
		int notebookID = notebookDAO.createNotebook("Libreta Default");
		
		int noteID = noteDAO.createNote("Nota 1", "Esto es la nota número 1");
		notebookDAO.addNoteToNotebook(notebookID, noteID);
		System.out.println(noteDAO.getNotebookOfNote(noteID));
		
		
		List<Note> notes = new ArrayList<>();
		notebookDAO.getAllNotesFromNotebook(notebookID, notes);
		System.out.println(notes);
		
		Note note = new Note(0, "nota", "nota", null, null);
		
		noteID = noteDAO.createNote("Nota 2", "Esto es la nota número 2");
		//notebookDAO.addNoteToNotebook(notebookID, noteID);
		
		noteID = noteDAO.createNote("Nota 3", "Esto es la nota número 3");
		//notebookDAO.addNoteToNotebook(notebookID, noteID);
		
		noteID = noteDAO.createNote("Nota 4", "Esto es la nota número 4");
		//notebookDAO.addNoteToNotebook(notebookID, noteID);
		
		noteDAO.getNotebookOfNote(noteID);
		
		/*List<Note> notas = new ArrayList<>();
		noteDAO.getAllNotes(notas);
		
		System.out.println(notas);
		
		notas = new ArrayList<>();
		noteDAO.editNote(1, "Nota editada", "Esto es la 'Nota 1' editada");
		
		noteDAO.getAllNotes(notas);
		
		System.out.println(notas);*/
		
		db.closeConnection(connection);
	}
}
