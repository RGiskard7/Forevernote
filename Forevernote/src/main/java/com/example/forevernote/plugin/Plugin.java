package com.example.forevernote.plugin;

/**
 * Interface for Forevernote plugins.
 * 
 * <p>Plugins extend the functionality of Forevernote. They can:</p>
 * <ul>
 *   <li>Register new commands in the Command Palette</li>
 *   <li>Add menu items</li>
 *   <li>Subscribe to application events</li>
 *   <li>Add sidebar panels</li>
 *   <li>Process note content</li>
 * </ul>
 * 
 * <h2>Plugin Lifecycle:</h2>
 * <ol>
 *   <li>{@link #initialize(PluginContext)} - Called when the plugin is loaded</li>
 *   <li>Plugin is now active and can respond to events</li>
 *   <li>{@link #shutdown()} - Called when the plugin is unloaded</li>
 * </ol>
 * 
 * <h2>Example Implementation:</h2>
 * <pre>{@code
 * public class WordCountPlugin implements Plugin {
 *     private PluginContext context;
 *     
 *     @Override
 *     public String getId() { return "word-count"; }
 *     
 *     @Override
 *     public String getName() { return "Word Count"; }
 *     
 *     @Override
 *     public String getVersion() { return "1.0.0"; }
 *     
 *     @Override
 *     public void initialize(PluginContext context) {
 *         this.context = context;
 *         context.registerCommand(new Command("Count Words", () -> countWords()));
 *     }
 *     
 *     @Override
 *     public void shutdown() {
 *         // Cleanup resources
 *     }
 * }
 * }</pre>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.1.0
 */
public interface Plugin {
    
    /**
     * Gets the unique identifier for this plugin.
     * Should be lowercase with hyphens (e.g., "word-count", "daily-notes").
     * 
     * @return The plugin ID
     */
    String getId();
    
    /**
     * Gets the display name of this plugin.
     * 
     * @return The plugin name
     */
    String getName();
    
    /**
     * Gets the version of this plugin (semantic versioning recommended).
     * 
     * @return The plugin version (e.g., "1.0.0")
     */
    String getVersion();
    
    /**
     * Gets a description of what this plugin does.
     * 
     * @return The plugin description
     */
    default String getDescription() {
        return "";
    }
    
    /**
     * Gets the author of this plugin.
     * 
     * @return The plugin author
     */
    default String getAuthor() {
        return "";
    }
    
    /**
     * Checks if this plugin is enabled.
     * 
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Initializes the plugin with the given context.
     * This is called when the plugin is loaded and should register
     * commands, menu items, event handlers, etc.
     * 
     * @param context The plugin context providing access to application services
     */
    void initialize(PluginContext context);
    
    /**
     * Shuts down the plugin.
     * This is called when the plugin is unloaded and should clean up
     * any resources, unsubscribe from events, etc.
     */
    void shutdown();
    
    /**
     * Gets the priority of this plugin (higher = loaded later).
     * Plugins with lower priority are loaded first.
     * 
     * @return The plugin priority (default: 100)
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * Gets the list of plugin IDs this plugin depends on.
     * 
     * @return List of dependency plugin IDs
     */
    default String[] getDependencies() {
        return new String[0];
    }
}
