package com.example.forevernote.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class SQLiteDBOLD {
	private static final String DATABASE_NAME = "data/dabase.sqlite";
	private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_NAME;
	
    private static SimpleDateFormat dtf = new SimpleDateFormat("dd/MM/yyyy");

    private static final String crearTablaNotas = 
    		"CREATE TABLE IF NOT EXISTS notas("
    				+ "nota_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
    				+ "titulo TEXT NOT NULL, "
    				+ "texto TEXT, "
    				+ "libreta INTEGER, "
    				+ "fecha_creacion TEXT"
    		+ ")";

    private static final String crearTablaLibretas = 
    		"CREATE TABLE IF NOT EXISTS libretas("
    				+ "libreta_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
    				+ "titulo TEXT NOT NULL UNIQUE, "
    				+ "fecha_creacion TEXT"
    		+ ")";

    private static final String crearTablaEtiquetas = 
    		"CREATE TABLE IF NOT EXISTS etiquetas("
    				+ "etiqueta_id INTEGER PRIMARY KEY AUTOINCREMENT, " 
    				+ "titulo TEXT NOT NULL UNIQUE, "
    				+ "fecha_creacion TEXT"
    		+ ")";

    private static final String libretaNotas = 
    		"CREATE TABLE IF NOT EXISTS libretaNotas("
    				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
    				+ "libreta_id INTEGER, " 
    				+ "nota_id INTEGER, "
    				+ "FOREIGN  KEY (libreta_id) REFERENCES libretas(libreta_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE, " 
    				+ "FOREIGN  KEY (nota_id) REFERENCES notas(nota_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE"
    		+ ")";

    private static final String etiquetaNotas = 
    		"CREATE TABLE etiquetaNotas("
    				+ "id INTEGER PRIMARY KEY AUTOINCREMENT, "
    				+ "etiqueta_id INTEGER, " 
    				+ "nota_id INTEGER, FOREIGN  KEY (etiqueta_id) REFERENCES etiquetas(etiqueta_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE, " 
    				+ "FOREIGN  KEY (nota_id) REFERENCES notas(nota_id) "
    					+ "MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE"
    		+ ")";
      

    /* private static final String checkBoxNotas = "CREATE TABLE checkBoxNotas(id INTEGER PRIMARY KEY AUTOINCREMENT, texto TEXT NOT NULL, " +
            "nota_id INTEGER NOT NULL, control INTEGER, CHECK (control in (0,1)), FOREIGN  KEY (nota_id) REFERENCES notas(nota_id) MATCH SIMPLE " +
            "ON UPDATE CASCADE ON DELETE CASCADE)"; */
    
    public SQLiteDBOLD() {
        // Constructor vacío
    }
    
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    public void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Error al cerrar la conexión a la base de datos: " + e.getMessage());
        }
    }

    public void initDatabase() {
        try (Connection connection = openConnection()) {
            // Crear tablas si no existen
            // createNotasTable(connection);
            // Agregar más tablas aquí si es necesario
        } catch (SQLException e) {
            System.out.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    /*private void createNotasTable(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS notas (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "titulo TEXT, " +
                                "contenido TEXT)";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            System.out.println("Tabla 'notas' creada exitosamente.");
        }
    }*/

}
