package com.example.forevernote.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;

/**
 * Loads external plugins from the plugins/ directory.
 * 
 * <p>This class scans the plugins/ directory for JAR files and dynamically loads
 * plugin classes that implement the Plugin interface. Plugins can be added or
 * removed by simply placing or deleting JAR files in the plugins/ directory.</p>
 * 
 * <p>Plugin JARs must contain:</p>
 * <ul>
 *   <li>A class that implements the Plugin interface</li>
 *   <li>A manifest entry "Plugin-Class" specifying the fully qualified class name</li>
 *   <li>Or the JAR can contain a single class implementing Plugin (auto-detected)</li>
 * </ul>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 4.4.0
 */
public class PluginLoader {
    
    private static final Logger logger = LoggerConfig.getLogger(PluginLoader.class);
    private static final String PLUGINS_DIR = "plugins";
    
    // Keep classloaders open so inner classes remain accessible
    private static final List<URLClassLoader> activeClassLoaders = new ArrayList<>();
    
    /**
     * Scans the plugins directory and loads all available plugins.
     * 
     * @return List of loaded plugin instances
     */
    public static List<Plugin> loadExternalPlugins() {
        List<Plugin> plugins = new ArrayList<>();
        Path pluginsPath = getPluginsDirectory();
        
        if (!Files.exists(pluginsPath)) {
            try {
                Files.createDirectories(pluginsPath);
                logger.info("Created plugins directory: " + pluginsPath);
            } catch (IOException e) {
                logger.warning("Failed to create plugins directory: " + e.getMessage());
                return plugins;
            }
        }
        
        if (!Files.isDirectory(pluginsPath)) {
            logger.warning("Plugins path is not a directory: " + pluginsPath);
            return plugins;
        }
        
        try {
            Files.list(pluginsPath)
                .filter(path -> path.toString().toLowerCase().endsWith(".jar"))
                .forEach(jarPath -> {
                    try {
                        Plugin plugin = loadPluginFromJar(jarPath);
                        if (plugin != null) {
                            plugins.add(plugin);
                            logger.info("Loaded external plugin: " + plugin.getName() + " v" + plugin.getVersion());
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to load plugin from " + jarPath.getFileName() + ": " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            logger.warning("Failed to scan plugins directory: " + e.getMessage());
        }
        
        logger.info("Loaded " + plugins.size() + " external plugin(s)");
        return plugins;
    }
    
    /**
     * Gets the plugins directory path.
     * Uses the application's data directory if available, otherwise uses a relative path.
     * 
     * @return The plugins directory path
     */
    private static Path getPluginsDirectory() {
        // Try to use the application's data directory
        String dataDir = System.getProperty("forevernote.data.dir");
        if (dataDir != null && !dataDir.isEmpty()) {
            return Paths.get(dataDir, PLUGINS_DIR);
        }
        
        // Fallback to relative path from current working directory
        return Paths.get(PLUGINS_DIR);
    }
    
    /**
     * Loads a plugin from a JAR file.
     * 
     * @param jarPath The path to the JAR file
     * @return The plugin instance, or null if loading failed
     */
    private static Plugin loadPluginFromJar(Path jarPath) {
        try {
            URL jarUrl = jarPath.toUri().toURL();
            URLClassLoader classLoader = new URLClassLoader(
                new URL[] { jarUrl },
                PluginLoader.class.getClassLoader()
            );
            
            // Try to read plugin class from manifest
            String pluginClassName = getPluginClassFromManifest(jarPath);
            
            // If not in manifest, try to auto-detect by scanning JAR
            if (pluginClassName == null) {
                pluginClassName = autoDetectPluginClass(jarPath, classLoader);
            }
            
            if (pluginClassName == null) {
                logger.warning("Could not determine plugin class for " + jarPath.getFileName());
                try {
                    classLoader.close();
                } catch (IOException e) {
                    // Ignore
                }
                return null;
            }
            
            // Load and instantiate the plugin class
            Class<?> pluginClass = classLoader.loadClass(pluginClassName);
            
            if (!Plugin.class.isAssignableFrom(pluginClass)) {
                logger.warning("Class " + pluginClassName + " does not implement Plugin interface");
                try {
                    classLoader.close();
                } catch (IOException e) {
                    // Ignore
                }
                return null;
            }
            
            Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
            // Keep classloader open - don't close it, or inner classes won't be accessible at runtime
            // The classloader will be closed when the application shuts down
            activeClassLoaders.add(classLoader);
            return plugin;
            
        } catch (Exception e) {
            logger.warning("Error loading plugin from " + jarPath.getFileName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Reads the plugin class name from the JAR manifest.
     * 
     * @param jarPath The path to the JAR file
     * @return The plugin class name, or null if not found
     */
    private static String getPluginClassFromManifest(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            return jarFile.getManifest()
                .getMainAttributes()
                .getValue("Plugin-Class");
        } catch (Exception e) {
            // Manifest might not exist or not have Plugin-Class entry
            return null;
        }
    }
    
    /**
     * Auto-detects the plugin class by scanning the JAR for classes implementing Plugin.
     * 
     * @param jarPath The path to the JAR file
     * @param classLoader The class loader to use
     * @return The plugin class name, or null if not found
     */
    private static String autoDetectPluginClass(Path jarPath, URLClassLoader classLoader) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                
                // Skip directories and non-class files
                if (name.endsWith("/") || !name.endsWith(".class")) {
                    continue;
                }
                
                // Convert path to class name
                String className = name.replace("/", ".").replace(".class", "");
                
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    if (Plugin.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                        return className;
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Skip classes that can't be loaded (dependencies might be missing)
                    continue;
                }
            }
        } catch (Exception e) {
            logger.warning("Error scanning JAR for plugin class: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Gets the plugins directory path as a File.
     * Useful for UI components that need to show the directory to users.
     * 
     * @return The plugins directory as a File
     */
    public static File getPluginsDirectoryFile() {
        return getPluginsDirectory().toFile();
    }
    
    /**
     * Closes all active classloaders.
     * Should be called when the application shuts down.
     */
    public static void closeAllClassLoaders() {
        for (URLClassLoader classLoader : activeClassLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                logger.warning("Error closing classloader: " + e.getMessage());
            }
        }
        activeClassLoaders.clear();
    }
}
