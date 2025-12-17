package com.example.forevernote;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.SQLiteDB;
import com.example.forevernote.ui.controller.MainController;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main application class for Forevernote.
 * This class initializes the JavaFX application and sets up the primary stage.
 */
public class Main extends Application {
    
    // Static block to ensure directories exist before logger initialization
    static {
        // Create data and logs directories before LoggerConfig initializes FileHandler
        // This must happen before any logger is created
        try {
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            java.io.File logsDir = new java.io.File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
        } catch (Exception e) {
            // If directory creation fails, log to console since logger may not be ready
            System.err.println("Warning: Could not create data/logs directories: " + e.getMessage());
        }
    }
    
    private static final Logger logger = LoggerConfig.getLogger(Main.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            initializeDatabase();
            
            // Load main view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/forevernote/ui/view/MainView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            // Get controller and configure keyboard shortcuts
            MainController controller = loader.getController();
            // Keyboard shortcuts can be configured here if needed

            // Apply CSS styling
            var cssResource = getClass().getResource("/com/example/forevernote/ui/css/modern-theme.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                logger.warning("Could not load CSS stylesheet: modern-theme.css not found");
            }
            
            // Configure primary stage
            primaryStage.setTitle("Forevernote - Free Note Taking");

            // Try to load app icon (optional)
            try {
                var iconStream = getClass().getResourceAsStream("/com/example/forevernote/ui/images/app-icon.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception e) {
                logger.warning("Could not load app icon: " + e.getMessage());
            }

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
     * Uses data/database.db relative to the working directory.
     * Scripts ensure execution from Forevernote/ directory.
     * Note: data/ and logs/ directories are already created in the static block.
     */
    private void initializeDatabase() {
        try {
            // Directories are already created in static block, but ensure they exist as a safety check
            java.io.File dataDir = new java.io.File("data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                logger.warning("Data directory was missing and recreated");
            }
            
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