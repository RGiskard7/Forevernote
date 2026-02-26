package com.example.forevernote.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.util.*;
import java.io.*;
import java.util.prefs.Preferences;
import java.sql.Connection;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.kordamp.ikonli.javafx.FontIcon;
import com.example.forevernote.event.events.UIEvents;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.database.SQLiteDB;
import com.example.forevernote.data.dao.interfaces.FactoryDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.*;
import com.example.forevernote.plugin.Plugin;
import com.example.forevernote.plugin.PluginManager;
import com.example.forevernote.plugin.PluginMenuRegistry;
import com.example.forevernote.plugin.SidePanelRegistry;
import com.example.forevernote.plugin.PreviewEnhancer;
import com.example.forevernote.plugin.PreviewEnhancerRegistry;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;
import com.example.forevernote.ui.components.CommandPalette;
import com.example.forevernote.ui.components.PluginManagerDialog;
import com.example.forevernote.ui.components.QuickSwitcher;
import com.example.forevernote.ui.workflow.FolderWorkflow;
import com.example.forevernote.ui.workflow.NoteWorkflow;
import com.example.forevernote.ui.workflow.PreviewWorkflow;
import com.example.forevernote.ui.workflow.TagWorkflow;
import com.example.forevernote.ui.workflow.ThemeWorkflow;

/**
 * Main controller for the Forevernote application.
 * Handles all UI interactions and manages the application state.
 * Implements PluginMenuRegistry and SidePanelRegistry to allow plugins to
 * register
 * menu items and UI panels dynamically (Modern-style).
 */
public class MainController implements PluginMenuRegistry, SidePanelRegistry, PreviewEnhancerRegistry {

    private static final Logger logger = LoggerConfig.getLogger(MainController.class);

    // Database and DAOs
    private Connection connection;
    private FactoryDAO factoryDAO;
    private FolderDAO folderDAO;
    private NoteDAO noteDAO;
    private TagDAO tagDAO;

    // Current state
    private Folder currentFolder;

    private Note getCurrentNote() {
        return editorController != null ? editorController.getCurrentNote() : null;
    }

    private boolean isModified() {
        return editorController != null && editorController.isModified();
    }

    private String currentFilterType = "all"; // "all", "folder", "tag", "favorites", "search"
    private Tag currentTag = null;

    // Event Listeners (Flags)
    // private boolean trashListenerAdded = false;

    // FXML UI Components
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private SplitPane contentSplitPane;

    private SplitPane navSplitPane;

    @FXML
    private SidebarController sidebarController;
    @FXML
    private NotesListController notesListController;
    @FXML
    private EditorController editorController;
    @FXML
    private ToolbarController toolbarController;

    // Sidebar (Tabs) Component References (Assigned from SidebarController)
    private javafx.scene.layout.VBox sidebarPane;
    private TabPane navigationTabPane;
    private TreeView<Folder> folderTreeView;
    private TextField filterFoldersField;
    private TreeItem<Folder> allNotesItem;
    private ListView<String> tagListView;
    private TreeView<Component> trashTreeView;
    private TextField filterTrashField;

    // Notes List Component References (Assigned from NotesListController)
    @FXML
    private VBox notesPanel;
    @FXML
    private Label notesPanelTitleLabel;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Button refreshBtn;
    @FXML
    private ListView<Note> notesListView;
    @FXML
    private HBox stackedModeHeader;

    // Editor Component References (Assigned from EditorController)
    private VBox editorContainer;
    private TextField noteTitleField;
    private ToggleButton toggleTagsBtn;
    private ToggleButton editorOnlyButton;
    private ToggleButton splitViewButton;
    private ToggleButton previewOnlyButton;
    private ToggleButton pinButton;
    private ToggleButton favoriteButton;
    private ToggleButton infoButton;
    private VBox tagsContainer;
    private FlowPane tagsFlowPane;
    private Label modifiedDateLabel;
    private SplitPane editorPreviewSplitPane;
    private VBox editorPane;
    private TextArea noteContentArea;
    private Label wordCountLabel;
    private VBox previewPane;
    private javafx.scene.web.WebView previewWebView;

    // Layout State
    private boolean isStackedLayout = false;

    // Preview Enhancers
    private final Map<String, PreviewEnhancer> previewEnhancers = new HashMap<>();
    @FXML
    private Separator toolbarSeparator2;
    @FXML
    private Separator toolbarSeparator3;
    @FXML
    private Button closeRightPanelBtn;

    // Right panel (Modern-style with collapsible sections)
    @FXML
    private VBox rightPanel;
    @FXML
    private VBox rightPanelContent;
    @FXML
    private VBox noteInfoSection;
    @FXML
    private HBox noteInfoHeader;
    @FXML
    private Label noteInfoCollapseIcon;
    @FXML
    private VBox noteInfoContent;

    // Preview and info labels
    @FXML
    private Label infoCreatedLabel;
    @FXML
    private Label infoModifiedLabel;
    @FXML
    private Label infoWordsLabel;
    @FXML
    private Label infoCharsLabel;
    @FXML
    private Label infoLatitudeLabel;
    @FXML
    private Label infoLongitudeLabel;

    // Theme and Language items moved to ToolbarController

    private ToggleGroup themeToggleGroup;
    private ToggleGroup languageToggleGroup;
    @FXML
    private Label infoAuthorLabel;
    @FXML
    private Label infoSourceUrlLabel;

    // Status bar
    @FXML
    private Label statusLabel;
    @FXML
    private Label noteCountLabel;
    @FXML
    private Label syncStatusLabel;

    // Plugin menu (dynamic)
    @FXML
    private Menu pluginsMenu;
    private final Map<String, Menu> pluginCategoryMenus = new HashMap<>();
    private final Map<String, List<MenuItem>> pluginMenuItems = new HashMap<>();

    // Plugin side panels (dynamic UI)
    @FXML
    private VBox pluginPanelsContainer;
    private final Map<String, VBox> pluginPanels = new HashMap<>();
    private final Map<String, List<String>> pluginPanelIds = new HashMap<>();

    // Plugin status bar items (dynamic UI)
    @FXML
    private HBox pluginStatusBarContainer;
    private final Map<String, javafx.scene.Node> pluginStatusBarItems = new HashMap<>();
    private final Map<String, List<String>> pluginStatusBarItemIds = new HashMap<>();

    // View mode state (editor/preview)
    private enum ViewMode {
        EDITOR_ONLY, SPLIT, PREVIEW_ONLY
    }

    private ViewMode currentViewMode = ViewMode.SPLIT;

    // Notes list view mode (list/grid)
    private enum NotesViewMode {
        LIST, GRID
    }

    private NotesViewMode currentNotesViewMode = NotesViewMode.LIST;

    // Grid view container (dynamically created)
    private javafx.scene.layout.TilePane notesGridPane;
    private javafx.scene.control.ScrollPane gridScrollPane;
    private VBox notesPanelContainer; // Reference to the notes panel container

    // UI Components for quick access
    private CommandPalette commandPalette;
    private QuickSwitcher quickSwitcher;

    // Services and Plugin System
    private NoteService noteService;
    private FolderService folderService;
    private TagService tagService;
    private EventBus eventBus;
    private PluginManager pluginManager;
    private PluginManagerDialog pluginManagerDialog;
    private NoteWorkflow noteWorkflow;
    private FolderWorkflow folderWorkflow;
    private TagWorkflow tagWorkflow;
    private ThemeWorkflow themeWorkflow;
    private PreviewWorkflow previewWorkflow;

    @FXML
    private java.util.ResourceBundle resources;

    private double uiFontSize = 13.0;
    private double editorFontSize = 14.0;

    private String getString(String key) {
        if (resources != null && resources.containsKey(key)) {
            return resources.getString(key);
        }
        return key; // Fallback to key if not found
    }

    private boolean isAllNotesVirtualFolder(Folder folder) {
        return folder != null && "ALL_NOTES_VIRTUAL".equals(folder.getId());
    }

    /**
     * Initialize the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        try {
            // Ensure navSplitPane for layout switching
            navSplitPane = new SplitPane();
            navSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

            // Initialize database connections
            initializeDatabase();

            if (toolbarController != null) {
                toolbarController.setEventBus(eventBus);
            }
            if (eventBus != null) {
                eventBus.subscribe(SystemActionEvent.class, this::handleSystemAction);
                subscribeToUIEvents();
            }
            if (sidebarController != null) {
                sidebarController.setEventBus(eventBus);
                sidebarController.setNoteService(noteService);
                sidebarController.setTagService(tagService);
                sidebarController.setFolderService(folderService);
                sidebarController.setBundle(resources);

                sidebarPane = sidebarController.getSidebarPane();
                navigationTabPane = sidebarController.getNavigationTabPane();
                folderTreeView = sidebarController.getFolderTreeView();
                filterFoldersField = sidebarController.getFilterFoldersField();
                tagListView = sidebarController.getTagListView();
                trashTreeView = sidebarController.getTrashTreeView();
                filterTrashField = sidebarController.getFilterTrashField();
            }
            if (notesListController != null) {
                notesListController.setEventBus(eventBus);
                notesListController.setServices(noteService, tagService, folderService);
                notesListController.setBundle(resources);
                notesPanel = notesListController.getNotesPanel();
                notesPanelTitleLabel = notesListController.getNotesPanelTitleLabel();
                sortComboBox = notesListController.getSortComboBox();
                refreshBtn = notesListController.getRefreshBtn();
                notesListView = notesListController.getNotesListView();
                stackedModeHeader = notesListController.getStackedModeHeader();
            }
            if (editorController != null) {
                editorController.setEventBus(eventBus);
                editorController.setServices(noteService);
                editorContainer = editorController.getEditorContainer();
                noteTitleField = editorController.getNoteTitleField();
                toggleTagsBtn = editorController.getToggleTagsBtn();
                editorOnlyButton = editorController.getEditorOnlyButton();
                splitViewButton = editorController.getSplitViewButton();
                previewOnlyButton = editorController.getPreviewOnlyButton();
                pinButton = editorController.getPinButton();
                favoriteButton = editorController.getFavoriteButton();
                infoButton = editorController.getInfoButton();
                tagsContainer = editorController.getTagsContainer();
                tagsFlowPane = editorController.getTagsFlowPane();
                modifiedDateLabel = editorController.getModifiedDateLabel();
                editorPreviewSplitPane = editorController.getEditorPreviewSplitPane();
                editorPane = editorController.getEditorPane();
                noteContentArea = editorController.getNoteContentArea();
                wordCountLabel = editorController.getWordCountLabel();
                previewPane = editorController.getPreviewPane();
                previewWebView = editorController.getPreviewWebView();
            }

            // Initialize UI components
            initializeSortOptions();
            initializeViewModeButtons();
            initializeIcons();
            initializeRightPanelSections();
            setupToolbarResponsiveness();
            initializeThemeMenu();
            initializeLanguageMenu();

            // Load initial data
            sidebarController.loadFolders();
            sidebarController.loadTags();
            sidebarController.loadRecentNotes();
            sidebarController.loadFavorites();
            sidebarController.loadTrashTree();

            // Initialize keyboard shortcuts after scene is ready
            Platform.runLater(this::initializeKeyboardShortcuts);

            // Initialize plugin system after scene is ready
            Platform.runLater(this::initializePluginSystem);

            updateStatus(getString("status.ready"));
            logger.info("MainController initialized successfully");

        } catch (Exception e) {
            logger.severe("Failed to initialize MainController: " + e.getMessage());
            updateStatus(java.text.MessageFormat.format(getString("status.error_details"), e.getMessage()));
        }
    }

    /**
     * Initialize database connections and DAOs.
     */
    /**
     * Initialize database connections and DAOs.
     */
    private void initializeDatabase() {
        try {
            // Determine storage type from AppConfig (defaulting to SQLite for now, but
            // could be "filesystem")
            // For now, let's auto-detect or use SQLite default.
            // Ideally AppConfig should have getStorageType()

            // To enable FileSystem mode, we can check a system property or config file
            Preferences prefs = Preferences.userNodeForPackage(MainController.class);
            String storageType = prefs.get("storage_type", System.getProperty("forevernote.storage", "sqlite"));

            if ("filesystem".equalsIgnoreCase(storageType)) {
                String customPath = prefs.get("filesystem_path", "");
                String dataDir;
                if (customPath != null && !customPath.isEmpty() && new File(customPath).exists()) {
                    dataDir = customPath;
                    logger.info("Using Custom File System Storage at " + dataDir);
                } else {
                    dataDir = com.example.forevernote.AppDataDirectory.getDataDirectory();
                    logger.info("Using Default File System Storage at " + dataDir);
                }

                factoryDAO = FactoryDAO.getFactory(FactoryDAO.FILE_SYSTEM_FACTORY, dataDir);
            } else {
                SQLiteDB db = SQLiteDB.getInstance();
                connection = db.openConnection();
                factoryDAO = FactoryDAO.getFactory(FactoryDAO.SQLITE_FACTORY, connection);
                logger.info("Initialized SQLite Storage");
            }

            folderDAO = factoryDAO.getFolderDAO();
            noteDAO = factoryDAO.getNoteDAO();
            tagDAO = factoryDAO.getLabelDAO();
            noteWorkflow = new NoteWorkflow(noteDAO);
            folderWorkflow = new FolderWorkflow();
            tagWorkflow = new TagWorkflow();
            themeWorkflow = new ThemeWorkflow();
            previewWorkflow = new PreviewWorkflow();

            // Initialize services
            noteService = new NoteService(noteDAO, folderDAO, tagDAO);
            folderService = new FolderService(folderDAO, noteDAO);
            tagService = new TagService(tagDAO, noteDAO);
            eventBus = EventBus.getInstance();

            logger.info("Database connections and services initialized");
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    @FXML
    public void handleSwitchStorage() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(getString("pref.storage"));
        alert.setHeaderText(getString("pref.storage.header"));
        alert.setContentText(getString("pref.storage.content"));

        ButtonType sqliteBtn = new ButtonType(getString("pref.storage.sqlite"));
        ButtonType filesystemBtn = new ButtonType(getString("pref.storage.filesystem"));
        ButtonType cancelBtn = new ButtonType(getString("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(sqliteBtn, filesystemBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() != cancelBtn) {
            String newType = "sqlite";
            String customPath = "";
            boolean changed = false;

            Preferences prefs = Preferences.userNodeForPackage(MainController.class);
            String currentType = prefs.get("storage_type", "sqlite");
            String currentPath = prefs.get("filesystem_path", "");

            if (result.get() == sqliteBtn) {
                newType = "sqlite";
                changed = !newType.equals(currentType);
            } else if (result.get() == filesystemBtn) {
                javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
                directoryChooser.setTitle(getString("pref.storage.browse"));

                File selectedDirectory = directoryChooser.showDialog(
                        mainSplitPane != null && mainSplitPane.getScene() != null ? mainSplitPane.getScene().getWindow()
                                : null);
                if (selectedDirectory != null) {
                    newType = "filesystem";
                    customPath = selectedDirectory.getAbsolutePath();
                    changed = !newType.equals(currentType) || !customPath.equals(currentPath);
                } else {
                    // User cancelled directory selection
                    return;
                }
            }

            if (changed) {
                prefs.put("storage_type", newType);
                prefs.put("filesystem_path", customPath);

                Alert restartAlert = new Alert(Alert.AlertType.INFORMATION);
                restartAlert.setTitle(getString("app.restart_required"));
                restartAlert.setHeaderText(null);
                restartAlert.setContentText(getString("app.restart_storage_message"));
                restartAlert.showAndWait();
            }
        }
    }

    /**
     * Initialize theme menu with toggle group.
     */
    private void initializeThemeMenu() {
        themeToggleGroup = new ToggleGroup();
        // Sync Themes Menu
        if (toolbarController != null) {
            if (toolbarController.getLightThemeMenuItem() != null) {
                toolbarController.getLightThemeMenuItem().setToggleGroup(themeToggleGroup);
            }
            if (toolbarController.getDarkThemeMenuItem() != null) {
                toolbarController.getDarkThemeMenuItem().setToggleGroup(themeToggleGroup);
            }
            if (toolbarController.getSystemThemeMenuItem() != null) {
                toolbarController.getSystemThemeMenuItem().setToggleGroup(themeToggleGroup);
            }
        }
        // Set initial selection based on current theme
        updateThemeMenuSelection();

        // Apply saved theme after scene is ready
        Platform.runLater(() -> {
            if ("system".equals(currentTheme)) {
                // For system theme, detect and apply
                String detectedTheme = detectSystemTheme();
                String savedTheme = currentTheme;
                currentTheme = detectedTheme;
                applyTheme();
                currentTheme = savedTheme;
            } else {
                applyTheme();
            }
        });
    }

    private void initializeLanguageMenu() {
        languageToggleGroup = new ToggleGroup();
        // Sync Language Menu
        if (toolbarController != null) {
            if (toolbarController.getEnglishLangMenuItem() != null) {
                toolbarController.getEnglishLangMenuItem().setToggleGroup(languageToggleGroup);
            }
            if (toolbarController.getSpanishLangMenuItem() != null) {
                toolbarController.getSpanishLangMenuItem().setToggleGroup(languageToggleGroup);
            }

            // Set current selection
            String currentLang = prefs.get("language", java.util.Locale.getDefault().getLanguage());
            if ("es".equals(currentLang)) {
                if (toolbarController.getSpanishLangMenuItem() != null)
                    toolbarController.getSpanishLangMenuItem().setSelected(true);
            } else {
                if (toolbarController.getEnglishLangMenuItem() != null)
                    toolbarController.getEnglishLangMenuItem().setSelected(true);
            }
        }
    }

    @FXML
    private void handleLanguageEnglish(ActionEvent event) {
        changeLanguage("en");
    }

    @FXML
    private void handleLanguageSpanish(ActionEvent event) {
        changeLanguage("es");
    }

    private void changeLanguage(String lang) {
        String currentLang = prefs.get("language", java.util.Locale.getDefault().getLanguage());
        if (!currentLang.equals(lang)) {
            prefs.put("language", lang);
            showAlert(Alert.AlertType.INFORMATION,
                    getString("app.restart_required"),
                    getString("app.restart_required"),
                    getString("app.restart_message"));
        }
    }

    /**
     * Update theme menu selection based on current theme.
     */
    private void updateThemeMenuSelection() {
        if (themeToggleGroup == null)
            return;

        if ("dark".equals(currentTheme)) {
            if (toolbarController != null) {
                if (toolbarController.getDarkThemeMenuItem() != null) {
                    toolbarController.getDarkThemeMenuItem().setSelected(true);
                }
            }
        } else if ("system".equalsIgnoreCase(currentTheme)) {
            if (toolbarController != null) {
                if (toolbarController.getSystemThemeMenuItem() != null) {
                    toolbarController.getSystemThemeMenuItem().setSelected(true);
                }
            }
        } else {
            if (toolbarController != null) {
                if (toolbarController.getLightThemeMenuItem() != null) {
                    toolbarController.getLightThemeMenuItem().setSelected(true);
                }
            }
        }
    }

    /**
     * Initialize global keyboard shortcuts.
     * Sets up Command Palette (Ctrl+P) and Quick Switcher (Ctrl+O).
     */
    private void initializeKeyboardShortcuts() {
        try {
            // Get the stage from any available component
            javafx.scene.Scene scene = null;
            if (mainSplitPane != null && mainSplitPane.getScene() != null) {
                scene = mainSplitPane.getScene();
            } else if (mainSplitPane != null && mainSplitPane.getScene() != null) {
                scene = mainSplitPane.getScene();
            }

            if (scene == null) {
                logger.warning("Scene not available for keyboard shortcuts");
                return;
            }

            Stage stage = (Stage) scene.getWindow();
            if (stage == null) {
                logger.warning("Stage not available for keyboard shortcuts");
                return;
            }

            // Initialize Command Palette
            commandPalette = new CommandPalette(stage);
            boolean isDark = isDarkThemeActive();
            commandPalette.setCommandHandler(this::executeCommand);

            // Initialize Quick Switcher
            quickSwitcher = new QuickSwitcher(stage);
            quickSwitcher.setDarkTheme(isDark);
            quickSwitcher.setOnNoteSelected(this::loadNoteInEditor);

            // Set up global key handler
            scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.isControlDown()) {
                    switch (event.getCode()) {
                        case P:
                            // Ctrl+P - Command Palette
                            if (!event.isShiftDown()) {
                                showCommandPalette();
                                event.consume();
                            }
                            break;
                        case O:
                            // Ctrl+O - Quick Switcher
                            showQuickSwitcher();
                            event.consume();
                            break;
                        default:
                            break;
                    }
                }
            });

            logger.info("Keyboard shortcuts initialized (Ctrl+P: Command Palette, Ctrl+O: Quick Switcher)");
        } catch (Exception e) {
            logger.warning("Failed to initialize keyboard shortcuts: " + e.getMessage());
        }
    }

    /**
     * Shows the Command Palette (Ctrl+P).
     */
    public void showCommandPalette() {
        if (commandPalette != null) {
            commandPalette.setDarkTheme(isDarkThemeActive());
            commandPalette.show();
            logger.info("Command Palette opened");
        } else {
            logger.warning("Command Palette not initialized yet");
            updateStatus(getString("status.command_palette_not_ready"));
        }
    }

    /**
     * Shows the Quick Switcher (Ctrl+O).
     */
    public void showQuickSwitcher() {
        if (quickSwitcher != null) {
            quickSwitcher.setDarkTheme(isDarkThemeActive());
            // Update notes list before showing
            quickSwitcher.setNotes(noteService.getAllNotes());
            quickSwitcher.show();
        }
    }

    /**
     * Initialize the plugin system.
     * Creates PluginManager, loads external plugins, and initializes them.
     * The core is completely decoupled - plugins register their own menu items
     * dynamically.
     */
    private void initializePluginSystem() {
        try {
            if (commandPalette == null) {
                logger.warning("CommandPalette not available, delaying plugin initialization");
                return;
            }

            // Create the plugin manager with all required services
            // Pass 'this' as both PluginMenuRegistry and SidePanelRegistry so plugins can
            // register UI components
            pluginManager = new PluginManager(noteService, folderService, tagService, eventBus, commandPalette, this,
                    this, this);

            // Load plugins from plugins/ directory (completely decoupled - no hardcoded
            // plugins)
            loadExternalPlugins();

            // Initialize all registered plugins (they will register their menu items during
            // init)
            pluginManager.initializeAll();

            Stage stage = mainSplitPane != null && mainSplitPane.getScene() != null
                    ? (Stage) mainSplitPane.getScene().getWindow()
                    : null;
            pluginManagerDialog = new PluginManagerDialog(stage, pluginManager);

            // Register plugin manager command in Command Palette
            commandPalette.addCommand(new CommandPalette.Command(
                    "Plugins: Manage Plugins",
                    "Open plugin manager to enable/disable plugins",
                    "Ctrl+Shift+P",
                    "=",
                    "Tools",
                    this::showPluginManager));

            // Subscribe to plugin events
            subscribeToPluginEvents();

            logger.info("Plugin system initialized with " + pluginManager.getPluginCount() + " plugins");
        } catch (Exception e) {
            logger.warning("Failed to initialize plugin system: " + e.getMessage());
        }
    }

    /**
     * Subscribe to events from plugins.
     */
    private void subscribeToUIEvents() {
        if (eventBus == null)
            return;

        eventBus.subscribe(UIEvents.ThemeChangedEvent.class, event -> {
            Platform.runLater(() -> {
                this.currentTheme = event.getTheme();
                prefs.put("theme", currentTheme);
                applyTheme();
                updateThemeMenuSelection();
            });
        });

        // Listen for note selections from the notes list
        eventBus.subscribe(NoteEvents.NoteSelectedEvent.class, event -> {
            Platform.runLater(() -> {
                Note note = event.getNote();
                if (note != null) {
                    loadNoteInEditor(note);
                }
            });
        });

        // Listen for notes loaded event to refresh UI portions managed directly by Main
        eventBus.subscribe(NoteEvents.NotesLoadedEvent.class, event -> {
            Platform.runLater(() -> {
                if (noteCountLabel != null) {
                    noteCountLabel.setText(event.getStatusMessage());
                }
                updateStatus(event.getStatusMessage());

                if (currentNotesViewMode == NotesViewMode.GRID) {
                    refreshGridView();
                }
            });
        });

        eventBus.subscribe(UIEvents.StatusUpdateEvent.class, event -> {
            Platform.runLater(() -> updateStatus(event.getMessage()));
        });

        eventBus.subscribe(NoteEvents.NoteCreatedEvent.class, event -> {
            Platform.runLater(() -> {
                // MainController doesn't need to do much here,
                // Sidebar and NotesList handle themselves.
                // But we might want to ensure editor is updated if needed (selection listener
                // handles it)
            });
        });

        eventBus.subscribe(NoteEvents.NoteDeletedEvent.class, event -> {
            Platform.runLater(() -> {
                Note current = getCurrentNote();
                if (current != null && current.getId().equals(event.getNoteId())) {
                    // Let EditorController handle the clearing via its own listener
                    // or call its clear method if it has one.
                    if (editorController != null) {
                        editorController.loadNote(null);
                    }
                    tagsFlowPane.getChildren().clear();
                    if (previewWebView != null) {
                        previewWebView.getEngine().loadContent("", "text/html");
                    }
                }
                refreshNotesList();
            });
        });

        eventBus.subscribe(FolderEvents.FolderDeletedEvent.class, event -> {
            Platform.runLater(() -> {
                if (currentFolder != null && currentFolder.getId().equals(event.getFolderId())) {
                    currentFolder = null;
                }
                // Refresh folder tree (though Sidebar manages the TreeView, Main still has
                // references in some places?)
                // Actually, sidebarController.folderTreeView.refresh() might be needed if not
                // done there.
                folderTreeView.refresh();
            });
        });

        eventBus.subscribe(NoteEvents.TrashItemDeletedEvent.class, event -> {
            Platform.runLater(() -> {
                // If it was the currently open note, we should have cleared it in
                // NoteDeletedEvent (move to trash)
                // If permanently deleted, we just need to ensure UI is consistent.
            });
        });

        // Listen for folder selections from the sidebar
        eventBus.subscribe(FolderEvents.FolderSelectedEvent.class, event -> {
            Platform.runLater(() -> {
                Folder selectedFolder = event.getFolder();
                if (selectedFolder != null) {
                    String id = selectedFolder.getId();
                    if (id == null && "INVISIBLE_ROOT".equals(selectedFolder.getTitle())) {
                        return; // Ignore invisible root
                    }

                    if ("ALL_NOTES_VIRTUAL".equals(id)) {
                        currentFolder = null;
                        if (notesListController != null) {
                            notesListController.loadAllNotes();
                        }
                    } else {
                        currentFolder = selectedFolder;
                        handleFolderSelection(selectedFolder);
                    }
                } else {
                    currentFolder = null;
                }
            });
        });

        // Listen for tag selections from the sidebar
        eventBus.subscribe(TagEvents.TagSelectedEvent.class, event -> {
            Platform.runLater(() -> {
                Tag tag = event.getTag();
                if (tagWorkflow == null) {
                    tagWorkflow = new TagWorkflow();
                }
                tagWorkflow.selectTag(tag, new TagWorkflow.TagSelectionPort() {
                    @Override
                    public void setCurrentFolderToNull() {
                        currentFolder = null;
                    }

                    @Override
                    public void setCurrentTag(Tag selectedTag) {
                        currentTag = selectedTag;
                    }

                    @Override
                    public void setCurrentFilterType(String filterType) {
                        currentFilterType = filterType;
                    }

                    @Override
                    public void loadNotesForTag(String tagTitle) {
                        if (notesListController != null) {
                            notesListController.loadNotesForTag(tagTitle);
                        }
                    }
                });
            });
        });

        // Listen for Note open requests (from Recent or Favorites lists, or plugins)
        eventBus.subscribe(NoteEvents.NoteOpenRequestEvent.class, event -> {
            Platform.runLater(() -> {
                Note note = event.getNote();
                if (note != null && note.getTitle() != null) {
                    // Find the full note object (recent/favorites lists only have titles for now)
                    Optional<Note> fullNote = noteService.getAllNotes().stream()
                            .filter(n -> note.getTitle().equals(n.getTitle()))
                            .findFirst();
                    if (fullNote.isPresent()) {
                        loadNoteInEditor(fullNote.get());
                    }
                }
            });
        });

        // Listen for trash item selections
        eventBus.subscribe(NoteEvents.TrashItemSelectedEvent.class, event -> {
            Platform.runLater(() -> {
                com.example.forevernote.data.models.interfaces.Component component = event.getComponent();
                if (component instanceof Note) {
                    loadNoteInEditor((Note) component);
                } else if (component instanceof Folder) {
                    handleFolderSelection((Folder) component);
                }
            });
        });
    }

    private void subscribeToPluginEvents() {
        // Listen for note open requests from plugins
        eventBus.subscribe(NoteEvents.NoteOpenRequestEvent.class, event -> {
            Platform.runLater(() -> {
                Note note = event.getNote();
                if (note != null) {
                    loadNoteInEditor(note);
                    // Also refresh notes list to show the note
                    notesListView.getSelectionModel().select(note);
                    logger.info("Opened note from plugin: " + note.getTitle());
                }
            });
        });

        // Plugin system events
        eventBus.subscribe(NoteEvents.NotesRefreshRequestedEvent.class, event -> {
            Platform.runLater(() -> {
                sidebarController.loadRecentNotes();
                sidebarController.loadTags();
                sidebarController.loadFavorites();
                logger.info("Refreshed notes from plugin request");
            });
        });
    }

    /**
     * Shows the Plugin Manager dialog (Modern-style).
     */
    public void showPluginManager() {
        if (pluginManagerDialog != null) {
            pluginManagerDialog.setDarkTheme(isDarkThemeActive());
            pluginManagerDialog.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(getString("dialog.plugin_manager.title"));
            alert.setHeaderText(getString("dialog.plugin_manager.not_initialized_header"));
            alert.setContentText(getString("dialog.plugin_manager.restart_required_content"));
            alert.showAndWait();
        }
    }

    // ==================== MENU HANDLERS ====================

    /**
     * Menu handler: Opens Command Palette (Tools > Command Palette).
     */
    @FXML
    private void handleCommandPalette(ActionEvent event) {
        showCommandPalette();
    }

    /**
     * Menu handler: Opens Quick Switcher (Tools > Quick Switcher).
     */
    @FXML
    private void handleQuickSwitcher(ActionEvent event) {
        showQuickSwitcher();
    }

    /**
     * Menu handler: Opens Plugin Manager (Tools > Plugins > Manage Plugins).
     */
    @FXML
    private void handlePluginManager(ActionEvent event) {
        showPluginManager();
    }

    // ==================== PLUGIN MENU REGISTRY IMPLEMENTATION ====================

    /**
     * Registers a menu item for a plugin.
     * Called by plugins during initialization to add their commands to the UI.
     */
    @Override
    public void registerMenuItem(String pluginId, String category, String itemName, Runnable action) {
        registerMenuItem(pluginId, category, itemName, null, action);
    }

    /**
     * Registers a menu item with a keyboard shortcut.
     */
    @Override
    public void registerMenuItem(String pluginId, String category, String itemName, String shortcut, Runnable action) {
        Platform.runLater(() -> {
            if (toolbarController == null || toolbarController.getPluginsMenu() == null) {
                logger.warning("Plugins menu not available for registration: " + itemName);
                return;
            }

            // Get or create the category submenu
            Menu categoryMenu = pluginCategoryMenus.get(category);
            if (categoryMenu == null) {
                categoryMenu = new Menu(category);
                pluginCategoryMenus.put(category, categoryMenu);

                // Add category menu after the separator (index 1 is after "Manage Plugins" and
                // separator)
                int insertIndex = Math.min(toolbarController.getPluginsMenu().getItems().size(), 2);
                toolbarController.getPluginsMenu().getItems().add(insertIndex, categoryMenu);
            }

            // Create the menu item
            MenuItem menuItem = new MenuItem(itemName);
            menuItem.setOnAction(e -> {
                if (pluginManager != null && pluginManager.isPluginEnabled(pluginId)) {
                    action.run();
                } else {
                    updateStatus(java.text.MessageFormat.format(getString("status.plugin_not_enabled"), pluginId));
                }
            });

            // Set shortcut if provided
            if (shortcut != null && !shortcut.isEmpty()) {
                try {
                    menuItem.setAccelerator(KeyCombination.keyCombination(shortcut));
                } catch (Exception e) {
                    logger.warning("Invalid shortcut for menu item: " + shortcut);
                }
            }

            // Add to category menu
            categoryMenu.getItems().add(menuItem);

            // Track menu items by plugin for removal later
            pluginMenuItems.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(menuItem);

            logger.fine("Registered menu item: " + category + " > " + itemName + " for plugin " + pluginId);
        });
    }

    /**
     * Adds a separator in the plugin's menu category.
     */
    @Override
    public void addMenuSeparator(String pluginId, String category) {
        Platform.runLater(() -> {
            Menu categoryMenu = pluginCategoryMenus.get(category);
            if (categoryMenu != null) {
                SeparatorMenuItem separator = new SeparatorMenuItem();
                categoryMenu.getItems().add(separator);
                pluginMenuItems.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(separator);
            }
        });
    }

    /**
     * Removes all menu items for a plugin.
     * Called when a plugin is disabled or unloaded.
     */
    @Override
    public void removePluginMenuItems(String pluginId) {
        Platform.runLater(() -> {
            List<MenuItem> items = pluginMenuItems.remove(pluginId);
            if (items != null) {
                for (MenuItem item : items) {
                    // Remove from all category menus
                    for (Menu categoryMenu : pluginCategoryMenus.values()) {
                        categoryMenu.getItems().remove(item);
                    }
                }

                // Clean up empty category menus
                pluginCategoryMenus.entrySet().removeIf(entry -> {
                    Menu menu = entry.getValue();
                    if (menu.getItems().isEmpty()) {
                        if (toolbarController != null && toolbarController.getPluginsMenu() != null) {
                            toolbarController.getPluginsMenu().getItems().remove(menu);
                        }
                        return true;
                    }
                    return false;
                });

                logger.info("Removed menu items for plugin: " + pluginId);
            }
        });
    }

    /**
     * Checks if a plugin is enabled.
     */
    @Override
    public boolean isPluginEnabled(String pluginId) {
        return pluginManager != null && pluginManager.isPluginEnabled(pluginId);
    }

    // ==================== SIDE PANEL REGISTRY IMPLEMENTATION (Modern-style UI)
    // ====================

    /**
     * Registers a side panel for a plugin.
     * Creates a collapsible section in the right sidebar with the plugin's content.
     */
    @Override
    public void registerSidePanel(String pluginId, String panelId, String title, javafx.scene.Node content,
            String icon) {
        Platform.runLater(() -> {
            if (pluginPanelsContainer == null) {
                logger.warning("Plugin panels container not available for registration: " + panelId);
                return;
            }

            String fullPanelId = pluginId + ":" + panelId;

            // Create panel wrapper
            VBox panelWrapper = new VBox();
            panelWrapper.getStyleClass().add("plugin-panel");
            panelWrapper.setSpacing(8);

            // Create header with icon, title, and collapse button
            HBox header = new HBox();
            header.getStyleClass().add("plugin-panel-header");
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            header.setSpacing(8);

            String headerText = (icon != null ? icon + " " : "") + title;
            Label titleLabel = new Label(headerText);
            titleLabel.getStyleClass().add("plugin-panel-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button collapseBtn = new Button("▼");
            collapseBtn.getStyleClass().add("plugin-panel-collapse-btn");
            collapseBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: transparent;");

            header.getChildren().addAll(titleLabel, spacer, collapseBtn);

            // Content wrapper
            VBox contentWrapper = new VBox();
            contentWrapper.getStyleClass().add("plugin-panel-content");
            contentWrapper.getChildren().add(content);

            // Toggle collapse
            collapseBtn.setOnAction(e -> {
                boolean isCollapsed = !contentWrapper.isVisible();
                contentWrapper.setVisible(isCollapsed);
                contentWrapper.setManaged(isCollapsed);
                collapseBtn.setText(isCollapsed ? "▼" : "▶");
            });

            panelWrapper.getChildren().addAll(header, contentWrapper);

            // Add to container
            pluginPanelsContainer.getChildren().add(panelWrapper);
            pluginPanels.put(fullPanelId, panelWrapper);
            pluginPanelIds.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(fullPanelId);

            // Show the container if it has content
            pluginPanelsContainer.setVisible(true);
            pluginPanelsContainer.setManaged(true);

            logger.fine("Registered side panel: " + title + " for plugin " + pluginId);
        });
    }

    /**
     * Removes a side panel.
     */
    @Override
    public void removeSidePanel(String pluginId, String panelId) {
        Platform.runLater(() -> {
            String fullPanelId = pluginId + ":" + panelId;
            VBox panel = pluginPanels.remove(fullPanelId);
            if (panel != null && pluginPanelsContainer != null) {
                pluginPanelsContainer.getChildren().remove(panel);

                // Update tracking
                List<String> ids = pluginPanelIds.get(pluginId);
                if (ids != null) {
                    ids.remove(fullPanelId);
                }

                // Hide container if empty
                if (pluginPanelsContainer.getChildren().isEmpty()) {
                    pluginPanelsContainer.setVisible(false);
                    pluginPanelsContainer.setManaged(false);
                }

                logger.info("Removed side panel: " + panelId);
            }
        });
    }

    /**
     * Removes all side panels for a plugin.
     */
    @Override
    public void removeAllSidePanels(String pluginId) {
        Platform.runLater(() -> {
            List<String> ids = pluginPanelIds.remove(pluginId);
            if (ids != null && pluginPanelsContainer != null) {
                for (String fullPanelId : ids) {
                    VBox panel = pluginPanels.remove(fullPanelId);
                    if (panel != null) {
                        pluginPanelsContainer.getChildren().remove(panel);
                    }
                }

                // Hide container if empty
                if (pluginPanelsContainer.getChildren().isEmpty()) {
                    pluginPanelsContainer.setVisible(false);
                    pluginPanelsContainer.setManaged(false);
                }

                logger.info("Removed all side panels for plugin: " + pluginId);
            }
        });
    }

    /**
     * Shows or hides the plugin panels section.
     */
    @Override
    public void setPluginPanelsVisible(boolean visible) {
        Platform.runLater(() -> {
            if (pluginPanelsContainer != null) {
                pluginPanelsContainer.setVisible(visible);
                pluginPanelsContainer.setManaged(visible);
            }
        });
    }

    /**
     * Checks if the plugin panels section is visible.
     */
    @Override
    public boolean isPluginPanelsVisible() {
        return pluginPanelsContainer != null && pluginPanelsContainer.isVisible();
    }

    // ==================== STATUS BAR ITEMS IMPLEMENTATION ====================

    /**
     * Registers a status bar item for a plugin.
     */
    public void registerStatusBarItem(String pluginId, String itemId, javafx.scene.Node content) {
        Platform.runLater(() -> {
            if (pluginStatusBarContainer == null) {
                logger.warning("Status bar container not available for: " + itemId);
                return;
            }

            String fullItemId = pluginId + ":" + itemId;

            // Wrap content with separator
            HBox wrapper = new HBox(8);
            wrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Separator sep = new Separator();
            sep.setOrientation(javafx.geometry.Orientation.VERTICAL);
            sep.getStyleClass().add("status-separator");

            wrapper.getChildren().addAll(sep, content);

            // Add to container
            pluginStatusBarContainer.getChildren().add(wrapper);
            pluginStatusBarItems.put(fullItemId, wrapper);
            pluginStatusBarItemIds.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(fullItemId);

            logger.fine("Registered status bar item: " + itemId + " for plugin " + pluginId);
        });
    }

    /**
     * Removes a status bar item.
     */
    public void removeStatusBarItem(String pluginId, String itemId) {
        Platform.runLater(() -> {
            String fullItemId = pluginId + ":" + itemId;
            javafx.scene.Node item = pluginStatusBarItems.remove(fullItemId);
            if (item != null && pluginStatusBarContainer != null) {
                pluginStatusBarContainer.getChildren().remove(item);

                List<String> ids = pluginStatusBarItemIds.get(pluginId);
                if (ids != null) {
                    ids.remove(fullItemId);
                }
            }
        });
    }

    /**
     * Updates a status bar item's content.
     */
    public void updateStatusBarItem(String pluginId, String itemId, javafx.scene.Node content) {
        Platform.runLater(() -> {
            String fullItemId = pluginId + ":" + itemId;
            javafx.scene.Node wrapper = pluginStatusBarItems.get(fullItemId);
            if (wrapper instanceof HBox) {
                HBox box = (HBox) wrapper;
                if (box.getChildren().size() > 1) {
                    box.getChildren().set(1, content);
                }
            }
        });
    }

    /**
     * Removes all status bar items for a plugin.
     * Called when a plugin is disabled or unloaded.
     */
    public void removeAllStatusBarItems(String pluginId) {
        Platform.runLater(() -> {
            List<String> ids = pluginStatusBarItemIds.remove(pluginId);
            if (ids != null && pluginStatusBarContainer != null) {
                for (String fullItemId : ids) {
                    javafx.scene.Node item = pluginStatusBarItems.remove(fullItemId);
                    if (item != null) {
                        pluginStatusBarContainer.getChildren().remove(item);
                    }
                }
            }
        });
    }

    /**
     * Loads plugins from the plugins/ directory.
     * Policy:
     * - Keep a minimal core plugin set bundled with the app (currently Mermaid).
     * - Load all optional/community plugins from plugins/ as external JAR files.
     */
    private void loadExternalPlugins() {
        try {
            // Register built-in plugins
            pluginManager.registerPlugin(new com.example.forevernote.plugin.mermaid.MermaidPlugin());

            List<Plugin> externalPlugins = com.example.forevernote.plugin.PluginLoader.loadExternalPlugins();
            int registeredCount = 0;

            for (Plugin plugin : externalPlugins) {
                if (pluginManager.registerPlugin(plugin)) {
                    registeredCount++;
                } else {
                    logger.warning("Failed to register external plugin: " + plugin.getName());
                }
            }

            if (registeredCount > 0) {
                logger.info("Registered " + registeredCount + " external plugin(s)");
            }
        } catch (Exception e) {
            logger.warning("Failed to load external plugins: " + e.getMessage());
        }
    }

    /**
     * Gets the plugin manager for external access.
     *
     * @return The plugin manager, or null if not initialized
     */
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Execute a command from the Command Palette.
     */
    private void executeCommand(String commandName) {
        logger.info("Executing command: " + commandName);

        switch (commandName) {
            // File commands
            case "New Note":
                handleNewNote(null);
                break;
            case "New Folder":
                handleNewFolder(null);
                break;
            case "Save":
                handleSave(null);
                break;
            case "Save All":
                handleSaveAll(null);
                break;
            case "Import":
                handleImport(null);
                break;
            case "Export":
                handleExport(null);
                break;
            case "Delete Note":
                handleDelete(null);
                break;

            // Edit commands
            case "Undo":
                handleUndo(null);
                break;
            case "Redo":
                handleRedo(null);
                break;
            case "Find":
                handleFind(null);
                break;
            case "Find and Replace":
                handleReplace(null);
                break;
            case "Cut":
                handleCut(null);
                break;
            case "Copy":
                handleCopy(null);
                break;
            case "Paste":
                handlePaste(null);
                break;

            // Format commands
            case "Bold":
                handleBold(null);
                break;
            case "Italic":
                handleItalic(null);
                break;
            case "Underline":
                handleUnderline(null);
                break;
            case "Insert Link":
                handleLink(null);
                break;
            case "Insert Image":
                handleImage(null);
                break;
            case "Insert Todo":
                handleTodoList(null);
                break;
            case "Insert List":
                handleNumberedList(null);
                break;

            // View commands
            case "Toggle Sidebar":
                handleToggleSidebar(null);
                break;
            case "Toggle Info Panel":
            case "Toggle Right Panel":
                handleToggleRightPanel(null);
                break;
            case "Editor Mode":
                handleEditorOnlyMode(null);
                break;
            case "Preview Mode":
                handlePreviewOnlyMode(null);
                break;
            case "Split Mode":
                handleSplitViewMode(null);
                break;
            case "Zoom In":
                handleZoomIn(null);
                break;
            case "Zoom Out":
                handleZoomOut(null);
                break;
            case "Reset Zoom":
                handleResetZoom(null);
                break;

            // Theme commands
            case "Light Theme":
                handleLightTheme(null);
                break;
            case "Dark Theme":
                handleDarkTheme(null);
                break;
            case "System Theme":
                handleSystemTheme(null);
                break;

            // Navigation commands
            case "Quick Switcher":
                showQuickSwitcher();
                break;
            case "Global Search":
                if (toolbarController != null && toolbarController.getSearchField() != null)
                    toolbarController.getSearchField().requestFocus();
                break;
            case "Go to All Notes":
                break;
            case "Go to Favorites":
                sidebarController.loadFavorites();
                break;
            case "Go to Recent":
                sidebarController.loadRecentNotes();
                break;

            // Tools commands
            case "Tag Manager":
                handleTagsManager(null);
                break;
            case "Preferences":
                handlePreferences(null);
                break;
            case "Toggle Favorite":
                handleToggleFavorite(null);
                break;
            case "Refresh":
                handleRefresh(null);
                break;
            case "Plugins: Manage Plugins":
                showPluginManager();
                break;

            // Help commands
            case "Keyboard Shortcuts":
                handleKeyboardShortcuts(null);
                break;
            case "Documentation":
                handleDocumentation(null);
                break;
            case "About Forevernote":
                handleAbout(null);
                break;

            default:
                // Try to find command in Command Palette (for plugin commands)
                if (commandPalette != null) {
                    CommandPalette.Command cmd = commandPalette.findCommand(commandName);
                    if (cmd != null) {
                        cmd.execute();
                        return;
                    }
                }
                logger.warning("Unknown command: " + commandName);
                updateStatus(java.text.MessageFormat.format(getString("status.unknown_command"), commandName));
        }
    }

    /**
     * Initialize sort options.
     */
    private void initializeSortOptions() {
        sortComboBox.getItems().addAll(
                getString("sort.title_az"),
                getString("sort.title_za"),
                getString("sort.created_newest"),
                getString("sort.created_oldest"),
                getString("sort.modified_newest"),
                getString("sort.modified_oldest"));
        sortComboBox.getSelectionModel().selectFirst();

        sortComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    sortNotes(newValue);
                });
    }

    /**
     * Initialize view mode toggle buttons (Modern-style).
     */
    private void initializeViewModeButtons() {
        // Create toggle group for editor/preview view modes
        ToggleGroup viewModeGroup = new ToggleGroup();
        if (editorOnlyButton != null) {
            editorOnlyButton.setToggleGroup(viewModeGroup);
        }
        if (splitViewButton != null) {
            splitViewButton.setToggleGroup(viewModeGroup);
            splitViewButton.setSelected(true); // Default to split view
        }
        if (previewOnlyButton != null) {
            previewOnlyButton.setToggleGroup(viewModeGroup);
        }

        // Create toggle group for list/grid view modes
        ToggleGroup notesViewGroup = new ToggleGroup();
        if (toolbarController != null && toolbarController.getListViewButton() != null) {
            toolbarController.getListViewButton().setToggleGroup(notesViewGroup);
            toolbarController.getListViewButton().setSelected(true);
        }
        if (toolbarController != null && toolbarController.getGridViewButton() != null) {
            toolbarController.getGridViewButton().setToggleGroup(notesViewGroup);
        }

        // Initialize grid view container
        initializeGridView();

        // Apply initial view mode
        applyViewMode();
    }

    /**
     * Initialize the grid view container.
     */
    private void initializeGridView() {
        notesGridPane = new javafx.scene.layout.TilePane();
        notesGridPane.setPrefColumns(3);
        notesGridPane.setHgap(12);
        notesGridPane.setVgap(12);
        notesGridPane.setPadding(new javafx.geometry.Insets(12));
        notesGridPane.setStyle("-fx-background-color: transparent;");

        gridScrollPane = new javafx.scene.control.ScrollPane(notesGridPane);
        gridScrollPane.setFitToWidth(true);
        gridScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        gridScrollPane.getStyleClass().add("notes-grid-scroll");

        // Store reference to the notes panel container
        if (notesListView != null && notesListView.getParent() instanceof VBox) {
            notesPanelContainer = (VBox) notesListView.getParent();
        }
    }

    /**
     * Handle switching to list view.
     */
    @FXML
    private void handleListView(ActionEvent event) {
        if (currentNotesViewMode != NotesViewMode.LIST) {
            currentNotesViewMode = NotesViewMode.LIST;
            applyNotesViewMode();
            updateStatus(getString("status.view_list"));
        }
    }

    /**
     * Handle switching to grid view.
     */
    @FXML
    private void handleGridView(ActionEvent event) {
        if (currentNotesViewMode != NotesViewMode.GRID) {
            currentNotesViewMode = NotesViewMode.GRID;
            applyNotesViewMode();
            updateStatus(getString("status.view_grid"));
        }
    }

    /**
     * Apply the current notes view mode (list/grid).
     */
    private void applyNotesViewMode() {
        if (notesListView == null || gridScrollPane == null)
            return;

        // Get or update the panel container reference
        if (notesPanelContainer == null) {
            javafx.scene.Parent parent = notesListView.getParent();
            if (parent instanceof VBox) {
                notesPanelContainer = (VBox) parent;
            } else {
                // Try to get parent from gridScrollPane if list is not in scene
                parent = gridScrollPane.getParent();
                if (parent instanceof VBox) {
                    notesPanelContainer = (VBox) parent;
                }
            }
        }

        if (notesPanelContainer == null) {
            logger.warning("Could not find notes panel container");
            return;
        }

        Platform.runLater(() -> {
            if (currentNotesViewMode == NotesViewMode.GRID) {
                // Switch to grid view
                if (notesPanelContainer.getChildren().contains(notesListView)) {
                    notesPanelContainer.getChildren().remove(notesListView);
                }
                if (!notesPanelContainer.getChildren().contains(gridScrollPane)) {
                    notesPanelContainer.getChildren().add(gridScrollPane);
                    VBox.setVgrow(gridScrollPane, Priority.ALWAYS);
                }
                refreshGridView();
            } else {
                // Switch to list view
                if (notesPanelContainer.getChildren().contains(gridScrollPane)) {
                    notesPanelContainer.getChildren().remove(gridScrollPane);
                }
                if (!notesPanelContainer.getChildren().contains(notesListView)) {
                    notesPanelContainer.getChildren().add(notesListView);
                    VBox.setVgrow(notesListView, Priority.ALWAYS);
                }
            }
        });
    }

    /**
     * Refresh the grid view with current notes.
     */
    private void refreshGridView() {
        if (notesGridPane == null)
            return;

        notesGridPane.getChildren().clear();

        // Use a copy to avoid ConcurrentModificationException and ensure we have
        // current items
        List<Note> notes = new ArrayList<>(notesListView.getItems());

        for (Note note : notes) {
            VBox card = createNoteCard(note);
            notesGridPane.getChildren().add(card);
        }
    }

    /**
     * Setup responsive behavior for the toolbar.
     */
    private void setupToolbarResponsiveness() {
        if (toolbarController == null || toolbarController.getToolbarHBox() == null
                || toolbarController.getToolbarOverflowBtn() == null)
            return;

        toolbarController.getToolbarHBox().widthProperty().addListener((obs, oldVal, newVal) -> {
            updateToolbarOverflow(newVal.doubleValue());
        });

        // Initial check
        Platform.runLater(() -> updateToolbarOverflow(toolbarController.getToolbarHBox().getWidth()));
    }

    /**
     * Update toolbar items based on available width.
     */
    private void updateToolbarOverflow(double width) {
        if (toolbarController == null || toolbarController.getToolbarHBox() == null
                || toolbarController.getToolbarOverflowBtn() == null)
            return;

        // Thresholds for different buttons (cumulative widths approx)
        boolean showSearch = width > 750;
        boolean showFileActions = width > 550;
        boolean showLayoutToggles = width > 400;

        toolbarController.setResponsiveState(showSearch, showLayoutToggles, showFileActions);

        // Update separator 2 (between file actions and save/delete) - actually between
        // file actions group and search
        // Layout Toggles
        if (toolbarController != null) {
            toolbarController.getSidebarToggleBtn().setVisible(showLayoutToggles);
            toolbarController.getSidebarToggleBtn().setManaged(showLayoutToggles);
            toolbarController.getNotesPanelToggleBtn().setVisible(showLayoutToggles);
            toolbarController.getNotesPanelToggleBtn().setManaged(showLayoutToggles);
            toolbarController.getSearchField().setVisible(showSearch);
            toolbarController.getSearchField().setManaged(showSearch);
        }
        if (toolbarController != null) {
            toolbarController.getLayoutSwitchBtn().setVisible(showLayoutToggles);
            toolbarController.getLayoutSwitchBtn().setManaged(showLayoutToggles);
            toolbarController.getToolbarSeparator1().setVisible(showLayoutToggles);
            toolbarController.getToolbarSeparator1().setManaged(showLayoutToggles);
        }

        // Manage Overflow Menu
        toolbarController.getToolbarOverflowBtn().getItems().clear();
        boolean needsOverflow = !showFileActions || !showSearch || !showLayoutToggles;

        if (needsOverflow && toolbarController != null) {
            toolbarController.getToolbarOverflowBtn().getItems().clear();
            if (!showSearch) {
                MenuItem searchItem = new MenuItem(getString("app.search.placeholder"));
                searchItem.setOnAction(e -> toolbarController.getSearchField().requestFocus());
                toolbarController.getToolbarOverflowBtn().getItems().add(searchItem);
                toolbarController.getToolbarOverflowBtn().getItems().add(new SeparatorMenuItem());
            }
            if (!showFileActions) {
                MenuItem newNoteItem = new MenuItem(getString("action.new_note"));
                newNoteItem.setOnAction(e -> handleNewNote(null));
                MenuItem newFolderItem = new MenuItem(getString("action.new_folder"));
                newFolderItem.setOnAction(e -> handleNewFolder(null));
                MenuItem newTagItem = new MenuItem(getString("action.new_tag"));
                newTagItem.setOnAction(e -> handleNewTag(null));
                MenuItem saveItem = new MenuItem(getString("action.save"));
                saveItem.setOnAction(e -> handleSave(null));
                MenuItem deleteItem = new MenuItem(getString("action.delete"));
                deleteItem.setOnAction(e -> handleDelete(null));
                toolbarController.getToolbarOverflowBtn().getItems().addAll(newNoteItem, newFolderItem, newTagItem,
                        saveItem,
                        new SeparatorMenuItem(), deleteItem);
            }
            if (!showLayoutToggles) {
                if (!toolbarController.getToolbarOverflowBtn().getItems().isEmpty())
                    toolbarController.getToolbarOverflowBtn().getItems().add(new SeparatorMenuItem());
                MenuItem toggleSidebar = new MenuItem(getString("action.toggle_sidebar"));
                toggleSidebar.setOnAction(e -> handleToggleSidebar(null));
                MenuItem toggleNotes = new MenuItem(getString("action.toggle_notes_list"));
                toggleNotes.setOnAction(e -> handleToggleNotesPanel(null));
                MenuItem switchLayout = new MenuItem(getString("action.switch_layout"));
                switchLayout.setOnAction(e -> handleViewLayoutSwitch(null));
                toolbarController.getToolbarOverflowBtn().getItems().addAll(toggleSidebar, toggleNotes, switchLayout);
            }

            toolbarController.getToolbarOverflowBtn().setVisible(true);
            toolbarController.getToolbarOverflowBtn().setManaged(true);
        } else if (toolbarController != null) {
            toolbarController.getToolbarOverflowBtn().getItems().clear();
            toolbarController.getToolbarOverflowBtn().setVisible(false);
            toolbarController.getToolbarOverflowBtn().setManaged(false);
        }
    }

    /**
     * Create a note card for grid view.
     */
    private VBox createNoteCard(Note note) {
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setPrefHeight(140);
        card.setPadding(new javafx.geometry.Insets(12));
        card.getStyleClass().add("note-card");

        // Determine theme colors
        boolean isDark = isDarkThemeActive();

        String bgColor = isDark ? "#2d2d2d" : "#ffffff";
        String borderColor = isDark ? "#404040" : "#e0e0e0";
        String titleColor = isDark ? "#e0e0e0" : "#333333";
        String previewColor = isDark ? "#888888" : "#666666";
        String dateColor = isDark ? "#666666" : "#999999";

        card.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 8; " +
                        "-fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);",
                bgColor, borderColor));

        // Title with favorite/pin indicators
        HBox titleRow = new HBox(5);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        if (note.isPinned()) {
            FontIcon pinIcon = new FontIcon("fth-map-pin");
            pinIcon.getStyleClass().add("feather-pin-active");
            pinIcon.setIconSize(12);
            titleRow.getChildren().add(pinIcon);
        }

        if (note.isFavorite()) {
            FontIcon favIcon = new FontIcon("fth-star");
            favIcon.setIconColor(javafx.scene.paint.Color.GOLD);
            favIcon.setIconSize(12);
            titleRow.getChildren().add(favIcon);
        }

        Label titleLabel = new Label(note.getTitle() != null ? note.getTitle() : getString("app.untitled"));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + titleColor + ";");
        titleLabel.setWrapText(true);
        titleLabel.setMaxHeight(40);
        titleRow.getChildren().add(titleLabel);

        // Preview text
        String preview = note.getContent() != null && !note.getContent().isEmpty()
                ? note.getContent().replaceAll("^#+\\s*", "").replaceAll("\\n", " ").trim()
                : "";
        if (preview.length() > 80) {
            preview = preview.substring(0, 77) + "...";
        }
        Label previewLabel = new Label(preview);
        previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + previewColor + ";");
        previewLabel.setWrapText(true);
        previewLabel.setMaxHeight(60);
        VBox.setVgrow(previewLabel, Priority.ALWAYS);

        // Date
        String dateText = note.getModifiedDate() != null ? note.getModifiedDate() : note.getCreatedDate();
        if (dateText != null && dateText.length() > 10) {
            dateText = dateText.substring(0, 10);
        }
        Label dateLabel = new Label(dateText != null ? dateText : "");
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + dateColor + ";");

        card.getChildren().addAll(titleRow, previewLabel, dateLabel);

        // Hover effects
        card.setOnMouseEntered(e -> {
            String hoverBg = isDark ? "#3a3a3a" : "#f5f5f5";
            card.setStyle(
                    card.getStyle().replace("-fx-background-color: " + bgColor, "-fx-background-color: " + hoverBg));
        });
        card.setOnMouseExited(e -> {
            card.setStyle(card.getStyle().replace("-fx-background-color: " + (isDark ? "#3a3a3a" : "#f5f5f5"),
                    "-fx-background-color: " + bgColor));
        });

        // Click to select and load note
        card.setOnMouseClicked(e -> {
            notesListView.getSelectionModel().select(note);
            loadNoteInEditor(note);
        });

        // Setup drag for grid cards
        setupNoteCardDrag(card, note);

        return card;
    }

    /**
     * Setup drag for note card in grid view.
     */
    private void setupNoteCardDrag(VBox card, Note note) {
        card.setOnDragDetected(event -> {
            javafx.scene.input.Dragboard db = card.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString("note:" + note.getId());
            db.setContent(content);

            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(javafx.scene.paint.Color.TRANSPARENT);
            db.setDragView(card.snapshot(params, null));

            event.consume();
            updateStatus(java.text.MessageFormat.format(getString("status.dragging"), note.getTitle()));
        });

        card.setOnDragDone(event -> {
            if (event.getTransferMode() == javafx.scene.input.TransferMode.MOVE) {
                updateStatus(getString("status.note_moved"));
            }
            event.consume();
        });
    }

    /**
     * Apply the current view mode to the UI.
     */
    private void applyViewMode() {
        if (editorPreviewSplitPane == null || editorPane == null || previewPane == null) {
            return;
        }

        switch (currentViewMode) {
            case EDITOR_ONLY:
                editorPane.setVisible(true);
                editorPane.setManaged(true);
                previewPane.setVisible(false);
                previewPane.setManaged(false);
                // Remove preview from split pane
                if (editorPreviewSplitPane.getItems().contains(previewPane)) {
                    editorPreviewSplitPane.getItems().remove(previewPane);
                }
                if (!editorPreviewSplitPane.getItems().contains(editorPane)) {
                    editorPreviewSplitPane.getItems().add(editorPane);
                }
                break;

            case PREVIEW_ONLY:
                editorPane.setVisible(false);
                editorPane.setManaged(false);
                previewPane.setVisible(true);
                previewPane.setManaged(true);
                // Remove editor from split pane
                if (editorPreviewSplitPane.getItems().contains(editorPane)) {
                    editorPreviewSplitPane.getItems().remove(editorPane);
                }
                if (!editorPreviewSplitPane.getItems().contains(previewPane)) {
                    editorPreviewSplitPane.getItems().add(previewPane);
                }
                updatePreview(); // Ensure preview is updated
                break;

            case SPLIT:
            default:
                editorPane.setVisible(true);
                editorPane.setManaged(true);
                previewPane.setVisible(true);
                previewPane.setManaged(true);
                // Ensure both are in split pane
                editorPreviewSplitPane.getItems().clear();
                editorPreviewSplitPane.getItems().addAll(editorPane, previewPane);
                editorPreviewSplitPane.setDividerPositions(0.5);
                updatePreview();
                break;
        }

        // Update button states
        if (editorOnlyButton != null) {
            editorOnlyButton.setSelected(currentViewMode == ViewMode.EDITOR_ONLY);
        }
        if (splitViewButton != null) {
            splitViewButton.setSelected(currentViewMode == ViewMode.SPLIT);
        }
        if (previewOnlyButton != null) {
            previewOnlyButton.setSelected(currentViewMode == ViewMode.PREVIEW_ONLY);
        }
    }

    /**
     * Handle editor-only mode button click.
     */
    @FXML
    private void handleEditorOnlyMode(ActionEvent event) {
        currentViewMode = ViewMode.EDITOR_ONLY;
        applyViewMode();
        updateStatus(getString("status.mode_editor"));
    }

    /**
     * Handle split view mode button click.
     */
    @FXML
    private void handleSplitViewMode(ActionEvent event) {
        currentViewMode = ViewMode.SPLIT;
        applyViewMode();
        updateStatus(getString("status.mode_split"));
    }

    /**
     * Handle preview-only mode button click.
     */
    @FXML
    private void handlePreviewOnlyMode(ActionEvent event) {
        currentViewMode = ViewMode.PREVIEW_ONLY;
        applyViewMode();
        updateStatus(getString("status.mode_preview"));
    }

    /**
     * Initialize all icons using Ikonli (Feather icons - similar to Obsidian).
     * Uses simple text labels that work reliably across all platforms.
     */
    private void initializeIcons() {
        // Icons are now set via FXML using Ikonli FontIcon
        // This method can remain empty or be removed in future refactoring
    }

    /**
     * Toggle the right panel visibility.
     */
    @FXML
    private void handleToggleRightPanel(ActionEvent event) {
        if (rightPanel != null) {
            boolean nextVisible = !rightPanel.isVisible();
            rightPanel.setVisible(nextVisible);
            rightPanel.setManaged(nextVisible);

            if (infoButton != null) {
                infoButton.setSelected(nextVisible);
            }

            // Ensure space is released/reclaimed
            if (nextVisible) {
                // Expanding
                rightPanel.setMinWidth(260);
                rightPanel.setMaxWidth(340);
                rightPanel.setPrefWidth(300);
            } else {
                // Collapsing
                rightPanel.setMinWidth(0);
                rightPanel.setMaxWidth(0);
                rightPanel.setPrefWidth(0);
            }

            if (nextVisible && getCurrentNote() != null) {
                updateNoteInfoPanel();
            }
        }
    }

    /**
     * Handle close right panel button.
     */
    @FXML
    private void handleCloseRightPanel(ActionEvent event) {
        if (rightPanel != null) {
            rightPanel.setVisible(false);
            rightPanel.setManaged(false);

            if (infoButton != null) {
                infoButton.setSelected(false);
            }
        }
    }

    /**
     * Legacy method - redirect to new handler.
     */
    @FXML
    private void handleShowNoteInfo(ActionEvent event) {
        handleToggleRightPanel(event);
    }

    /**
     * Initialize the collapsible sections in the right panel.
     */
    private void initializeRightPanelSections() {
        // Make Note Info section collapsible
        if (noteInfoHeader != null && noteInfoContent != null && noteInfoCollapseIcon != null) {
            noteInfoHeader.setOnMouseClicked(e -> {
                boolean isCollapsed = !noteInfoContent.isVisible();
                noteInfoContent.setVisible(isCollapsed);
                noteInfoContent.setManaged(isCollapsed);
                noteInfoCollapseIcon.setText(isCollapsed ? "▼" : "▶");
            });
            noteInfoHeader.setStyle("-fx-cursor: hand;");
        }

        // Plugin panels container should always be visible (content is added
        // dynamically)
        if (pluginPanelsContainer != null) {
            pluginPanelsContainer.setVisible(true);
            pluginPanelsContainer.setManaged(true);
        }
    }

    /**
     * Update the note info panel with current note data.
     */
    private void updateNoteInfoPanel() {
        if (getCurrentNote() == null)
            return;

        if (infoCreatedLabel != null && getCurrentNote().getCreatedDate() != null) {
            infoCreatedLabel.setText(getCurrentNote().getCreatedDate().toString());
        }
        if (infoModifiedLabel != null && getCurrentNote().getModifiedDate() != null) {
            infoModifiedLabel.setText(getCurrentNote().getModifiedDate().toString());
        }

        String content = noteContentArea.getText();
        if (infoWordsLabel != null) {
            int words = content == null || content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
            infoWordsLabel.setText(String.valueOf(words));
        }
        if (infoCharsLabel != null) {
            infoCharsLabel.setText(String.valueOf(content == null ? 0 : content.length()));
        }
        if (infoLatitudeLabel != null) {
            String latVal = getCurrentNote().getLatitude() != 0 ? String.valueOf(getCurrentNote().getLatitude()) : "-";
            infoLatitudeLabel.setText(java.text.MessageFormat.format(getString("info.lat"), latVal));
        }
        if (infoLongitudeLabel != null) {
            String lonVal = getCurrentNote().getLongitude() != 0 ? String.valueOf(getCurrentNote().getLongitude())
                    : "-";
            infoLongitudeLabel.setText(java.text.MessageFormat.format(getString("info.lon"), lonVal));
        }
        if (infoAuthorLabel != null) {
            String authorVal = (getCurrentNote().getAuthor() != null && !getCurrentNote().getAuthor().isEmpty())
                    ? getCurrentNote().getAuthor()
                    : "-";
            infoAuthorLabel.setText(java.text.MessageFormat.format(getString("info.author"), authorVal));
        }
        if (infoSourceUrlLabel != null) {
            String sourceVal = (getCurrentNote().getSourceUrl() != null && !getCurrentNote().getSourceUrl().isEmpty())
                    ? getCurrentNote().getSourceUrl()
                    : "-";
            infoSourceUrlLabel.setText(java.text.MessageFormat.format(getString("info.source"), sourceVal));
        }
    }

    /**
     * Load all notes via NotesListController.
     */
    private void loadAllNotes() {
        if (notesListController != null) {
            currentFolder = null;
            currentTag = null;
            currentFilterType = "all";
        }
    }

    /**
     * Load notes for selected folder via NotesListController, and update UI layout.
     */
    private void handleFolderSelection(Folder folder) {
        if (folderWorkflow == null) {
            folderWorkflow = new FolderWorkflow();
        }
        folderWorkflow.handleFolderSelection(folder, isStackedLayout, new FolderWorkflow.FolderSelectionPort() {
            @Override
            public void setCurrentFolder(Folder selectedFolder) {
                currentFolder = selectedFolder;
            }

            @Override
            public void clearCurrentTag() {
                currentTag = null;
            }

            @Override
            public void setCurrentFilterType(String filterType) {
                currentFilterType = filterType;
            }

            @Override
            public void loadNotesForFolder(Folder selectedFolder) {
                if (notesListController != null) {
                    notesListController.loadNotesForFolder(selectedFolder);
                }
            }

            @Override
            public boolean hasNotesPanel() {
                return notesPanel != null;
            }

            @Override
            public double getNotesPanelMaxWidth() {
                return notesPanel != null ? notesPanel.getMaxWidth() : 0;
            }

            @Override
            public void setNotesPanelMinWidth(double width) {
                if (notesPanel != null) {
                    notesPanel.setMinWidth(width);
                }
            }

            @Override
            public void setNotesPanelMaxWidth(double width) {
                if (notesPanel != null) {
                    notesPanel.setMaxWidth(width);
                }
            }

            @Override
            public double[] getNavDividerPositions() {
                return navSplitPane != null ? navSplitPane.getDividerPositions() : new double[0];
            }

            @Override
            public void setNavDividerPosition(double position) {
                if (navSplitPane != null) {
                    navSplitPane.setDividerPositions(position);
                }
            }

            @Override
            public void setContentDividerPosition(double position) {
                if (contentSplitPane != null) {
                    contentSplitPane.setDividerPositions(position);
                }
            }

            @Override
            public void setNotesPanelToggleSelected(boolean selected) {
                if (toolbarController != null && toolbarController.getNotesPanelToggleBtn() != null) {
                    toolbarController.getNotesPanelToggleBtn().setSelected(selected);
                }
            }
        });
    }

    /**
     * Load note in editor.
     */
    private void loadNoteInEditor(Note note) {
        if (isModified() && getCurrentNote() != null) {
            // Ask to save changes
            Optional<ButtonType> result = showSaveDialog();
            if (result.isPresent()) {
                if (result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                    return;
                }
                if (result.get().getText().equals(getString("action.save"))) {
                    handleSave(new ActionEvent());
                }
            }
        }

        if (editorController != null) {
            editorController.loadNote(note);
        }

        Note activeNote = getCurrentNote();
        if (activeNote == null) {
            if (tagsFlowPane != null) {
                tagsFlowPane.getChildren().clear();
            }
            if (modifiedDateLabel != null) {
                modifiedDateLabel.setText("");
            }
            if (previewWebView != null) {
                updatePreview();
            }
            updateStatus(getString("status.no_note_selected"));
            return;
        }

        // Load tags
        loadNoteTags(activeNote);

        // Update metadata
        updateNoteMetadata(activeNote);

        // Ensure WebView has correct background color based on theme
        if (previewWebView != null && !previewWebView.getStyleClass().contains("webview-theme")) {
            previewWebView.getStyleClass().add("webview-theme");
        }

        // Update preview
        updatePreview();

        // Update favorite button icon
        updateFavoriteButtonIcon();

        // Update pinned button icon
        updatePinnedButtonIcon();

        // Refresh favorites list to show current favorite status
        sidebarController.loadFavorites();

        updateStatus(java.text.MessageFormat.format(getString("status.note_loaded"), activeNote.getTitle()));
    }

    /**
     * Load tags for a note.
     */
    private void loadNoteTags(Note note) {
        if (noteWorkflow == null) {
            noteWorkflow = new NoteWorkflow(noteDAO);
        }
        noteWorkflow.loadNoteTags(note, tagsFlowPane, this::handleAddTagToNote, this::removeTagFromNote);
    }

    /**
     * Update note metadata display.
     */
    private void updateNoteMetadata(Note note) {
        if (noteWorkflow == null) {
            noteWorkflow = new NoteWorkflow(noteDAO);
        }
        noteWorkflow.updateNoteMetadata(
                note,
                modifiedDateLabel,
                infoCreatedLabel,
                infoModifiedLabel,
                infoWordsLabel,
                infoCharsLabel,
                infoLatitudeLabel,
                infoLongitudeLabel,
                infoAuthorLabel,
                infoSourceUrlLabel,
                this::getString);
    }

    /**
     * Load notes for a specific tag via NotesListController.
     */
    private void loadNotesForTag(String tagName) {
        if (tagWorkflow == null) {
            tagWorkflow = new TagWorkflow();
        }
        tagWorkflow.selectTagByTitle(tagName, new TagWorkflow.TagSelectionPort() {
            @Override
            public void setCurrentFolderToNull() {
                currentFolder = null;
            }

            @Override
            public void setCurrentTag(Tag selectedTag) {
                currentTag = selectedTag;
            }

            @Override
            public void setCurrentFilterType(String filterType) {
                currentFilterType = filterType;
            }

            @Override
            public void loadNotesForTag(String tagTitle) {
                if (notesListController != null) {
                    notesListController.loadNotesForTag(tagTitle);
                }
            }
        });
    }

    /**
     * Perform search via NotesListController.
     */
    private void performSearch(String searchText) {
        if (notesListController != null) {
            currentFilterType = "search";
            notesListController.performSearch(searchText);
        }
    }

    /**
     * Sort notes via NotesListController.
     */
    private void sortNotes(String sortOption) {
        if (notesListController != null) {
            notesListController.sortNotes(sortOption);
        }
    }

    /**
     * Update word count.
     */
    private void updateWordCount() {
        String content = noteContentArea != null ? noteContentArea.getText() : "";
        if (content == null)
            content = "";

        int wordCount = countWords(content);

        if (wordCountLabel != null) {
            wordCountLabel.setText(wordCount + " words");
        }
        if (infoWordsLabel != null) {
            infoWordsLabel.setText(String.valueOf(wordCount));
        }
        if (infoCharsLabel != null) {
            infoCharsLabel.setText(String.valueOf(content.length()));
        }
    }

    /**
     * Count words in text.
     */
    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * Update preview with syntax highlighting (highlight.js).
     */
    @Override
    public void registerPreviewEnhancer(String pluginId, PreviewEnhancer enhancer) {
        if (pluginId != null && enhancer != null) {
            previewEnhancers.put(pluginId, enhancer);
            Platform.runLater(this::updatePreview);
        }
    }

    @Override
    public void unregisterPreviewEnhancer(String pluginId) {
        if (pluginId != null) {
            previewEnhancers.remove(pluginId);
            Platform.runLater(this::updatePreview);
        }
    }

    private void updatePreview() {
        if (previewWebView == null || getCurrentNote() == null) {
            return;
        }
        if (previewWorkflow == null) {
            previewWorkflow = new PreviewWorkflow();
        }
        String content = noteContentArea != null ? noteContentArea.getText() : "";
        boolean isDarkTheme = "dark".equals(resolveThemeToApply());
        String html = (content != null && !content.trim().isEmpty())
                ? previewWorkflow.buildPreviewHtml(content, isDarkTheme, previewEnhancers.values())
                : previewWorkflow.buildEmptyHtml(isDarkTheme);
        previewWebView.getEngine().loadContent(html, "text/html");
    }

    /**
     * Update status bar.
     */
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Show save dialog.
     */
    private Optional<ButtonType> showSaveDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(getString("dialog.save_changes.title"));
        alert.setHeaderText(getString("dialog.save_changes.header"));
        alert.setContentText(getString("dialog.save_changes.content"));

        ButtonType saveButton = new ButtonType(getString("action.save"));
        ButtonType dontSaveButton = new ButtonType(getString("action.dont_save"));
        ButtonType cancelButton = new ButtonType(getString("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

        return alert.showAndWait();
    }

    /**
     * Add tag to current note.
     */
    @FXML
    private void handleAddTagToNote() {
        if (getCurrentNote() == null) {
            updateStatus(getString("status.no_note"));
            return;
        }

        try {
            // Get existing tags
            List<Tag> existingTags = tagService.getAllTags();
            List<Tag> noteTags = noteDAO.fetchTags(getCurrentNote().getId());

            // Filter out tags already assigned to the note
            List<String> availableTagNames = existingTags.stream()
                    .filter(tag -> !noteTags.stream().anyMatch(nt -> nt.getId().equals(tag.getId())))
                    .map(Tag::getTitle)
                    .sorted()
                    .toList();

            // Create dialog for adding tag
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle(getString("dialog.add_tag.title"));
            dialog.setHeaderText(availableTagNames.isEmpty()
                    ? getString("dialog.add_tag.header_new")
                    : getString("dialog.add_tag.header_select"));

            // Set buttons
            ButtonType addButtonType = new ButtonType(getString("action.add"), ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

            // Create layout
            VBox content = new VBox(10);
            ComboBox<String> tagComboBox = new ComboBox<>();
            tagComboBox.setEditable(true);
            tagComboBox.getItems().addAll(availableTagNames);
            tagComboBox.setPromptText(getString("dialog.add_tag.prompt"));
            tagComboBox.setPrefWidth(300);
            content.getChildren().add(new Label(getString("label.tag")));
            content.getChildren().add(tagComboBox);
            dialog.getDialogPane().setContent(content);

            // Convert result
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    return tagComboBox.getEditor().getText();
                }
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String tagName = result.get().trim();

                // Check if it's an existing tag
                Optional<Tag> existingTag = existingTags.stream()
                        .filter(t -> t.getTitle().equals(tagName))
                        .findFirst();

                Tag tag;
                if (existingTag.isPresent()) {
                    tag = existingTag.get();
                } else {
                    // Create new tag
                    tag = new Tag(tagName);
                    Tag createdTag = tagService.createTag(tag.getTitle());
                    tag.setId(createdTag.getId());
                }

                // Check if tag is already assigned to note (double check)
                List<Tag> currentNoteTags = noteDAO.fetchTags(getCurrentNote().getId());
                boolean alreadyHasTag = currentNoteTags.stream()
                        .anyMatch(t -> t.getId().equals(tag.getId()));

                if (alreadyHasTag) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(getString("dialog.tag_already_assigned.title"));
                    alert.setHeaderText(
                            java.text.MessageFormat.format(getString("dialog.tag_already_assigned.header"), tagName));
                    alert.showAndWait();
                } else {
                    noteDAO.addTag(getCurrentNote(), tag);
                    loadNoteTags(getCurrentNote());
                    // Update tags list in sidebar
                    sidebarController.loadTags();
                    updateStatus(java.text.MessageFormat.format(getString("status.tag_added"), tagName));
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to add tag: " + e.getMessage());
            updateStatus(getString("status.tag_add_error"));
        }
    }

    /**
     * Remove tag from note.
     */
    private void removeTagFromNote(Tag tag) {
        if (getCurrentNote() == null) {
            updateStatus(getString("status.no_note_selected"));
            return;
        }
        if (tag == null || tag.getId() == null) {
            updateStatus(getString("status.invalid_tag"));
            return;
        }

        // Confirm removal
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(getString("dialog.remove_tag.title"));
        confirm.setHeaderText(java.text.MessageFormat.format(getString("dialog.remove_tag.header"), tag.getTitle()));
        confirm.setContentText(getString("dialog.remove_tag.content"));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                noteDAO.removeTag(getCurrentNote(), tag);
                loadNoteTags(getCurrentNote());
                updateStatus(java.text.MessageFormat.format(getString("status.tag_removed"), tag.getTitle()));
            } catch (Exception e) {
                logger.severe("Failed to remove tag: " + e.getMessage());
                updateStatus(java.text.MessageFormat.format(getString("status.tag_remove_error"), e.getMessage()));
            }
        }
    }

    /**
     * Handle editor key press.
     */
    private void handleEditorKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.S && event.isControlDown()) {
            handleSave(new ActionEvent());
            event.consume();
        }
    }

    // ==================== MENU HANDLERS ====================

    @FXML
    private void handleNewNote(ActionEvent event) {
        try {
            Note newNote = new Note(getString("action.new_note"), "");

            // Set parent folder regardless of storage
            if (currentFolder != null && currentFolder.getId() != null &&
                    !"ROOT".equals(currentFolder.getId()) &&
                    !isAllNotesVirtualFolder(currentFolder)) {
                newNote.setParent(currentFolder);
            }

            // Fix: If a folder is selected, prepare the ID with the folder path
            // Data Access Logic: Only do this for FileSystem, SQLite handles ID generation
            Preferences prefs = Preferences.userNodeForPackage(MainController.class);
            boolean isFileSystem = !"sqlite".equals(prefs.get("storage_type", "sqlite"));

            if (isFileSystem && currentFolder != null && currentFolder.getId() != null &&
                    !"ROOT".equals(currentFolder.getId()) &&
                    !isAllNotesVirtualFolder(currentFolder)) {

                String pathSeparator = File.separator;
                String folderPath = currentFolder.getId();
                String safeTitle = newNote.getTitle().replaceAll("[^a-zA-Z0-9\\.\\-_ ]", "_");

                // We set an ID like "Folder/New Note" so NoteDAOFileSystem detects the parent
                // folder
                newNote.setId(folderPath + pathSeparator + safeTitle);
            }

            Note createdNote = noteService.createNote(newNote);
            String noteId = createdNote.getId();
            if (noteId == null) {
                // If creation failed, do not proceed
                updateStatus(getString("status.error_creating_note"));
                return;
            }
            newNote.setId(noteId);

            if (currentFolder != null && currentFolder.getId() != null &&
                    !"ROOT".equals(currentFolder.getId()) &&
                    !isAllNotesVirtualFolder(currentFolder)) {
                folderService.addNoteToFolder(currentFolder, newNote);
            }

            // With FS DAO and our fix, the file is already in the right place.
            // We don't need to manually add it to the Folder object's children list
            // because refreshNotesList() will re-fetch from disk correctly.

            notesListView.getItems().add(0, newNote);
            notesListView.getSelectionModel().select(newNote);
            loadNoteInEditor(newNote);

            // Refresh recent notes to include new note
            sidebarController.loadRecentNotes();

            // Refresh folder tree note counts and list visually
            folderTreeView.refresh();

            // Fire event for plugins
            if (eventBus != null) {
                eventBus.publish(new NoteEvents.NoteCreatedEvent(newNote));
            }

            updateStatus(getString("status.note_created"));
        } catch (Exception e) {
            logger.severe("Failed to create new note: " + e.getMessage());
            updateStatus(getString("status.error_creating_note"));
        }
    }

    @FXML
    private void handleNewFolder(ActionEvent event) {
        // Create dialog
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle(getString("dialog.new_folder.title"));

        // Determine if we should create in root or as subfolder
        boolean createInRoot = (currentFolder == null ||
                isAllNotesVirtualFolder(currentFolder));

        String headerText = createInRoot
                ? getString("dialog.new_folder.header_root")
                : java.text.MessageFormat.format(getString("dialog.new_folder.header_sub"), currentFolder.getTitle());

        dialog.setHeaderText(headerText);
        dialog.setContentText(getString("dialog.new_folder.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                Folder newFolder = new Folder(result.get().trim());

                // Only add as subfolder if currentFolder is set and not "All Notes"
                if (!createInRoot && currentFolder != null) {
                    newFolder.setParent(currentFolder);
                }

                String folderId = folderDAO.createFolder(newFolder);
                newFolder.setId(folderId);

                if (!createInRoot && currentFolder != null) {
                    folderDAO.addSubFolder(currentFolder, newFolder);
                }
                // Otherwise, it's created in root (parent_id will be NULL)

                // Select "All Notes" root to make it clear where new folders are created
                if (folderTreeView.getRoot() != null) {
                    folderTreeView.getSelectionModel().select(folderTreeView.getRoot());
                }
                currentFolder = null;
                updateStatus(java.text.MessageFormat.format(getString("status.folder_created"), newFolder.getTitle()));
            } catch (Exception e) {
                logger.severe("Failed to create folder: " + e.getMessage());
                    updateStatus(java.text.MessageFormat.format(getString("status.error_details"), e.getMessage()));
            }
        }
    }

    /**
     * Handle creating a new subfolder in the currently selected folder.
     */
    private void handleNewSubfolder(ActionEvent event) {
        if (currentFolder == null ||
                isAllNotesVirtualFolder(currentFolder)) {
            handleNewFolder(event);
            return;
        }

        TextInputDialog dialog = new TextInputDialog(getString("dialog.new_subfolder.default_name"));
        dialog.setTitle(getString("dialog.new_subfolder.title"));
        dialog.setHeaderText(
                java.text.MessageFormat.format(getString("dialog.new_subfolder.header"), currentFolder.getTitle()));
        dialog.setContentText(getString("dialog.new_subfolder.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                Folder newSubfolder = new Folder(result.get().trim());
                newSubfolder.setParent(currentFolder);

                String folderId = folderDAO.createFolder(newSubfolder);
                newSubfolder.setId(folderId);

                folderDAO.addSubFolder(currentFolder, newSubfolder);

                updateStatus(
                        java.text.MessageFormat.format(getString("status.subfolder_created"), newSubfolder.getTitle()));
            } catch (Exception e) {
                logger.severe("Failed to create subfolder: " + e.getMessage());
                updateStatus(getString("status.subfolder_error"));
            }
        }
    }

    @FXML
    private void handleImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(getString("dialog.import.title"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(getString("file_filter.supported"), "*.md", "*.txt", "*.markdown"),
                new FileChooser.ExtensionFilter(getString("file_filter.markdown"), "*.md", "*.markdown"),
                new FileChooser.ExtensionFilter(getString("file_filter.text"), "*.txt"),
                new FileChooser.ExtensionFilter(getString("file_filter.all"), "*.*"));

        List<File> files = fileChooser.showOpenMultipleDialog(mainSplitPane.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            int imported = 0;
            int failed = 0;

            for (File file : files) {
                try {
                    String content = Files.readString(file.toPath());
                    String title = file.getName();
                    // Remove extension from title
                    int dotIndex = title.lastIndexOf('.');
                    if (dotIndex > 0) {
                        title = title.substring(0, dotIndex);
                    }

                    // Create new note
                    Note newNote = new Note(title, content);
                    String safeTitle = title.replaceAll("[^a-zA-Z0-9\\.\\-_ ]", "_");
                    Preferences prefs = Preferences.userNodeForPackage(MainController.class);
                    boolean isFileSystem = !"sqlite".equals(prefs.get("storage_type", "sqlite"));

                    if (isFileSystem && currentFolder != null && currentFolder.getId() != null &&
                            !"ROOT".equals(currentFolder.getId()) &&
                            !isAllNotesVirtualFolder(currentFolder)) {
                        String pathSeparator = File.separator;
                        String folderPath = currentFolder.getId();
                        newNote.setId(folderPath + pathSeparator + safeTitle);
                    }

                    Note createdNote = noteService.createNote(newNote);
                    newNote.setId(createdNote.getId());

                    if (!isFileSystem && currentFolder != null && currentFolder.getId() != null &&
                            !"ROOT".equals(currentFolder.getId())) {
                        // For non-filesystem storage (SQLite), createNote produces the ID safely
                        // Then we add it to the folder. Since SQLite doesn't use path folders on disk.
                        folderService.addNoteToFolder(currentFolder, createdNote);
                    }

                    imported++;
                } catch (Exception e) {
                    logger.warning("Failed to import file " + file.getName() + ": " + e.getMessage());
                    failed++;
                }
            }

            // Refresh lists
            refreshNotesList();
            sidebarController.loadRecentNotes();

            // Show result
            String message = java.text.MessageFormat.format(getString("status.imported_notes"), imported);
            if (failed > 0) {
                message += "\n" + java.text.MessageFormat.format(getString("status.import_failed_count"), failed);
            }
            updateStatus(message);
            showAlert(Alert.AlertType.INFORMATION, getString("status.import_complete"),
                    getString("dialog.import_finished"), message);
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (editorController != null) {
            editorController.handleSave();
        }
        refreshNotesList();
        if (sidebarController != null) {
            sidebarController.loadRecentNotes();
            sidebarController.loadFavorites();
        }
    }

    /**
     * Refresh the notes list to reflect current state.
     */
    private void refreshNotesList() {
        if (noteWorkflow == null) {
            noteWorkflow = new NoteWorkflow(noteDAO);
        }
        String searchText = toolbarController != null && toolbarController.getSearchField() != null
                ? toolbarController.getSearchField().getText()
                : "";
        noteWorkflow.refreshNotesList(
                currentFilterType,
                currentFolder,
                currentTag,
                searchText,
                currentNotesViewMode == NotesViewMode.GRID,
                new NoteWorkflow.NotesListPort() {
                    @Override
                    public void loadAllNotes() {
                        if (notesListController != null) {
                            notesListController.loadAllNotes();
                        }
                    }

                    @Override
                    public void loadNotesForFolder(Folder folder) {
                        if (notesListController != null) {
                            notesListController.loadNotesForFolder(folder);
                        }
                    }

                    @Override
                    public void loadNotesForTag(String tagTitle) {
                        if (notesListController != null) {
                            notesListController.loadNotesForTag(tagTitle);
                        }
                    }

                    @Override
                    public void performSearch(String query) {
                        if (notesListController != null) {
                            notesListController.performSearch(query);
                        }
                    }
                },
                () -> {
                    if (sidebarController != null) {
                        sidebarController.loadFavorites();
                    }
                },
                this::refreshGridView);
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        if (eventBus != null) {
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.DELETE));
        }
    }

    @FXML
    private void handleExit(ActionEvent event) {
        if (isModified() && getCurrentNote() != null) {
            Optional<ButtonType> result = showSaveDialog();
            if (result.isPresent()) {
                if (result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                    return;
                }
                if (result.get().getText().equals(getString("action.save"))) {
                    handleSave(event);
                }
            }
        }

        shutdownApplication();
        Platform.exit();
        System.exit(0);
    }

    /**
     * Gracefully shuts down runtime resources.
     */
    public void shutdownApplication() {
        try {
            if (pluginManager != null) {
                pluginManager.shutdownAll();
            }
            com.example.forevernote.plugin.PluginLoader.closeAllClassLoaders();

            if (connection != null && !connection.isClosed()) {
                SQLiteDB db = SQLiteDB.getInstance();
                db.closeConnection(connection);
            }
        } catch (Exception e) {
            logger.warning("Error during shutdown: " + e.getMessage());
        }
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (getCurrentNote() == null) {
            showAlert(Alert.AlertType.WARNING, getString("dialog.export.title"),
                    getString("dialog.export.no_note_header"), getString("dialog.export.no_note_content"));
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(getString("dialog.export.save_title"));
        fileChooser.setInitialFileName(sanitizeFileName(getCurrentNote().getTitle()));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(getString("file_filter.markdown"), "*.md"),
                new FileChooser.ExtensionFilter(getString("file_filter.text"), "*.txt"),
                new FileChooser.ExtensionFilter(getString("file_filter.all"), "*.*"));

        File file = fileChooser.showSaveDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Add title as header for Markdown
                if (file.getName().endsWith(".md")) {
                    writer.write("# " + getCurrentNote().getTitle() + "\n\n");
                }
                writer.write(getCurrentNote().getContent() != null ? getCurrentNote().getContent() : "");
                updateStatus(java.text.MessageFormat.format(getString("status.exported"), file.getName()));
                showAlert(Alert.AlertType.INFORMATION, getString("status.export_success"),
                        getString("dialog.export.success_header"),
                        java.text.MessageFormat.format(getString("dialog.export.saved_to"), file.getAbsolutePath()));
            } catch (IOException e) {
                logger.severe("Failed to export note: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, getString("status.export_failed"),
                        getString("dialog.export.failed_header"), e.getMessage());
            }
        }
    }

    /**
     * Sanitize filename for safe file system use.
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "untitled";
        }
        return name.replaceAll("[^a-zA-Z0-9\\-_ ]", "_").substring(0, Math.min(name.length(), 50));
    }

    /**
     * Show a simple alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleUndo(ActionEvent event) {
        if (noteContentArea != null) {
            noteContentArea.undo();
        }
    }

    @FXML
    private void handleRedo(ActionEvent event) {
        // JavaFX TextArea doesn't have redo by default
        updateStatus(getString("status.redo_not_available"));
    }

    @FXML
    private void handleCut(ActionEvent event) {
        if (noteContentArea != null && noteContentArea.getSelectedText() != null) {
            noteContentArea.cut();
        } else if (noteTitleField != null && noteTitleField.getSelectedText() != null) {
            noteTitleField.cut();
        }
    }

    @FXML
    private void handleCopy(ActionEvent event) {
        if (noteContentArea != null && noteContentArea.getSelectedText() != null) {
            noteContentArea.copy();
        } else if (noteTitleField != null && noteTitleField.getSelectedText() != null) {
            noteTitleField.copy();
        }
    }

    @FXML
    private void handlePaste(ActionEvent event) {
        if (noteContentArea != null && noteContentArea.isFocused()) {
            noteContentArea.paste();
        } else if (noteTitleField != null && noteTitleField.isFocused()) {
            noteTitleField.paste();
        }
    }

    @FXML
    private void handleFind(ActionEvent event) {
        if (noteContentArea != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle(getString("dialog.find.title"));
            dialog.setHeaderText(getString("dialog.find.header"));
            dialog.setContentText(getString("dialog.find.content"));

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String searchText = result.get().trim();
                String content = noteContentArea.getText();
                int index = content.indexOf(searchText);
                if (index >= 0) {
                    noteContentArea.selectRange(index, index + searchText.length());
                    noteContentArea.requestFocus();
                    updateStatus(java.text.MessageFormat.format(getString("status.found_text"), searchText));
                } else {
                    updateStatus(java.text.MessageFormat.format(getString("status.text_not_found"), searchText));
                }
            }
        }
    }

    @FXML
    private void handleReplace(ActionEvent event) {
        if (noteContentArea == null) {
            updateStatus(getString("status.no_note_open"));
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(getString("dialog.replace.title"));
        dialog.setHeaderText(getString("dialog.replace.header"));

        ButtonType replaceButton = new ButtonType(getString("action.replace_one"), ButtonBar.ButtonData.OK_DONE);
        ButtonType replaceAllButton = new ButtonType(getString("action.replace_all"), ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(replaceButton, replaceAllButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        TextField findField = new TextField();
        findField.setPromptText(getString("dialog.replace.find_prompt"));
        TextField replaceField = new TextField();
        replaceField.setPromptText(getString("dialog.replace.with_prompt"));

        content.getChildren().addAll(
                new Label(getString("dialog.replace.find_label")), findField,
                new Label(getString("dialog.replace.with_label")), replaceField);

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == replaceButton || buttonType == replaceAllButton) {
                return findField.getText() + "|" + replaceField.getText() + "|" +
                        (buttonType == replaceAllButton ? "all" : "one");
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String[] parts = result.get().split("\\|");
            if (parts.length == 3) {
                String find = parts[0];
                String replace = parts[1];
                boolean replaceAll = parts[2].equals("all");

                String noteContent = noteContentArea.getText();
                if (replaceAll) {
                    String newContent = noteContent.replace(find, replace);
                    noteContentArea.setText(newContent);
                    updateStatus(getString("status.replaced_all"));
                } else {
                    int index = noteContent.indexOf(find);
                    if (index >= 0) {
                        String newContent = noteContent.substring(0, index) + replace +
                                noteContent.substring(index + find.length());
                        noteContentArea.setText(newContent);
                        noteContentArea.selectRange(index, index + replace.length());
                        updateStatus(getString("status.replaced_first"));
                    } else {
                        updateStatus(getString("status.text_not_found_general"));
                    }
                }

            }
        }
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        if (sidebarPane != null) {
            if (isStackedLayout) {
                // In stacked layout, "Toggle Sidebar" button actually toggles the entire Left
                // Navigation Panel (Sidebar + Notes)
                if (navSplitPane != null) {
                    boolean isCollapsed = navSplitPane.getMaxWidth() < 10;

                    if (isCollapsed) {
                        // Expand: restore navSplitPane size
                        navSplitPane.setMinWidth(200);
                        navSplitPane.setMaxWidth(Double.MAX_VALUE);
                        navSplitPane.setPrefWidth(300);
                        mainSplitPane.setDividerPositions(0.25);
                        updateStatus(getString("status.nav_shown"));
                        if (toolbarController != null && toolbarController.getSidebarToggleBtn() != null)
                            toolbarController.getSidebarToggleBtn().setSelected(true);
                    } else {
                        // Collapse: hide navSplitPane completely
                        navSplitPane.setMinWidth(0);
                        navSplitPane.setMaxWidth(0);
                        navSplitPane.setPrefWidth(0);
                        updateStatus(getString("status.nav_hidden"));
                        if (toolbarController != null && toolbarController.getSidebarToggleBtn() != null)
                            toolbarController.getSidebarToggleBtn().setSelected(false);
                    }
                }
            } else {
                // Default Mode: Toggle sidebar normally
                boolean isCollapsed = sidebarPane.getMaxWidth() < 10;

                if (isCollapsed) {
                    // Expand
                    sidebarPane.setMinWidth(200);
                    sidebarPane.setMaxWidth(Double.MAX_VALUE);
                    sidebarPane.setPrefWidth(250);
                    mainSplitPane.setDividerPositions(0.22);
                    updateStatus(getString("status.sidebar_shown"));
                    if (toolbarController != null && toolbarController.getSidebarToggleBtn() != null)
                        toolbarController.getSidebarToggleBtn().setSelected(true);
                } else {
                    // Collapse
                    sidebarPane.setMinWidth(0);
                    sidebarPane.setMaxWidth(0);
                    sidebarPane.setPrefWidth(0);
                    updateStatus(getString("status.sidebar_hidden"));
                    if (toolbarController != null && toolbarController.getSidebarToggleBtn() != null)
                        toolbarController.getSidebarToggleBtn().setSelected(false);
                }
            }
        }
    }

    @FXML
    private void handleToggleNotesPanel(ActionEvent event) {
        if (isStackedLayout) {
            // In stacked layout, Notes Toggle behaves like Sidebar Toggle (toggles entire
            // stack)
            handleToggleSidebar(event);
            return;
        }

        if (notesPanel != null) {
            boolean isCollapsed = notesPanel.getMaxWidth() < 10;

            if (isCollapsed) {
                // Expand
                notesPanel.setMinWidth(180);
                notesPanel.setMaxWidth(Double.MAX_VALUE);
                notesPanel.setPrefWidth(280);

                if (contentSplitPane != null) {
                    contentSplitPane.setDividerPositions(0.25);
                }
                updateStatus(getString("status.notes_panel_shown"));
                if (toolbarController != null && toolbarController.getNotesPanelToggleBtn() != null)
                    toolbarController.getNotesPanelToggleBtn().setSelected(true);
            } else {
                // Collapse
                notesPanel.setMinWidth(0);
                notesPanel.setMaxWidth(0);
                notesPanel.setPrefWidth(0);
                updateStatus(getString("status.notes_panel_hidden"));
                if (toolbarController != null && toolbarController.getNotesPanelToggleBtn() != null)
                    toolbarController.getNotesPanelToggleBtn().setSelected(false);
            }
        }
    }

    @FXML
    private void handleZoomIn(ActionEvent event) {
        uiFontSize += 1.0;
        applyUiZoom();
    }

    @FXML
    private void handleZoomOut(ActionEvent event) {
        if (uiFontSize > 8.0) {
            uiFontSize -= 1.0;
            applyUiZoom();
        }
    }

    @FXML
    private void handleResetZoom(ActionEvent event) {
        uiFontSize = 13.0; // Default
        applyUiZoom();
    }

    private void applyUiZoom() {
        if (toolbarController != null && toolbarController.getToolbarHBox() != null
                && toolbarController.getToolbarHBox().getScene() != null) {
            toolbarController.getToolbarHBox().getScene().getRoot().setStyle("-fx-font-size: " + uiFontSize + "px;");
        }
    }

    @FXML
    private void handleEditorZoomIn(ActionEvent event) {
        editorFontSize += 1.0;
        applyEditorZoom();
    }

    @FXML
    private void handleEditorZoomOut(ActionEvent event) {
        if (editorFontSize > 8.0) {
            editorFontSize -= 1.0;
            applyEditorZoom();
        }
    }

    @FXML
    private void handleEditorResetZoom(ActionEvent event) {
        editorFontSize = 14.0; // Default
        applyEditorZoom();
    }

    private void applyEditorZoom() {
        if (noteContentArea != null) {
            noteContentArea.setStyle("-fx-font-size: " + editorFontSize + "px;");
        }
        if (noteTitleField != null) {
            noteTitleField.setStyle("-fx-font-size: " + (editorFontSize + 2) + "px;");
        }
    }

    @FXML
    private void handleViewLayoutSwitch(ActionEvent event) {
        isStackedLayout = !isStackedLayout;

        // Clean up current layout
        mainSplitPane.getItems().clear();
        contentSplitPane.getItems().clear();
        navSplitPane.getItems().clear(); // Safe clear

        // Reset sizes securely
        sidebarPane.setMinWidth(200);
        sidebarPane.setMaxWidth(Double.MAX_VALUE);
        notesPanel.setMinWidth(180);
        notesPanel.setMaxWidth(Double.MAX_VALUE);

        if (isStackedLayout) {
            // Stacked Mode: [NavSplit(Sidebar/Notes)] | [EditorContainer]
            if (navSplitPane == null)
                navSplitPane = new SplitPane(); // Safety check
            navSplitPane.getItems().clear();
            navSplitPane.getItems().addAll(sidebarPane, notesPanel);
            navSplitPane.setDividerPositions(0.5);

            mainSplitPane.getItems().addAll(navSplitPane, editorContainer);
            mainSplitPane.setDividerPositions(0.25);

            updateStatus(getString("status.layout_stacked"));
        } else {
            // Default Mode: [Sidebar] | [ContentSplit(Notes|EditorContainer)]
            // Note: editorContainer is inside contentSplitPane in default view
            contentSplitPane.getItems().addAll(notesPanel, editorContainer);
            contentSplitPane.setDividerPositions(0.3);

            mainSplitPane.getItems().addAll(sidebarPane, contentSplitPane);
            mainSplitPane.setDividerPositions(0.22);

            updateStatus(getString("status.layout_column"));
        }

        // Synch toggle buttons
        if (toolbarController != null && toolbarController.getSidebarToggleBtn() != null)
            toolbarController.getSidebarToggleBtn().setSelected(true);
        if (toolbarController != null && toolbarController.getNotesPanelToggleBtn() != null)
            toolbarController.getNotesPanelToggleBtn().setSelected(true);
    }

    @FXML
    private void handleToggleTags(ActionEvent event) {
        // Determine target visibility
        boolean show = true;

        if (toggleTagsBtn != null) {
            // If button exists, follow its state
            show = toggleTagsBtn.isSelected();
        } else if (tagsContainer != null) {
            // Toggle current state
            show = !tagsContainer.isVisible();
        }

        if (tagsContainer != null) {
            tagsContainer.setVisible(show);
            tagsContainer.setManaged(show);
            updateStatus(show ? getString("status.tags_bar_shown") : getString("status.tags_bar_hidden"));
        }
    }

    // Preferences for persistence
    private static final Preferences prefs = Preferences.userNodeForPackage(MainController.class);

    private String currentTheme = prefs.get("theme", "light"); // Load from preferences

    @FXML
    private void handleLightTheme(ActionEvent event) {
        currentTheme = "light";
        prefs.put("theme", currentTheme); // Save preference
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(getString("status.theme_light"));
    }

    @FXML
    private void handleDarkTheme(ActionEvent event) {
        currentTheme = "dark";
        prefs.put("theme", currentTheme); // Save preference
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(getString("status.theme_dark"));
    }

    @FXML
    private void handleSystemTheme(ActionEvent event) {
        currentTheme = "system";
        prefs.put("theme", currentTheme); // Save preference
        // Detect system theme using system properties
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isSystemDark = false;

        // Try to detect system theme based on OS
        if (osName.contains("win")) {
            // Windows: Detection requires JNA to read registry
            // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize\AppsUseLightTheme
            isSystemDark = detectWindowsTheme();
        } else if (osName.contains("mac")) {
            // macOS: Check system preference
            try {
                Process process = Runtime.getRuntime().exec("defaults read -g AppleInterfaceStyle");
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                isSystemDark = "Dark".equals(line);
                process.waitFor();
            } catch (Exception e) {
                logger.warning("Could not detect macOS theme: " + e.getMessage());
            }
        } else {
            // Linux: Check GTK theme or other methods
            // For now, default to light
            isSystemDark = false;
        }

        // Apply the detected system theme
        String actualTheme = isSystemDark ? "dark" : "light";
        currentTheme = "system"; // Keep track that we're in system mode
        updateThemeMenuSelection();

        // Temporarily set to detected theme for applyTheme()
        String previousTheme = currentTheme;
        currentTheme = actualTheme;
        applyTheme();
        currentTheme = previousTheme; // Restore system mode

        updateStatus(java.text.MessageFormat.format(getString("status.theme_system"), actualTheme));
    }

    /**
     * Detect Windows theme (simplified approach).
     * In production, use JNA or similar library for better detection.
     */
    private boolean detectWindowsTheme() {
        // Windows theme detection is complex without JNA.
        // For a robust solution, read registry:
        // HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize\AppsUseLightTheme
        // For now, default to light theme
        return false;
    }

    private void applyTheme() {
        if (mainSplitPane == null) {
            logger.warning("Cannot apply theme: mainSplitPane is null");
            return;
        }

        javafx.scene.Scene scene = mainSplitPane.getScene();
        if (scene == null) {
            logger.warning("Cannot apply theme: scene is null");
            return;
        }

        // Remove existing theme stylesheets
        scene.getStylesheets().removeIf(stylesheet -> stylesheet.contains("modern-theme.css") ||
                stylesheet.contains("dark-theme.css"));

        // Determine actual theme to use (especially for 'system' mode)
        String themeToApply = resolveThemeToApply();

        // Add the appropriate theme stylesheet
        java.net.URL themeResource;
        if ("dark".equalsIgnoreCase(themeToApply)) {
            themeResource = getClass().getResource("/com/example/forevernote/ui/css/dark-theme.css");
        } else {
            themeResource = getClass().getResource("/com/example/forevernote/ui/css/modern-theme.css");
        }

        if (themeResource != null) {
            scene.getStylesheets().add(themeResource.toExternalForm());
            logger.info("Theme changed to: " + currentTheme + " (Applied: " + themeToApply + ")");

            // Ensure WebView has correct background color
            if (previewWebView != null) {
                if (!previewWebView.getStyleClass().contains("webview-theme")) {
                    previewWebView.getStyleClass().add("webview-theme");
                }

                // Still set background via JavaScript to ensure it's applied to the body
                String bgColor = "dark".equalsIgnoreCase(themeToApply) ? "#1E1E1E" : "#FFFFFF";
                previewWebView.getEngine().executeScript(
                        "document.body.style.backgroundColor = '" + bgColor + "';");
            }

            // Update preview to reflect theme change
            if (getCurrentNote() != null) {
                updatePreview();
            }
        } else {
            logger.warning("Could not load theme stylesheet for: " + currentTheme);
        }
    }

    /**
     * Detect system theme.
     */
    private String detectSystemTheme() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isSystemDark = false;

        if (osName.contains("win")) {
            isSystemDark = detectWindowsTheme();
        } else if (osName.contains("mac")) {
            try {
                Process process = Runtime.getRuntime().exec("defaults read -g AppleInterfaceStyle");
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                isSystemDark = "Dark".equals(line);
                process.waitFor();
            } catch (Exception e) {
                logger.warning("Could not detect macOS theme: " + e.getMessage());
            }
        }

        return isSystemDark ? "dark" : "light";
    }

    private String resolveThemeToApply() {
        if (themeWorkflow == null) {
            themeWorkflow = new ThemeWorkflow();
        }
        return themeWorkflow.resolveThemeToApply(currentTheme, this::detectSystemTheme);
    }

    private boolean isDarkThemeActive() {
        if (themeWorkflow == null) {
            themeWorkflow = new ThemeWorkflow();
        }
        return themeWorkflow.isDarkTheme(currentTheme, this::detectSystemTheme);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        if (toolbarController != null && toolbarController.getSearchField() != null) {
            toolbarController.getSearchField().requestFocus();
            toolbarController.getSearchField().selectAll();
            updateStatus(getString("status.search_focused"));
        }
    }

    @FXML
    private void handleTagsManager(ActionEvent event) {
        try {
            List<Tag> allTags = tagService.getAllTags();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(getString("dialog.tags_manager.title"));
            dialog.setHeaderText(getString("dialog.tags_manager.header"));

            ButtonType closeButton = new ButtonType(getString("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);

            VBox content = new VBox(10);
            content.setPadding(new javafx.geometry.Insets(20));

            ListView<Tag> tagListView = new ListView<>();
            tagListView.getItems().addAll(allTags);
            tagListView.setCellFactory(lv -> new ListCell<Tag>() {
                @Override
                protected void updateItem(Tag tag, boolean empty) {
                    super.updateItem(tag, empty);
                    if (empty || tag == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        HBox hbox = new HBox(10);
                        Label nameLabel = new Label(tag.getTitle());
                        nameLabel.setPrefWidth(200);
                        Label dateLabel = new Label(
                                tag.getCreatedDate() != null ? tag.getCreatedDate() : getString("label.not_available"));
                        dateLabel.setStyle("-fx-text-fill: gray;");

                        Button deleteButton = new Button(getString("action.delete"));
                        deleteButton.setOnAction(e -> {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle(getString("dialog.delete_tag.title"));
                            confirm.setHeaderText(getString("dialog.tags_manager.delete_header"));
                            confirm.setContentText(getString("dialog.tags_manager.delete_content"));
                            Optional<ButtonType> result = confirm.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                try {
                                    tagService.deleteTag(tag.getId());
                                    tagListView.getItems().remove(tag);
                                    sidebarController.loadTags(); // Refresh sidebar
                                    updateStatus(java.text.MessageFormat.format(getString("status.tag_deleted"),
                                            tag.getTitle()));
                                } catch (Exception ex) {
                                    logger.severe("Failed to delete tag: " + ex.getMessage());
                                }
                            }
                        });

                        hbox.getChildren().addAll(nameLabel, dateLabel, deleteButton);
                        setGraphic(hbox);
                    }
                }
            });

            content.getChildren().add(new Label(
                    java.text.MessageFormat.format(getString("dialog.tags_manager.all_tags_count"), allTags.size())));
            content.getChildren().add(tagListView);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefSize(500, 400);

            dialog.showAndWait();
        } catch (Exception e) {
            logger.severe("Failed to open tags manager: " + e.getMessage());
            updateStatus(getString("status.tags_manager_error"));
        }
    }

    @FXML
    private void handlePreferences(ActionEvent event) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(getString("dialog.preferences.title"));
        dialog.setHeaderText(getString("dialog.preferences.header"));

        ButtonType closeButton = new ButtonType(getString("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        // Database location
        Label dbLabel = new Label(getString("dialog.preferences.db_location"));
        Label dbPathLabel = new Label("Forevernote/data/database.db");
        dbPathLabel.setStyle("-fx-text-fill: gray;");

        // Auto-save option (placeholder)
        Label autoSaveLabel = new Label(getString("dialog.preferences.autosave_placeholder"));
        autoSaveLabel.setStyle("-fx-text-fill: gray;");

        content.getChildren().addAll(
                new Label(getString("dialog.preferences.general_settings")),
                dbLabel, dbPathLabel,
                new Separator(),
                autoSaveLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(400, 300);

        dialog.showAndWait();
    }

    @FXML
    private void handleDocumentation(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(getString("dialog.documentation.title"));
        alert.setHeaderText(getString("dialog.documentation.header"));
        alert.setContentText(getString("dialog.documentation.content"));
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500, 400);
        alert.showAndWait();
    }

    @FXML
    private void handleKeyboardShortcuts(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(getString("dialog.shortcuts.title"));
        alert.setHeaderText(getString("dialog.shortcuts.header"));
        alert.setContentText(getString("dialog.shortcuts.content"));
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(450, 500);
        alert.showAndWait();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(getString("dialog.about.title"));

        ButtonType closeButton = new ButtonType(getString("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox content = new VBox(16);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setAlignment(javafx.geometry.Pos.CENTER);

        // App icon and name
        Label titleLabel = new Label(getString("about.app_name"));
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label versionLabel = new Label("Version " + com.example.forevernote.AppConfig.getAppVersion());
        versionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");

        Label descLabel = new Label(com.example.forevernote.AppConfig.getAppDescription());
        descLabel.setStyle("-fx-font-size: 13px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(350);
        descLabel.setAlignment(javafx.geometry.Pos.CENTER);

        // Separator
        Separator separator = new Separator();
        separator.setPrefWidth(300);

        // Tech stack
        Label techLabel = new Label(getString("about.tech_stack"));
        techLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        // Copyright
        Label copyrightLabel = new Label(com.example.forevernote.AppConfig.getAppCopyright());
        copyrightLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        // Developer credit
        Label developerLabel = new Label(getString("about.developer_credit"));
        developerLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        content.getChildren().addAll(
                titleLabel, versionLabel, descLabel,
                separator,
                techLabel, copyrightLabel, developerLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(400, 320);

        dialog.showAndWait();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        // Refresh based on current context
        try {
            switch (currentFilterType) {
                case "folder":
                    if (currentFolder != null) {
                        handleFolderSelection(currentFolder);
                    } else {
                    }
                    break;
                case "tag":
                    if (currentTag != null) {
                        loadNotesForTag(currentTag.getTitle());
                    } else {
                    }
                    break;
                case "favorites":
                    // Load favorites
                    List<Note> allNotes = noteService.getAllNotes();
                    List<Note> favoriteNotes = allNotes.stream()
                            .filter(Note::isFavorite)
                            .toList();
                    notesListView.getSelectionModel().clearSelection();
                    notesListView.getItems().setAll(favoriteNotes);
                    sortNotes(sortComboBox.getValue());
                    noteCountLabel.setText(favoriteNotes.size() + " favorite notes");
                    currentFilterType = "favorites";
                    currentFolder = null;
                    currentTag = null;
                    updateStatus(getString("status.favs_refreshed"));
                    break;
                case "search":
                    // Re-execute current search
                    String searchText = toolbarController != null && toolbarController.getSearchField() != null
                            ? toolbarController.getSearchField().getText()
                            : "";
                    if (searchText != null && !searchText.trim().isEmpty()) {
                        performSearch(searchText);
                    } else {
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.severe("Failed to refresh: " + e.getMessage());
            updateStatus(getString("status.refresh_error"));
        }
    }

    @FXML
    private void handleToggleFavorite(ActionEvent event) {
        if (getCurrentNote() == null) {
            updateStatus(getString("status.no_note"));
            return;
        }
        toggleFavorite(getCurrentNote());
    }

    /**
     * Toggle favorite status of a specific note.
     */
    private void toggleFavorite(Note note) {
        if (note == null)
            return;

        try {
            // Toggle favorite status
            boolean newFavoriteStatus = !note.isFavorite();
            note.setFavorite(newFavoriteStatus);

            // Save to database
            noteService.updateNote(note);

            // Update UI if this is the current note
            if (getCurrentNote() != null && getCurrentNote().getId().equals(note.getId())) {
                updateFavoriteButtonIcon();
            }

            // Refresh lists
            refreshNotesList();
            sidebarController.loadFavorites();

            updateStatus(newFavoriteStatus ? "Note marked as favorite" : "Note unmarked as favorite");
        } catch (Exception e) {
            logger.severe("Failed to toggle favorite: " + e.getMessage());
            updateStatus(getString("status.fav_error"));
        }
    }

    @FXML
    private void handleTogglePin(ActionEvent event) {
        if (getCurrentNote() == null) {
            updateStatus(getString("status.no_note"));
            return;
        }
        togglePin(getCurrentNote());
    }

    private void togglePin(Note note) {
        if (note == null)
            return;

        try {
            boolean newPinStatus = !note.isPinned();
            note.setPinned(newPinStatus);

            noteService.updateNote(note);

            if (getCurrentNote() != null && getCurrentNote().getId().equals(note.getId())) {
                updatePinnedButtonIcon();
            }

            refreshNotesList();
            updateStatus(newPinStatus ? "Note pinned" : "Note unpinned");
        } catch (Exception e) {
            logger.severe("Failed to toggle pin: " + e.getMessage());
            updateStatus(getString("status.pin_error"));
        }
    }

    private void updatePinnedButtonIcon() {
        if (pinButton == null)
            return;

        if (getCurrentNote() != null && getCurrentNote().isPinned()) {
            pinButton.setSelected(true);
            pinButton.setTooltip(new Tooltip("Unpin note (current state: Pinned)"));
        } else {
            pinButton.setSelected(false);
            pinButton.setTooltip(new Tooltip("Pin note"));
        }
    }

    /**
     * Update the favorite button icon based on current note's favorite status.
     */
    private void updateFavoriteButtonIcon() {
        if (favoriteButton != null && getCurrentNote() != null) {
            boolean isFav = getCurrentNote().isFavorite();
            favoriteButton.setSelected(isFav);
            if (favoriteButton.getTooltip() != null) {
                favoriteButton.getTooltip()
                        .setText(isFav ? getString("action.remove_favorite") : getString("action.add_favorite"));
            }
        }
    }

    @FXML
    private void handleNewTag(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(getString("dialog.new_tag.title"));
        dialog.setHeaderText(getString("dialog.new_tag.header"));
        dialog.setContentText(getString("dialog.new_tag.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                String tagName = result.get().trim();
                if (tagService.tagExists(tagName)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(getString("dialog.tag_exists.title"));
                    alert.setHeaderText(getString("dialog.tag_exists.header"));
                    alert.setContentText(getString("dialog.tag_exists.content"));
                    alert.showAndWait();
                } else {
                    Tag newTag = new Tag(tagName);
                    Tag createdTag = tagService.createTag(newTag.getTitle());
                    newTag.setId(createdTag.getId());
                    sidebarController.loadTags(); // Refresh tag list
                    updateStatus(java.text.MessageFormat.format(getString("status.tag_created"), tagName));
                }
            } catch (Exception e) {
                logger.severe("Failed to create tag: " + e.getMessage());
                updateStatus(getString("status.error") + ": " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleBold(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.BOLD));
    }

    @FXML
    private void handleItalic(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.ITALIC));
    }

    @FXML
    private void handleUnderline(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.STRIKE));
    }

    @FXML
    private void handleLink(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.LINK));
    }

    @FXML
    private void handleImage(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.IMAGE));
    }

    @FXML
    private void handleAttachment(ActionEvent event) {
        updateStatus(getString("status.attachments_not_supported"));
    }

    @FXML
    private void handleTodoList(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.TODO_LIST));
    }

    @FXML
    private void handleNumberedList(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.NUMBERED_LIST));
    }

    @FXML
    private void handleSaveAll(ActionEvent event) {
        if (editorController != null)
            editorController.handleSave();
        updateStatus(getString("status.saved_all"));
    }

    @FXML
    private void handleHeading1(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.HEADING1));
    }

    @FXML
    private void handleHeading2(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.HEADING2));
    }

    @FXML
    private void handleBulletList(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.BULLET_LIST));
    }

    @FXML
    private void handleCode(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.CODE));
    }

    @FXML
    private void handleQuote(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.QUOTE));
    }

    @FXML
    private void handleHeading3(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.HEADING3));
    }

    @FXML
    private void handleRealUnderline(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.UNDERLINE));
    }

    @FXML
    private void handleHighlight(ActionEvent event) {
        if (eventBus != null)
            eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.HIGHLIGHT));
    }

    /**
     * Delete a tag.
     */
    private void handleDeleteTag(Tag tag) {
        if (tag == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(getString("dialog.delete_tag.title"));
        alert.setHeaderText(getString("dialog.delete_tag.header"));
        alert.setContentText(java.text.MessageFormat.format(getString("dialog.delete_tag.content"), tag.getTitle()));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                tagService.deleteTag(tag.getId());
                sidebarController.loadTags();
                if (getCurrentNote() != null)
                    loadNoteTags(getCurrentNote());
                updateStatus(java.text.MessageFormat.format(getString("status.deleted_tag"), tag.getTitle()));
            } catch (Exception e) {
                logger.severe("Failed to delete tag: " + e.getMessage());
                updateStatus(getString("status.error_deleting_tag"));
            }
        }
    }

    /**
     * Finds a tag by title.
     */
    private Tag findTagByTitle(String title) {
        if (title == null)
            return null;
        try {
            return tagService.getAllTags().stream()
                    .filter(t -> t.getTitle().equals(title))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void handleSystemAction(SystemActionEvent event) {
        javafx.application.Platform.runLater(() -> {
            switch (event.getActionType()) {
                case NEW_NOTE:
                    // Handled by NotesListController
                    break;
                case NEW_FOLDER:
                    handleNewFolder(null);
                    break;
                case NEW_TAG:
                    handleNewTag(null);
                    break;
                case SAVE:
                    // SAVE is handled by EditorController via SystemActionEvent subscription.
                    // Avoid duplicate processing here.
                    break;
                case SAVE_ALL:
                    handleSaveAll(null);
                    break;
                case DELETE:
                    // Handled by NotesListController and SidebarController
                    break;
                case IMPORT:
                    handleImport(null);
                    break;
                case EXPORT:
                    handleExport(null);
                    break;
                case EXIT:
                    handleExit(null);
                    break;
                case UNDO:
                    handleUndo(null);
                    break;
                case REDO:
                    handleRedo(null);
                    break;
                case CUT:
                    handleCut(null);
                    break;
                case COPY:
                    handleCopy(null);
                    break;
                case PASTE:
                    handlePaste(null);
                    break;
                case FIND:
                    handleFind(null);
                    break;
                case REPLACE:
                    handleReplace(null);
                    break;
                case TOGGLE_SIDEBAR:
                    handleToggleSidebar(null);
                    break;
                case TOGGLE_NOTES_LIST:
                    handleToggleNotesPanel(null);
                    break;
                case SWITCH_LAYOUT:
                    handleViewLayoutSwitch(null);
                    break;
                case ZOOM_IN:
                    handleZoomIn(null);
                    break;
                case ZOOM_OUT:
                    handleZoomOut(null);
                    break;
                case RESET_ZOOM:
                    handleResetZoom(null);
                    break;
                case ZOOM_EDITOR_IN:
                    handleEditorZoomIn(null);
                    break;
                case ZOOM_EDITOR_OUT:
                    handleEditorZoomOut(null);
                    break;
                case RESET_EDITOR_ZOOM:
                    handleEditorResetZoom(null);
                    break;
                case LIST_VIEW:
                    handleListView(null);
                    break;
                case GRID_VIEW:
                    handleGridView(null);
                    break;
                case TAGS_MANAGER:
                    handleTagsManager(null);
                    break;
                case PLUGIN_MANAGER:
                    handlePluginManager(null);
                    break;
                case PREFERENCES:
                    handlePreferences(null);
                    break;
                case SWITCH_STORAGE:
                    handleSwitchStorage();
                    break;
                case DOCUMENTATION:
                    handleDocumentation(null);
                    break;
                case ABOUT:
                    handleAbout(null);
                    break;
                case SORT_FOLDERS:
                    sidebarController.handleSortFolders(null);
                    break;
                case EXPAND_ALL_FOLDERS:
                    sidebarController.handleExpandAllFolders(null);
                    break;
                case COLLAPSE_ALL_FOLDERS:
                    sidebarController.handleCollapseAllFolders(null);
                    break;
                case SORT_TAGS:
                    // Re-routed to SidebarController natively
                    break;
                case SORT_RECENT:
                    // Re-routed
                    break;
                case SORT_FAVORITES:
                    // Re-routed
                    break;
                case SORT_TRASH:
                    sidebarController.handleSortTrash(null);
                    break;
                case EMPTY_TRASH:
                    sidebarController.handleEmptyTrash(null);
                    break;
                case REFRESH_NOTES:
                    handleRefresh(null);
                    break;
                case TOGGLE_TAGS:
                    handleToggleTags(null);
                    break;
                case EDITOR_ONLY_MODE:
                    handleEditorOnlyMode(null);
                    break;
                case SPLIT_VIEW_MODE:
                    handleSplitViewMode(null);
                    break;
                case PREVIEW_ONLY_MODE:
                    handlePreviewOnlyMode(null);
                    break;
                case TOGGLE_PIN:
                    handleTogglePin(null);
                    break;
                case TOGGLE_FAVORITE:
                    handleToggleFavorite(null);
                    break;
                case TOGGLE_RIGHT_PANEL:
                    handleToggleRightPanel(null);
                    break;
                default:
                    break;
            }
        });
    }
}
