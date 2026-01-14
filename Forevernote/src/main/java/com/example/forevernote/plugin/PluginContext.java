package com.example.forevernote.plugin;

import com.example.forevernote.event.AppEvent;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;
import com.example.forevernote.ui.components.CommandPalette;

import java.util.function.Consumer;

/**
 * Context provided to plugins for accessing application services.
 * 
 * <p>The PluginContext provides plugins with:</p>
 * <ul>
 *   <li>Access to application services (NoteService, FolderService, TagService)</li>
 *   <li>Event subscription and publishing</li>
 *   <li>Command registration</li>
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
    private final String pluginId;
    
    /**
     * Creates a new plugin context.
     * 
     * @param pluginId       The ID of the plugin this context belongs to
     * @param noteService    The note service
     * @param folderService  The folder service
     * @param tagService     The tag service
     * @param eventBus       The event bus
     * @param commandPalette The command palette
     */
    public PluginContext(String pluginId, NoteService noteService, FolderService folderService, 
                         TagService tagService, EventBus eventBus, CommandPalette commandPalette) {
        this.pluginId = pluginId;
        this.noteService = noteService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.eventBus = eventBus;
        this.commandPalette = commandPalette;
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
}
