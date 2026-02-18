package com.example.forevernote.plugin;

import java.util.function.Consumer;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.event.AppEvent;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;
import com.example.forevernote.ui.components.CommandPalette;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;

/**
 * Context provided to plugins during initialization.
 * Provides access to application services, UI registration, and event system.
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.2.0
 */
public class PluginContext {

    private static final Logger logger = LoggerConfig.getLogger(PluginContext.class);

    private final String pluginId;
    private final NoteService noteService;
    private final FolderService folderService;
    private final TagService tagService;
    private final EventBus eventBus;
    private final CommandPalette commandPalette;
    private final PluginMenuRegistry menuRegistry;
    private final SidePanelRegistry sidePanelRegistry;
    private final PreviewEnhancerRegistry previewEnhancerRegistry;

    /**
     * Creates a new PluginContext.
     * 
     * @param pluginId          The ID of the plugin using this context
     * @param noteService       The note service
     * @param folderService     The folder service
     * @param tagService        The tag service
     * @param eventBus          The event bus
     * @param commandPalette    The command palette
     * @param menuRegistry      The menu registry for registering menu items
     * @param sidePanelRegistry The side panel registry for registering UI panels
     */
    public PluginContext(
            String pluginId,
            NoteService noteService,
            FolderService folderService,
            TagService tagService,
            EventBus eventBus,
            CommandPalette commandPalette,
            PluginMenuRegistry menuRegistry,
            SidePanelRegistry sidePanelRegistry,
            PreviewEnhancerRegistry previewEnhancerRegistry) {
        this.pluginId = pluginId;
        this.noteService = noteService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.eventBus = eventBus;
        this.commandPalette = commandPalette;
        this.menuRegistry = menuRegistry;
        this.sidePanelRegistry = sidePanelRegistry;
        this.previewEnhancerRegistry = previewEnhancerRegistry;
    }

    /**
     * Gets the note service.
     * 
     * @return The note service
     */
    public NoteService getNoteService() {
        return noteService;
    }

    /**
     * Gets the folder service.
     * 
     * @return The folder service
     */
    public FolderService getFolderService() {
        return folderService;
    }

    /**
     * Gets the tag service.
     * 
     * @return The tag service
     */
    public TagService getTagService() {
        return tagService;
    }

    /**
     * Gets the event bus.
     * 
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Gets the command palette.
     * 
     * @return The command palette
     */
    public CommandPalette getCommandPalette() {
        return commandPalette;
    }

    /**
     * Registers a command in the Command Palette.
     * 
     * @param name        The command name
     * @param description The command description
     * @param action      The action to execute
     */
    public void registerCommand(String name, String description, Runnable action) {
        registerCommand(name, description, null, action);
    }

    /**
     * Registers a command in the Command Palette with a keyboard shortcut.
     * 
     * @param name        The command name
     * @param description The command description
     * @param shortcut    The keyboard shortcut (e.g., "Ctrl+Shift+W")
     * @param action      The action to execute
     */
    public void registerCommand(String name, String description, String shortcut, Runnable action) {
        if (commandPalette != null) {
            commandPalette.addCommand(new CommandPalette.Command(
                    name, description, shortcut != null ? shortcut : "", ">", "Plugins", action));
            logger.fine("Plugin " + pluginId + " registered command: " + name);
        }
    }

    /**
     * Unregisters a command from the Command Palette.
     * 
     * @param commandName The name of the command to unregister
     */
    public void unregisterCommand(String commandName) {
        if (commandPalette != null) {
            commandPalette.removeCommand(commandName);
            logger.fine("Plugin " + pluginId + " unregistered command: " + commandName);
        }
    }

    /**
     * Registers a menu item in a category.
     * 
     * @param category The menu category (e.g., "Core", "Productivity", "AI")
     * @param itemName The menu item name
     * @param action   The action to execute
     */
    public void registerMenuItem(String category, String itemName, Runnable action) {
        registerMenuItem(category, itemName, null, action);
    }

    /**
     * Registers a menu item in a category with a keyboard shortcut.
     * 
     * @param category The menu category
     * @param itemName The menu item name
     * @param shortcut The keyboard shortcut
     * @param action   The action to execute
     */
    public void registerMenuItem(String category, String itemName, String shortcut, Runnable action) {
        if (menuRegistry != null) {
            menuRegistry.registerMenuItem(pluginId, category, itemName, shortcut, action);
        }
    }

    /**
     * Adds a separator in a menu category.
     * 
     * @param category The menu category
     */
    public void addMenuSeparator(String category) {
        if (menuRegistry != null) {
            menuRegistry.addMenuSeparator(pluginId, category);
        }
    }

    /**
     * Registers a side panel in the right sidebar.
     * 
     * @param panelId The unique panel ID
     * @param title   The panel title
     * @param content The panel content (JavaFX Node)
     */
    public void registerSidePanel(String panelId, String title, Node content) {
        registerSidePanel(panelId, title, content, null);
    }

    /**
     * Registers a side panel with an icon.
     * 
     * @param panelId The unique panel ID
     * @param title   The panel title
     * @param content The panel content
     * @param icon    The icon (emoji or text)
     */
    public void registerSidePanel(String panelId, String title, Node content, String icon) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.registerSidePanel(pluginId, panelId, title, content, icon);
        }
    }

    /**
     * Removes a side panel.
     * 
     * @param panelId The panel ID to remove
     */
    public void removeSidePanel(String panelId) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.removeSidePanel(pluginId, panelId);
        }
    }

    /**
     * Shows or hides the plugin panels section.
     * 
     * @param visible true to show, false to hide
     */
    public void setPluginPanelsVisible(boolean visible) {
        if (sidePanelRegistry != null) {
            sidePanelRegistry.setPluginPanelsVisible(visible);
        }
    }

    /**
     * Checks if the plugin panels section is visible.
     * 
     * @return true if visible, false otherwise
     */
    public boolean isPluginPanelsVisible() {
        if (sidePanelRegistry != null) {
            return sidePanelRegistry.isPluginPanelsVisible();
        }
        return false;
    }

    /**
     * Subscribes to an event type.
     * 
     * @param <T>       The event type
     * @param eventType The event class
     * @param handler   The event handler
     * @return The subscription (can be used to unsubscribe)
     */
    public <T extends AppEvent> EventBus.Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        if (eventBus != null) {
            return eventBus.subscribe(eventType, handler);
        }
        return null;
    }

    /**
     * Publishes an event.
     * 
     * @param event The event to publish
     */
    public void publish(AppEvent event) {
        if (eventBus != null) {
            eventBus.publish(event);
        }
    }

    /**
     * Requests to open a note in the editor.
     * 
     * @param note The note to open
     */
    public void requestOpenNote(Note note) {
        if (eventBus != null && note != null) {
            Platform.runLater(() -> {
                eventBus.publish(new NoteEvents.NoteOpenRequestEvent(note));
            });
        }
    }

    /**
     * Requests a refresh of the notes list.
     */
    public void requestRefreshNotes() {
        if (eventBus != null) {
            Platform.runLater(() -> {
                eventBus.publish(new NoteEvents.NotesRefreshRequestedEvent());
            });
        }
    }

    /**
     * Shows an information dialog.
     * 
     * @param title   The dialog title
     * @param header  The dialog header
     * @param content The dialog content
     */
    public void showInfo(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /**
     * Shows an error dialog.
     * 
     * @param title   The dialog title
     * @param message The error message
     */
    public void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Logs a message.
     * 
     * @param message The message to log
     */
    public void log(String message) {
        logger.info("[" + pluginId + "] " + message);
    }

    /**
     * Logs an error.
     * 
     * @param message   The error message
     * @param throwable The exception
     */
    public void logError(String message, Throwable throwable) {
        logger.severe("[" + pluginId + "] " + message);
        if (throwable != null) {
            logger.severe("[" + pluginId + "] Exception: " + throwable.getMessage());
        }
    }

    /**
     * Gets the plugin ID.
     * 
     * @return The plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Registers a preview enhancer.
     * This allows the plugin to inject CSS/JS into the note preview.
     * 
     * @param enhancer The preview enhancer
     */
    public void registerPreviewEnhancer(PreviewEnhancer enhancer) {
        if (previewEnhancerRegistry != null) {
            previewEnhancerRegistry.registerPreviewEnhancer(pluginId, enhancer);
        }
    }

    /**
     * Unregisters the preview enhancer.
     */
    public void unregisterPreviewEnhancer() {
        if (previewEnhancerRegistry != null) {
            previewEnhancerRegistry.unregisterPreviewEnhancer(pluginId);
        }
    }
}
