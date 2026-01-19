package com.example.forevernote.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;
import com.example.forevernote.ui.components.CommandPalette;

/**
 * Manages the lifecycle of plugins in Forevernote.
 * 
 * <p>The PluginManager handles:</p>
 * <ul>
 *   <li>Plugin registration and discovery</li>
 *   <li>Plugin lifecycle (initialize, enable, disable, shutdown)</li>
 *   <li>Dependency resolution</li>
 *   <li>Plugin state management</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>{@code
 * PluginManager manager = new PluginManager(noteService, folderService, tagService, eventBus, commandPalette);
 * manager.registerPlugin(new WordCountPlugin());
 * manager.registerPlugin(new DailyNotesPlugin());
 * manager.initializeAll();
 * }</pre>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.1.0
 */
public class PluginManager {
    
    private static final Logger logger = LoggerConfig.getLogger(PluginManager.class);
    
    private final Map<String, Plugin> plugins = new HashMap<>();
    private final Map<String, PluginState> pluginStates = new HashMap<>();
    private final Map<String, PluginContext> pluginContexts = new HashMap<>();
    
    private final NoteService noteService;
    private final FolderService folderService;
    private final TagService tagService;
    private final EventBus eventBus;
    private final CommandPalette commandPalette;
    private final PluginMenuRegistry menuRegistry;
    private final SidePanelRegistry sidePanelRegistry;
    
    /**
     * Plugin state enumeration.
     */
    public enum PluginState {
        REGISTERED,
        INITIALIZED,
        ENABLED,
        DISABLED,
        ERROR
    }
    
    /**
     * Creates a new PluginManager.
     * 
     * @param noteService       The note service
     * @param folderService     The folder service
     * @param tagService        The tag service
     * @param eventBus          The event bus
     * @param commandPalette    The command palette
     * @param menuRegistry      The menu registry for plugin menu items
     * @param sidePanelRegistry The side panel registry for plugin UI panels
     */
    public PluginManager(NoteService noteService, FolderService folderService, 
                        TagService tagService, EventBus eventBus, CommandPalette commandPalette,
                        PluginMenuRegistry menuRegistry, SidePanelRegistry sidePanelRegistry) {
        this.noteService = noteService;
        this.folderService = folderService;
        this.tagService = tagService;
        this.eventBus = eventBus;
        this.commandPalette = commandPalette;
        this.menuRegistry = menuRegistry;
        this.sidePanelRegistry = sidePanelRegistry;
        logger.info("PluginManager initialized");
    }
    
    // ==================== Plugin Registration ====================
    
    /**
     * Registers a plugin with the manager.
     * 
     * @param plugin The plugin to register
     * @return true if registration was successful
     */
    public boolean registerPlugin(Plugin plugin) {
        if (plugin == null) {
            logger.warning("Attempted to register null plugin");
            return false;
        }
        
        String id = plugin.getId();
        if (id == null || id.isEmpty()) {
            logger.warning("Plugin has invalid ID");
            return false;
        }
        
        if (plugins.containsKey(id)) {
            logger.warning("Plugin already registered: " + id);
            return false;
        }
        
        plugins.put(id, plugin);
        pluginStates.put(id, PluginState.REGISTERED);
        logger.info("Registered plugin: " + id + " v" + plugin.getVersion());
        return true;
    }
    
    /**
     * Unregisters a plugin.
     * 
     * @param pluginId The ID of the plugin to unregister
     * @return true if unregistration was successful
     */
    public boolean unregisterPlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return false;
        }
        
        // Shutdown if initialized
        PluginState state = pluginStates.get(pluginId);
        if (state == PluginState.INITIALIZED || state == PluginState.ENABLED) {
            shutdownPlugin(pluginId);
        }
        
        plugins.remove(pluginId);
        pluginStates.remove(pluginId);
        pluginContexts.remove(pluginId);
        logger.info("Unregistered plugin: " + pluginId);
        return true;
    }
    
    // ==================== Plugin Lifecycle ====================
    
    /**
     * Initializes a specific plugin.
     * 
     * @param pluginId The ID of the plugin to initialize
     * @return true if initialization was successful
     */
    public boolean initializePlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            logger.warning("Plugin not found: " + pluginId);
            return false;
        }
        
        PluginState state = pluginStates.get(pluginId);
        if (state != PluginState.REGISTERED) {
            logger.warning("Plugin already initialized or in error state: " + pluginId);
            return false;
        }
        
        // Check dependencies
        if (!checkDependencies(plugin)) {
            logger.warning("Plugin dependencies not met: " + pluginId);
            pluginStates.put(pluginId, PluginState.ERROR);
            return false;
        }
        
        try {
            // Create context
            PluginContext context = new PluginContext(
                pluginId, noteService, folderService, tagService, eventBus, commandPalette, menuRegistry, sidePanelRegistry
            );
            pluginContexts.put(pluginId, context);
            
            // Initialize
            plugin.initialize(context);
            pluginStates.put(pluginId, PluginState.INITIALIZED);
            logger.info("Initialized plugin: " + pluginId);
            
            // Auto-enable if plugin is enabled
            if (plugin.isEnabled()) {
                pluginStates.put(pluginId, PluginState.ENABLED);
            }
            
            return true;
        } catch (Exception e) {
            logger.severe("Failed to initialize plugin " + pluginId + ": " + e.getMessage());
            pluginStates.put(pluginId, PluginState.ERROR);
            return false;
        }
    }
    
    /**
     * Initializes all registered plugins in priority order.
     */
    public void initializeAll() {
        List<Plugin> sortedPlugins = new ArrayList<>(plugins.values());
        sortedPlugins.sort((a, b) -> a.getPriority() - b.getPriority());
        
        for (Plugin plugin : sortedPlugins) {
            initializePlugin(plugin.getId());
        }
        
        logger.info("Initialized " + getEnabledPlugins().size() + " plugins");
    }
    
    /**
     * Shuts down a specific plugin.
     * 
     * @param pluginId The ID of the plugin to shutdown
     */
    public void shutdownPlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return;
        }
        
        PluginState state = pluginStates.get(pluginId);
        if (state != PluginState.INITIALIZED && state != PluginState.ENABLED) {
            return;
        }
        
        try {
            plugin.shutdown();
            pluginStates.put(pluginId, PluginState.DISABLED);
            logger.info("Shutdown plugin: " + pluginId);
        } catch (Exception e) {
            logger.severe("Error shutting down plugin " + pluginId + ": " + e.getMessage());
            pluginStates.put(pluginId, PluginState.ERROR);
        }
    }
    
    /**
     * Shuts down all plugins.
     */
    public void shutdownAll() {
        for (String pluginId : new ArrayList<>(plugins.keySet())) {
            shutdownPlugin(pluginId);
        }
        logger.info("All plugins shutdown");
    }
    
    /**
     * Enables a plugin.
     * Re-initializes the plugin if it was previously disabled.
     * 
     * @param pluginId The ID of the plugin to enable
     * @return true if successful
     */
    public boolean enablePlugin(String pluginId) {
        PluginState state = pluginStates.get(pluginId);
        if (state == PluginState.DISABLED) {
            // Re-initialize the plugin to restore commands, menus, and panels
            Plugin plugin = plugins.get(pluginId);
            if (plugin != null) {
                try {
                    PluginContext context = pluginContexts.get(pluginId);
                    if (context != null) {
                        plugin.initialize(context);
                    }
                    pluginStates.put(pluginId, PluginState.ENABLED);
                    logger.info("Re-enabled plugin: " + pluginId);
                    return true;
                } catch (Exception e) {
                    logger.warning("Error re-enabling plugin: " + e.getMessage());
                    pluginStates.put(pluginId, PluginState.ERROR);
                    return false;
                }
            }
        } else if (state == PluginState.REGISTERED) {
            return initializePlugin(pluginId);
        }
        return false;
    }
    
    /**
     * Disables a plugin.
     * Calls shutdown on the plugin and removes all registered UI elements.
     * 
     * @param pluginId The ID of the plugin to disable
     * @return true if successful
     */
    public boolean disablePlugin(String pluginId) {
        PluginState state = pluginStates.get(pluginId);
        if (state == PluginState.ENABLED || state == PluginState.INITIALIZED) {
            // Call plugin shutdown to clean up commands and subscriptions
            Plugin plugin = plugins.get(pluginId);
            if (plugin != null) {
                try {
                    plugin.shutdown();
                } catch (Exception e) {
                    logger.warning("Error during plugin shutdown: " + e.getMessage());
                }
            }
            
            // Remove registered menu items
            if (menuRegistry != null) {
                menuRegistry.removePluginMenuItems(pluginId);
            }
            
            // Remove registered UI elements (side panels and status bar items)
            if (sidePanelRegistry != null) {
                sidePanelRegistry.removeAllSidePanels(pluginId);
                sidePanelRegistry.removeAllStatusBarItems(pluginId);
            }
            
            pluginStates.put(pluginId, PluginState.DISABLED);
            logger.info("Disabled plugin: " + pluginId);
            return true;
        }
        return false;
    }
    
    // ==================== Plugin Queries ====================
    
    /**
     * Gets a plugin by ID.
     * 
     * @param pluginId The plugin ID
     * @return Optional containing the plugin if found
     */
    public Optional<Plugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }
    
    /**
     * Gets the state of a plugin.
     * 
     * @param pluginId The plugin ID
     * @return The plugin state, or null if not found
     */
    public PluginState getPluginState(String pluginId) {
        return pluginStates.get(pluginId);
    }
    
    /**
     * Gets the context of a plugin.
     * 
     * @param pluginId The plugin ID
     * @return Optional containing the plugin context if initialized
     */
    public Optional<PluginContext> getPluginContext(String pluginId) {
        return Optional.ofNullable(pluginContexts.get(pluginId));
    }
    
    /**
     * Gets all registered plugins.
     * 
     * @return Unmodifiable list of all plugins
     */
    public List<Plugin> getAllPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(plugins.values()));
    }
    
    /**
     * Gets all enabled plugins.
     * 
     * @return List of enabled plugins
     */
    public List<Plugin> getEnabledPlugins() {
        return plugins.values().stream()
            .filter(p -> pluginStates.get(p.getId()) == PluginState.ENABLED)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all disabled plugins.
     * 
     * @return List of disabled plugins
     */
    public List<Plugin> getDisabledPlugins() {
        return plugins.values().stream()
            .filter(p -> pluginStates.get(p.getId()) == PluginState.DISABLED)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the count of plugins.
     * 
     * @return Total plugin count
     */
    public int getPluginCount() {
        return plugins.size();
    }
    
    /**
     * Checks if a plugin is enabled.
     * 
     * @param pluginId The plugin ID to check
     * @return true if the plugin exists and is enabled
     */
    public boolean isPluginEnabled(String pluginId) {
        PluginState state = pluginStates.get(pluginId);
        return state == PluginState.ENABLED;
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Checks if a plugin's dependencies are met.
     * 
     * @param plugin The plugin to check
     * @return true if all dependencies are available and initialized
     */
    private boolean checkDependencies(Plugin plugin) {
        for (String depId : plugin.getDependencies()) {
            if (!plugins.containsKey(depId)) {
                logger.warning("Missing dependency: " + depId + " for plugin " + plugin.getId());
                return false;
            }
            
            PluginState depState = pluginStates.get(depId);
            if (depState != PluginState.INITIALIZED && depState != PluginState.ENABLED) {
                logger.warning("Dependency not initialized: " + depId + " for plugin " + plugin.getId());
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets plugin info as a formatted string.
     * 
     * @param pluginId The plugin ID
     * @return Plugin info string
     */
    public String getPluginInfo(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return "Plugin not found: " + pluginId;
        }
        
        return String.format(
            "Plugin: %s v%s\n" +
            "ID: %s\n" +
            "Author: %s\n" +
            "Description: %s\n" +
            "State: %s",
            plugin.getName(),
            plugin.getVersion(),
            plugin.getId(),
            plugin.getAuthor().isEmpty() ? "Unknown" : plugin.getAuthor(),
            plugin.getDescription().isEmpty() ? "No description" : plugin.getDescription(),
            pluginStates.get(pluginId)
        );
    }
}
