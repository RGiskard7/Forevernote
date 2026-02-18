package com.example.forevernote.plugin;

import java.util.ArrayList;
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
 * Manages the lifecycle and state of all plugins.
 * 
 * <p>
 * Responsibilities:
 * </p>
 * <ul>
 * <li>Register and unregister plugins</li>
 * <li>Initialize and shutdown plugins</li>
 * <li>Enable and disable plugins</li>
 * <li>Track plugin states</li>
 * <li>Resolve plugin dependencies</li>
 * </ul>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.2.0
 */
public class PluginManager {

    private static final Logger logger = LoggerConfig.getLogger(PluginManager.class);

    /**
     * Plugin states.
     */
    public enum PluginState {
        REGISTERED, // Plugin is registered but not initialized
        INITIALIZED, // Plugin is initialized and ready
        ENABLED, // Plugin is enabled and active
        DISABLED, // Plugin is disabled
        ERROR // Plugin encountered an error
    }

    private final NoteService noteService;
    private final FolderService folderService;
    private final TagService tagService;
    private final EventBus eventBus;
    private final CommandPalette commandPalette;
    private final PluginMenuRegistry menuRegistry;
    private final SidePanelRegistry sidePanelRegistry;
    private final PreviewEnhancerRegistry previewEnhancerRegistry;

    // Plugin storage
    private final Map<String, Plugin> plugins = new HashMap<>();
    private final Map<String, PluginState> pluginStates = new HashMap<>();
    private final Map<String, PluginContext> pluginContexts = new HashMap<>();

    /**
     * Creates a new PluginManager.
     * 
     * @param noteService       The note service
     * @param folderService     The folder service
     * @param tagService        The tag service
     * @param eventBus          The event bus
     * @param commandPalette    The command palette
     * @param menuRegistry      The menu registry
     * @param sidePanelRegistry The side panel registry
     */
    public PluginManager(
            NoteService noteService,
            FolderService folderService,
            TagService tagService,
            EventBus eventBus,
            CommandPalette commandPalette,
            PluginMenuRegistry menuRegistry,
            SidePanelRegistry sidePanelRegistry,
            PreviewEnhancerRegistry previewEnhancerRegistry) {
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
     * Registers a plugin.
     * 
     * @param plugin The plugin to register
     * @return true if registered successfully, false otherwise
     */
    public boolean registerPlugin(Plugin plugin) {
        if (plugin == null) {
            logger.warning("Attempted to register null plugin");
            return false;
        }

        String pluginId = plugin.getId();
        if (pluginId == null || pluginId.isEmpty()) {
            logger.warning("Plugin has invalid ID");
            return false;
        }

        if (plugins.containsKey(pluginId)) {
            logger.warning("Plugin already registered: " + pluginId);
            return false;
        }

        plugins.put(pluginId, plugin);
        pluginStates.put(pluginId, PluginState.REGISTERED);
        logger.info("Registered plugin: " + plugin.getName() + " (" + pluginId + ")");

        return true;
    }

    /**
     * Unregisters a plugin.
     * 
     * @param pluginId The plugin ID
     * @return true if unregistered successfully, false otherwise
     */
    public boolean unregisterPlugin(String pluginId) {
        if (pluginId == null || !plugins.containsKey(pluginId)) {
            return false;
        }

        // Shutdown plugin first
        shutdownPlugin(pluginId);

        // Remove plugin
        plugins.remove(pluginId);
        pluginStates.remove(pluginId);
        pluginContexts.remove(pluginId);

        // Remove UI components
        if (menuRegistry != null) {
            menuRegistry.removePluginMenuItems(pluginId);
        }
        if (sidePanelRegistry != null) {
            sidePanelRegistry.removeAllSidePanels(pluginId);
        }

        logger.info("Unregistered plugin: " + pluginId);
        return true;
    }

    /**
     * Initializes a plugin.
     * 
     * @param pluginId The plugin ID
     * @return true if initialized successfully, false otherwise
     */
    public boolean initializePlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            logger.warning("Plugin not found: " + pluginId);
            return false;
        }

        PluginState currentState = pluginStates.get(pluginId);
        if (currentState == PluginState.INITIALIZED || currentState == PluginState.ENABLED) {
            logger.fine("Plugin already initialized: " + pluginId);
            return true;
        }

        // Check dependencies
        String[] dependencies = plugin.getDependencies();
        for (String depId : dependencies) {
            if (!plugins.containsKey(depId)) {
                logger.warning("Plugin " + pluginId + " depends on missing plugin: " + depId);
                pluginStates.put(pluginId, PluginState.ERROR);
                return false;
            }
            if (!isPluginEnabled(depId)) {
                logger.warning("Plugin " + pluginId + " depends on disabled plugin: " + depId);
                pluginStates.put(pluginId, PluginState.ERROR);
                return false;
            }
        }

        try {
            // Create context
            PluginContext context = new PluginContext(
                    pluginId,
                    noteService,
                    folderService,
                    tagService,
                    eventBus,
                    commandPalette,
                    menuRegistry,
                    sidePanelRegistry,
                    previewEnhancerRegistry);
            pluginContexts.put(pluginId, context);

            // Initialize plugin
            plugin.initialize(context);

            pluginStates.put(pluginId, PluginState.INITIALIZED);
            logger.info("Initialized plugin: " + plugin.getName() + " (" + pluginId + ")");

            // Enable if plugin is enabled by default
            if (plugin.isEnabled()) {
                enablePlugin(pluginId);
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
        // Sort by priority
        List<Plugin> sortedPlugins = new ArrayList<>(plugins.values());
        sortedPlugins.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));

        // Initialize in order
        for (Plugin plugin : sortedPlugins) {
            initializePlugin(plugin.getId());
        }

        logger.info("Initialized " + plugins.size() + " plugin(s)");
    }

    /**
     * Shuts down a plugin.
     * 
     * @param pluginId The plugin ID
     */
    public void shutdownPlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return;
        }

        try {
            plugin.shutdown();
            pluginStates.put(pluginId, PluginState.DISABLED);
            logger.info("Shut down plugin: " + pluginId);
        } catch (Exception e) {
            logger.warning("Error shutting down plugin " + pluginId + ": " + e.getMessage());
        }
    }

    /**
     * Shuts down all plugins.
     */
    public void shutdownAll() {
        for (String pluginId : new ArrayList<>(plugins.keySet())) {
            shutdownPlugin(pluginId);
        }
        logger.info("Shut down all plugins");
    }

    /**
     * Enables a plugin.
     * 
     * @param pluginId The plugin ID
     * @return true if enabled successfully, false otherwise
     */
    public boolean enablePlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return false;
        }

        PluginState currentState = pluginStates.get(pluginId);
        if (currentState == PluginState.ENABLED) {
            return true;
        }

        // Initialize if not already initialized
        if (currentState == PluginState.REGISTERED) {
            if (!initializePlugin(pluginId)) {
                return false;
            }
        }

        pluginStates.put(pluginId, PluginState.ENABLED);
        logger.info("Enabled plugin: " + pluginId);
        return true;
    }

    /**
     * Disables a plugin.
     * 
     * @param pluginId The plugin ID
     * @return true if disabled successfully, false otherwise
     */
    public boolean disablePlugin(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return false;
        }

        pluginStates.put(pluginId, PluginState.DISABLED);

        // Remove UI components
        if (menuRegistry != null) {
            menuRegistry.removePluginMenuItems(pluginId);
        }
        if (sidePanelRegistry != null) {
            sidePanelRegistry.removeAllSidePanels(pluginId);
        }

        logger.info("Disabled plugin: " + pluginId);
        return true;
    }

    /**
     * Checks if a plugin is enabled.
     * 
     * @param pluginId The plugin ID
     * @return true if enabled, false otherwise
     */
    public boolean isPluginEnabled(String pluginId) {
        PluginState state = pluginStates.get(pluginId);
        return state == PluginState.ENABLED || state == PluginState.INITIALIZED;
    }

    /**
     * Gets the state of a plugin.
     * 
     * @param pluginId The plugin ID
     * @return The plugin state, or null if plugin not found
     */
    public PluginState getPluginState(String pluginId) {
        return pluginStates.get(pluginId);
    }

    /**
     * Gets a plugin by ID.
     * 
     * @param pluginId The plugin ID
     * @return The plugin, or empty if not found
     */
    public Optional<Plugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    /**
     * Gets all registered plugins.
     * 
     * @return List of all plugins
     */
    public List<Plugin> getAllPlugins() {
        return new ArrayList<>(plugins.values());
    }

    /**
     * Gets all enabled plugins.
     * 
     * @return List of enabled plugins
     */
    public List<Plugin> getEnabledPlugins() {
        return plugins.values().stream()
                .filter(p -> isPluginEnabled(p.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Gets all disabled plugins.
     * 
     * @return List of disabled plugins
     */
    public List<Plugin> getDisabledPlugins() {
        return plugins.values().stream()
                .filter(p -> !isPluginEnabled(p.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Gets the number of registered plugins.
     * 
     * @return The plugin count
     */
    public int getPluginCount() {
        return plugins.size();
    }

    /**
     * Gets plugin information as a string.
     * 
     * @param pluginId The plugin ID
     * @return Plugin information string
     */
    public String getPluginInfo(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return "Plugin not found: " + pluginId;
        }

        PluginState state = pluginStates.get(pluginId);
        StringBuilder info = new StringBuilder();
        info.append("Name: ").append(plugin.getName()).append("\n");
        info.append("ID: ").append(plugin.getId()).append("\n");
        info.append("Version: ").append(plugin.getVersion()).append("\n");
        info.append("State: ").append(state).append("\n");
        if (!plugin.getAuthor().isEmpty()) {
            info.append("Author: ").append(plugin.getAuthor()).append("\n");
        }
        if (!plugin.getDescription().isEmpty()) {
            info.append("Description: ").append(plugin.getDescription()).append("\n");
        }

        return info.toString();
    }
}
