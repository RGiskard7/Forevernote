import java.sql.Connection;

import com.example.forevernote.data.SQLiteDB;
import com.example.forevernote.data.dao.FactoryDAO;
import com.example.forevernote.data.dao.INoteDAO;

public class Test {
	public static final int SQLITE_FACTORY = 1;

	public static void main(String[] args) {
		Connection connection = SQLiteDB.openConnection();
		
		FactoryDAO dao = FactoryDAO.getFactory(1, connection);
		INoteDAO noteDAO = dao.getNotaDao();
		
		noteDAO.createNote("Nota 1", "Esto es la nota número 1");
		noteDAO.createNote("Nota 2", "Esto es la nota número 2");
		noteDAO.createNote("Nota 3", "Esto es la nota número 3");
		noteDAO.createNote("Nota 4", "Esto es la nota número 4");
		
		SQLiteDB.closeConnection();
	}
}
