package com.example.forevernote.plugin;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.event.AppEvent;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;
import com.example.forevernote.ui.components.CommandPalette;

import javafx.scene.Node;
import java.util.function.Consumer;

/**
 * Context provided to plugins for accessing application services.
 * 
 * <p>The PluginContext provides plugins with:</p>
 * <ul>
 *   <li>Access to application services (NoteService, FolderService, TagService)</li>
 *   <li>Event subscription and publishing</li>
 *   <li>Command registration</li>
 *   <li>Menu item registration</li>
 *   <li>UI customization capabilities</li>
 * </ul>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.1.0
 */
public class PluginContext {
    
    private final NoteService noteService;
    private final FolderService folderService;
    private final TagService tagService;
    private final EventBus eventBus;
    private final CommandPalette commandPalette;
    private final PluginMenuRegistry menuRegistry;
    private final SidePanelRegistry sidePanelRegistry;
    private final String pluginId;
    
    /**
     * Creates a new plugin context.
     * 
     * @param pluginId          The ID of the plugin this context belongs to
     * @param noteService       The note service
     * @param folderService     The folder service
     * @param tagService        The tag service
     * @param eventBus          The event bus
     * @param commandPalette    The command palette
     * @param menuRegistry      The menu registry for adding menu items
     * @param sidePanelRegistry The side panel registry for adding UI panels
     */
    public PluginContext(String pluginId, NoteService noteService, FolderService folderService, 
                         TagService tagService, EventBus eventBus, CommandPalette commandPalette,
                         PluginMenuRegistry menuRegistry, SidePanelRegistry sidePanelRegistry) {
        this.pluginId = pluginId;
        this.noteService = noteService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.eventBus = eventBus;
        this.commandPalette = commandPalette;
        this.menuRegistry = menuRegistry;
        this.sidePanelRegistry = sidePanelRegistry;
    }
    
    // ==================== Service Access ====================
    
    /**
     * Gets the note service for note operations.
     * 
     * @return The note service
     */
    public NoteService getNoteService() {
        return noteService;
    }
    
    /**
     * Gets the folder service for folder operations.
     * 
     * @return The folder service
     */
    public FolderService getFolderService() {
        return folderService;
    }
    
    /**
     * Gets the tag service for tag operations.
     * 
     * @return The tag service
     */
    public TagService getTagService() {
        return tagService;
    }
    
    // ==================== Event System ====================
    
    /**
     * Subscribes to an event type.
     * 
     * @param <T>       The event type
     * @param eventType The event class
     * @param handler   The handler to call when the event is published
     * @return A subscription that can be cancelled
     */
    public <T extends AppEvent> EventBus.Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        return eventBus.subscribe(eventType, handler);
    }
    
    /**
     * Publishes an event.
     * 
     * @param <T>   The event type
     * @param event The event to publish
     */
    public <T extends AppEvent> void publish(T event) {
        eventBus.publish(event);
    }
    
    // ==================== Command Registration ====================
    
    /**
     * Registers a command in the Command Palette.
     * 
     * @param command The command to register
     */
    public void registerCommand(CommandPalette.Command command) {
        if (commandPalette != null) {
            commandPalette.addCommand(command);
        }
    }
    
    /**
     * Creates and registers a simple command.
     * 
     * @param name        The command name
     * @param description The command description
     * @param action      The action to execute
     */
    public void registerCommand(String name, String description, Runnable action) {
        registerCommand(new CommandPalette.Command(name, description, null, action));
    }
    
    /**
     * Creates and registers a command with a shortcut.
     * 
     * @param name        The command name
     * @param description The command description
     * @param shortcut    The keyboard shortcut
     * @param action      The action to execute
     */
    public void registerCommand(String name, String description, String shortcut, Runnable action) {
        registerCommand(new CommandPalette.Command(name, description, shortcut, action));
    }
    
    /**
     * Removes a command from the Command Palette.
     * 
     * @param commandName The name of the command to remove
     */
    public void unregisterCommand(String commandName) {
        if (commandPalette != null) {
            commandPalette.removeCommand(commandName);
        }
    }
    
    // ==================== Plugin Info ====================
    
    /**
     * Gets the ID of the plugin this context belongs to.
     * 
     * @return The plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * Logs a message from the plugin.
     * 
     * @param message The message to log
     */
    public void log(String message) {
        System.out.println("[Plugin:" + pluginId + "] " + message);
    }
    
    /**
     * Logs an error from the plugin.
     * 
     * @param message The error message
     * @param error   The exception
     */
    public void logError(String message, Throwable error) {
        System.err.println("[Plugin:" + pluginId + "] ERROR: " + message);
        if (error != null) {
            error.printStackTrace();
        }
    }
    
    // ==================== UI Interaction ====================
    
    /**
     * Requests to open a note in the editor.
     * The note will be loaded in the main editor view.
     * 
     * @param note The note to open
     */
    public void requestOpenNote(Note note) {
        if (note != null) {
            publish(new NoteEvents.NoteOpenRequestEvent(note));
            log("Requested to open note: " + note.getTitle());
        }
    }
    
    /**
     * Requests to refresh the notes list in the UI.
     */
    public void requestRefreshNotes() {
        publish(new NoteEvents.NotesRefreshRequestedEvent());
        log("Requested notes refresh");
    }
    
    /**
     * Shows an information dialog to the user.
     * 
     * @param title   Dialog title
     * @param header  Header text (can be null)
     * @param content Content message
     */
    public void showInfo(String title, String header, String content) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION
            );
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    /**
     * Shows an error dialog to the user.
     * 
     * @param title   Dialog title
     * @param message Error message
     */
    public void showError(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle(title);
            alert.setHeaderText("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // ==================== Menu Registration ====================
    
    /**
     * Registers a menu item in the Plugins menu.
     * 
     * @param category The menu category (e.g., "Core", "Productivity", "AI")
     * @param itemName The display name of the menu item
     * @param action   The action to execute when clicked
     */
    public void registerMenuItem(String category, String itemName, Runnable action) {
        if (menuRegistry != null) {
            menuRegistry.registerMenuItem(pluginId, category, itemName, action);
            log("Registered menu item: " + category + " > " + itemName);
        }
    }
    
    /**
     * Registers a menu item with a keyboard shortcut.
     * 
     * @param category The menu category
     * @param itemName The display name
     * @param shortcut The keyboard shortcut (e.g., "Ctrl+Shift+W")
     * @param action   The action to execute
     */
    public void registerMenuItem(String category, String itemName, String shortcut, Runnable action) {
        if (menuRegistry != null) {
            menuRegistry.registerMenuItem(pluginId, category, itemName, shortcut, action);
            log("Registered menu item: " + category + " > " + itemName + " (" + shortcut + ")");
        }
    }
    
    /**
     * Adds a separator in the plugin's menu category.
     * 
     * @param category The menu category
     */
    public void addMenuSeparator(String category) {
        if (menuRegistry != null) {
            menuRegistry.addMenuSeparator(pluginId, category);
        }
    }
    
    /**
     * Gets the menu registry for advanced menu operations.
     * 
     * @return The menu registry, or null if not available
     */
    public PluginMenuRegistry getMenuRegistry() {
        return menuRegistry;
    }
    
    // ==================== Side Panel Registration (UI Modification) ====================
    
    /**
     * Registers a side panel in the application's right sidebar.
     * This allows plugins to add custom UI components (Obsidian-style).
     * 
     * @param panelId A unique identifier for this panel
     * @param title   The display title for the panel header
     * @param content The JavaFX Node to display as panel content
     */
    public void registerSidePanel(String panelId, String title, Node content) {
        registerSidePanel(panelId, title, content, null);
    }
    
    /**
     * Registers a side panel with an icon.
     * 
     * @param panelId A unique identifier for this panel
     * @param title   The display title for the panel header
     * @param content The JavaFX Node to display as panel content
     * @param icon    An icon/emoji for the panel header (e.g., "📅")
     */
    public void registerSidePanel(String panelId, String title, Node content, String icon) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.registerSidePanel(pluginId, panelId, title, content, icon);
            log("Registered side panel: " + title);
        }
    }
    
    /**
     * Removes a side panel.
     * 
     * @param panelId The panel's unique ID
     */
    public void removeSidePanel(String panelId) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.removeSidePanel(pluginId, panelId);
            log("Removed side panel: " + panelId);
        }
    }
    
    /**
     * Gets the side panel registry for advanced operations.
     * 
     * @return The side panel registry, or null if not available
     */
    public SidePanelRegistry getSidePanelRegistry() {
        return sidePanelRegistry;
    }
    
    // ==================== Status Bar Items ====================
    
    /**
     * Registers a status bar item in the bottom bar.
     * 
     * @param itemId  A unique identifier for this item
     * @param content The JavaFX Node to display (typically a Label)
     */
    public void registerStatusBarItem(String itemId, Node content) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.registerStatusBarItem(pluginId, itemId, content);
            log("Registered status bar item: " + itemId);
        }
    }
    
    /**
     * Removes a status bar item.
     * 
     * @param itemId The item's unique ID
     */
    public void removeStatusBarItem(String itemId) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.removeStatusBarItem(pluginId, itemId);
        }
    }
    
    /**
     * Updates the content of a status bar item.
     * 
     * @param itemId  The item's unique ID
     * @param content The new content
     */
    public void updateStatusBarItem(String itemId, Node content) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.updateStatusBarItem(pluginId, itemId, content);
        }
    }
}
