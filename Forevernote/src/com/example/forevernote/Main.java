package com.example.forevernote;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.SQLiteDB;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main application class for Forevernote.
 * This class initializes the JavaFX application and sets up the primary stage.
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerConfig.getLogger(Main.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            initializeDatabase();
            
            // Load main view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/forevernote/ui/view/MainView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            
            // Apply CSS styling
            scene.getStylesheets().add(getClass().getResource("/com/example/forevernote/ui/css/modern-theme.css").toExternalForm());
            
            // Configure primary stage
            primaryStage.setTitle("Forevernote - Free Note Taking");
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/com/example/forevernote/ui/images/app-icon.png")));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Show the stage
            primaryStage.show();
            
            logger.info("Forevernote application started successfully");
            
        } catch (IOException e) {
            logger.severe("Failed to load main view: " + e.getMessage());
            throw new RuntimeException("Failed to start application", e);
        }
    }
    
    /**
     * Initializes the SQLite database.
     */
    private void initializeDatabase() {
        try {
            SQLiteDB.configure("data/database.db");
            SQLiteDB db = SQLiteDB.getInstance();
            db.initDatabase();
            logger.info("Database initialized successfully");
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Main method to launch the application.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}