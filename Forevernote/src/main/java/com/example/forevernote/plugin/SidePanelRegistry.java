package com.example.forevernote.plugin;

import javafx.scene.Node;

/**
 * Interface for plugin UI registration (Obsidian-style).
 * Allows plugins to add custom UI components to the application.
 * 
 * <p>Plugins can register:</p>
 * <ul>
 *   <li>Side panels in the right sidebar</li>
 *   <li>Status bar items in the bottom bar</li>
 * </ul>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.3.0
 */
public interface SidePanelRegistry {
    
    // ==================== Side Panels (Right Sidebar) ====================
    
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
    
    // ==================== Status Bar Items (Bottom Bar) ====================
    
    /**
     * Registers a status bar item for a plugin.
     * The item appears in the bottom status bar.
     * 
     * @param pluginId The plugin's unique ID
     * @param itemId   A unique identifier for this item
     * @param content  The JavaFX Node to display (typically a Label)
     */
    void registerStatusBarItem(String pluginId, String itemId, Node content);
    
    /**
     * Removes a status bar item.
     * 
     * @param pluginId The plugin's unique ID
     * @param itemId   The item's unique ID
     */
    void removeStatusBarItem(String pluginId, String itemId);
    
    /**
     * Updates the content of a status bar item.
     * 
     * @param pluginId The plugin's unique ID
     * @param itemId   The item's unique ID  
     * @param content  The new content
     */
    void updateStatusBarItem(String pluginId, String itemId, Node content);
    
    /**
     * Removes all status bar items for a plugin.
     * Called when a plugin is disabled or unloaded.
     * 
     * @param pluginId The plugin's unique ID
     */
    void removeAllStatusBarItems(String pluginId);
}
