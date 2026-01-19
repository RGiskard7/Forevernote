package com.example.forevernote.plugin;

/**
 * Interface for plugin menu registration.
 * Allows plugins to register their own menu items dynamically.
 * 
 * <p>The core application implements this interface and plugins use it
 * to add their commands to the UI menu without the core knowing about
 * specific plugins.</p>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.2.0
 */
public interface PluginMenuRegistry {
    
    /**
     * Registers a menu item for a plugin.
     * 
     * @param pluginId    The plugin's unique ID
     * @param category    The menu category (e.g., "Core", "Productivity", "AI")
     * @param itemName    The display name of the menu item
     * @param action      The action to execute when clicked
     */
    void registerMenuItem(String pluginId, String category, String itemName, Runnable action);
    
    /**
     * Registers a menu item with a keyboard shortcut.
     * 
     * @param pluginId    The plugin's unique ID
     * @param category    The menu category
     * @param itemName    The display name
     * @param shortcut    The keyboard shortcut (e.g., "Ctrl+Shift+W")
     * @param action      The action to execute
     */
    void registerMenuItem(String pluginId, String category, String itemName, String shortcut, Runnable action);
    
    /**
     * Adds a separator in the plugin's menu category.
     * 
     * @param pluginId The plugin's unique ID
     * @param category The menu category
     */
    void addMenuSeparator(String pluginId, String category);
    
    /**
     * Removes all menu items for a plugin.
     * Called when a plugin is disabled or unloaded.
     * 
     * @param pluginId The plugin's unique ID
     */
    void removePluginMenuItems(String pluginId);
    
    /**
     * Checks if a plugin is enabled.
     * Plugins can use this to conditionally show/hide menu items.
     * 
     * @param pluginId The plugin ID to check
     * @return true if the plugin is enabled
     */
    boolean isPluginEnabled(String pluginId);
}
