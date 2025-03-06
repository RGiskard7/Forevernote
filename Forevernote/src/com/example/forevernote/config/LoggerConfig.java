package com.example.forevernote.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.LogManager;

/**
 * LoggerConfig is responsible for configuring the logging system for the application.
 * It reads the logging configuration from a properties file and sets up the logging system accordingly.
 */
public class LoggerConfig {

	// Static block to configure the logger
    static {
    	// Crear una carpeta resource e incluirla en el classpath para que getResourceAsStream lo encuentre
    	// Attempt to load the logging configuration from the 'logging.properties' file
        try (InputStream configFile = LoggerConfig.class.getClassLoader().getResourceAsStream("logging.properties")) {
            if (configFile == null) {
            	// Throw an exception if the configuration file is not found
                throw new IOException("Could not find logging.properties file");
            }
        	// Load the logging configuration from the properties file
            
            //System.out.println(inputStreamToString(configFile));
            //LogManager.getLogManager().readConfiguration(configFile);
            System.out.println("Logging configuration loaded successfully.");

        } catch (IOException e) {
        	// Log a severe error if there is an issue loading the configuration file
            Logger.getLogger(LoggerConfig.class.getName()).severe("Could not load logging configuration: " + e.getMessage());
        }
    }

    /**
     * Returns the logger for the given class.
     * 
     * @param clazz the class for which to get the logger
     * @return the logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }
    
    private static String inputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
