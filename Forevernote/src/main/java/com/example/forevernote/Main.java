package com.example.forevernote;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.SQLiteDB;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main application class for Forevernote.
 */
public class Main extends Application {
    
    // Create directories BEFORE logger loads (logger needs logs/ to exist)
    static {
        if (!AppDataDirectory.ensureDirectoriesExist()) {
            System.err.println("Warning: Could not create data/logs directories");
        }
    }
    
    private static final Logger logger = LoggerConfig.getLogger(Main.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            initializeDatabase();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/com/example/forevernote/ui/view/MainView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            var cssResource = getClass().getResource(
                "/com/example/forevernote/ui/css/modern-theme.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            }
            
            primaryStage.setTitle(AppConfig.getWindowTitle());

            // Load window icon from app.properties
            try {
                String iconPath = "/" + AppConfig.getWindowIconPath();
                var iconStream = getClass().getResourceAsStream(iconPath);
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception e) {
                // Icon is optional
            }

            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();
            
            // macOS focus workaround
            if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
                Platform.runLater(() -> {
                    primaryStage.toFront();
                    primaryStage.requestFocus();
                });
            }
            
            logger.info("Forevernote started. Data: " + AppDataDirectory.getDataDirectory());
            
        } catch (IOException e) {
            logger.severe("Failed to load main view: " + e.getMessage());
            throw new RuntimeException("Failed to start application", e);
        }
    }
    
    private void initializeDatabase() {
        try {
            String dbPath = new File(AppDataDirectory.getDataDirectory(), "database.db")
                .getAbsolutePath();
            
            SQLiteDB.configure(dbPath);
            SQLiteDB.getInstance().initDatabase();
            
            logger.info("Database initialized at: " + dbPath);
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
