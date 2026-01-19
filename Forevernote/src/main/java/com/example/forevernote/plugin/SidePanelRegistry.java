package com.example.forevernote.plugin;

import javafx.scene.Node;

/**
 * Interface for plugin side panel registration.
 * Allows plugins to add custom UI panels to the application sidebar (Obsidian-style).
 * 
 * <p>Plugins can register custom panels that appear in the right sidebar,
 * similar to how Obsidian allows plugins to add UI components.</p>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.3.0
 */
public interface SidePanelRegistry {
    
    /**
     * Registers a side panel for a plugin.
     * The panel will appear as a collapsible section in the right sidebar.
     * 
     * @param pluginId  The plugin's unique ID
     * @param panelId   A unique identifier for this panel within the plugin
     * @param title     The display title for the panel header
     * @param content   The JavaFX Node to display as panel content
     * @param icon      An optional icon/emoji for the panel header (can be null)
     */
    void registerSidePanel(String pluginId, String panelId, String title, Node content, String icon);
    
    /**
     * Removes a side panel.
     * 
     * @param pluginId The plugin's unique ID
     * @param panelId  The panel's unique ID
     */
    void removeSidePanel(String pluginId, String panelId);
    
    /**
     * Removes all side panels for a plugin.
     * Called when a plugin is disabled or unloaded.
     * 
     * @param pluginId The plugin's unique ID
     */
    void removeAllSidePanels(String pluginId);
    
    /**
     * Shows or hides the plugin panels section.
     * 
     * @param visible true to show, false to hide
     */
    void setPluginPanelsVisible(boolean visible);
    
    /**
     * Checks if the plugin panels section is visible.
     * 
     * @return true if visible
     */
    boolean isPluginPanelsVisible();
}
