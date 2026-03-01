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
import java.util.logging.Level;
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
import com.example.forevernote.ui.workflow.TagManagementWorkflow;
import com.example.forevernote.ui.workflow.CommandRoutingWorkflow;
import com.example.forevernote.ui.workflow.PluginLifecycleWorkflow;
import com.example.forevernote.ui.workflow.CommandUIWorkflow;
import com.example.forevernote.ui.workflow.DocumentIOWorkflow;
import com.example.forevernote.ui.workflow.UiEventHandlerWorkflow;
import com.example.forevernote.ui.workflow.UiInitializationWorkflow;
import com.example.forevernote.ui.workflow.UiEventSubscriptionWorkflow;
import com.example.forevernote.ui.workflow.NotesGridWorkflow;
import com.example.forevernote.ui.workflow.UiLayoutWorkflow;
import com.example.forevernote.ui.workflow.CommandRegistryWorkflow;
import com.example.forevernote.ui.workflow.FileCommandWorkflow;
import com.example.forevernote.ui.workflow.EditorCommandWorkflow;
import com.example.forevernote.ui.workflow.NavigationCommandWorkflow;
import com.example.forevernote.ui.workflow.UiDialogWorkflow;
import com.example.forevernote.ui.workflow.ThemeCommandWorkflow;

/**
 * Main controller for the Forevernote application.
 * Handles all UI interactions and manages the application state.
 * Implements PluginMenuRegistry and SidePanelRegistry to allow plugins to
 * register
 * menu items and UI panels dynamically (Modern-style).
 */
public class MainController implements PluginMenuRegistry, SidePanelRegistry, PreviewEnhancerRegistry {

    private static final Logger logger = LoggerConfig.getLogger(MainController.class);

    private Connection connection;
    private FactoryDAO factoryDAO;
    private FolderDAO folderDAO;
    private NoteDAO noteDAO;
    private TagDAO tagDAO;

    private Folder currentFolder;

    private Note getCurrentNote() {
        return editorController != null ? editorController.getCurrentNote() : null;
    }

    private boolean isModified() {
        return editorController != null && editorController.isModified();
    }

    private String currentFilterType = "all"; // "all", "folder", "tag", "favorites", "search"
    private Tag currentTag = null;

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

    private javafx.scene.layout.VBox sidebarPane;
    private TabPane navigationTabPane;
    private TreeView<Folder> folderTreeView;
    private TextField filterFoldersField;
    private TreeItem<Folder> allNotesItem;
    private ListView<String> tagListView;
    private TreeView<Component> trashTreeView;
    private TextField filterTrashField;

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

    private boolean isStackedLayout = false;

    private final Map<String, PreviewEnhancer> previewEnhancers = new HashMap<>();
    private final CommandRoutingWorkflow commandRoutingWorkflow = new CommandRoutingWorkflow();
    private final Map<SystemActionEvent.ActionType, Runnable> systemActionHandlers = new EnumMap<>(
            SystemActionEvent.ActionType.class);
    @FXML
    private Separator toolbarSeparator2;
    @FXML
    private Separator toolbarSeparator3;
    @FXML
    private Button closeRightPanelBtn;

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

    private ToggleGroup themeToggleGroup;
    private ToggleGroup languageToggleGroup;
    @FXML
    private Label infoAuthorLabel;
    @FXML
    private Label infoSourceUrlLabel;

    @FXML
    private Label statusLabel;
    @FXML
    private Label noteCountLabel;
    @FXML
    private Label syncStatusLabel;

    @FXML
    private Menu pluginsMenu;
    private final Map<String, Menu> pluginCategoryMenus = new HashMap<>();
    private final Map<String, List<MenuItem>> pluginMenuItems = new HashMap<>();

    @FXML
    private VBox pluginPanelsContainer;
    private final Map<String, VBox> pluginPanels = new HashMap<>();
    private final Map<String, List<String>> pluginPanelIds = new HashMap<>();

    @FXML
    private HBox pluginStatusBarContainer;
    private final Map<String, javafx.scene.Node> pluginStatusBarItems = new HashMap<>();
    private final Map<String, List<String>> pluginStatusBarItemIds = new HashMap<>();

    private UiLayoutWorkflow.ViewMode currentViewMode = UiLayoutWorkflow.ViewMode.SPLIT;

    private enum NotesViewMode {
        LIST, GRID
    }

    private NotesViewMode currentNotesViewMode = NotesViewMode.LIST;

    private javafx.scene.layout.TilePane notesGridPane;
    private javafx.scene.control.ScrollPane gridScrollPane;
    private VBox notesPanelContainer; // Reference to the notes panel container

    private CommandPalette commandPalette;
    private QuickSwitcher quickSwitcher;

    private NoteService noteService;
    private FolderService folderService;
    private TagService tagService;
    private EventBus eventBus;
    private PluginManager pluginManager;
    private PluginManagerDialog pluginManagerDialog;
    private NoteWorkflow noteWorkflow;
    private FolderWorkflow folderWorkflow;
    private TagWorkflow tagWorkflow;
    private final TagManagementWorkflow tagManagementWorkflow = new TagManagementWorkflow();
    private PreviewWorkflow previewWorkflow;
    private final PluginLifecycleWorkflow pluginLifecycleWorkflow = new PluginLifecycleWorkflow();
    private final CommandUIWorkflow commandUIWorkflow = new CommandUIWorkflow();
    private final DocumentIOWorkflow documentIOWorkflow = new DocumentIOWorkflow();
    private final UiEventSubscriptionWorkflow uiEventSubscriptionWorkflow = new UiEventSubscriptionWorkflow();
    private final UiEventHandlerWorkflow uiEventHandlerWorkflow = new UiEventHandlerWorkflow();
    private final UiInitializationWorkflow uiInitializationWorkflow = new UiInitializationWorkflow();
    private final NotesGridWorkflow notesGridWorkflow = new NotesGridWorkflow();
    private final UiLayoutWorkflow uiLayoutWorkflow = new UiLayoutWorkflow();
    private final CommandRegistryWorkflow commandRegistryWorkflow = new CommandRegistryWorkflow();
    private final FileCommandWorkflow fileCommandWorkflow = new FileCommandWorkflow();
    private final EditorCommandWorkflow editorCommandWorkflow = new EditorCommandWorkflow();
    private final NavigationCommandWorkflow navigationCommandWorkflow = new NavigationCommandWorkflow();
    private final UiDialogWorkflow uiDialogWorkflow = new UiDialogWorkflow();
    private final ThemeCommandWorkflow themeCommandWorkflow = new ThemeCommandWorkflow();

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
            initializeCommandRouting();
            initializeSystemActionHandlers();
            if (eventBus != null) {
                eventBus.subscribe(SystemActionEvent.class, this::handleSystemAction);
                subscribeToUIEvents();
            }
            if (sidebarController != null) {
                sidebarController.setEventBus(eventBus);
                sidebarController.setNoteService(noteService);
                sidebarController.setTagService(tagService);
                sidebarController.setFolderService(folderService);
                sidebarController.setFolderDAO(folderDAO);
                sidebarController.setNoteDAO(noteDAO);
                sidebarController.setTagDAO(tagDAO);
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
            logger.log(Level.SEVERE, "Failed to initialize MainController", e);
            updateStatus(java.text.MessageFormat.format(getString("status.error_details"), e.getMessage()));
        }
    }
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
            previewWorkflow = new PreviewWorkflow();

            // Initialize services
            noteService = new NoteService(noteDAO, folderDAO, tagDAO);
            folderService = new FolderService(folderDAO, noteDAO);
            tagService = new TagService(tagDAO, noteDAO);
            eventBus = EventBus.getInstance();

            logger.info("Database connections and services initialized");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
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

            ensureCommandUisInitialized(stage);
            commandUIWorkflow.initializeKeyboardShortcuts(
                    scene,
                    this::showCommandPalette,
                    this::showQuickSwitcher,
                    logger::info,
                    logger::warning);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to initialize keyboard shortcuts", e);
        }
    }

    /**
     * Shows the Command Palette (Ctrl+P).
     */
    public void showCommandPalette() {
        ensureCommandUisInitialized(getPrimaryStage());
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
        ensureCommandUisInitialized(getPrimaryStage());
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
            // optional plugins)
            PluginLifecycleWorkflow.LoadResult pluginLoadResult = pluginLifecycleWorkflow
                    .registerCoreAndExternalPlugins(pluginManager, logger::warning);

            // Initialize all registered plugins (they will register their menu items during
            // init)
            pluginManager.initializeAll();

            Stage stage = mainSplitPane != null && mainSplitPane.getScene() != null
                    ? (Stage) mainSplitPane.getScene().getWindow()
                    : null;
            pluginManagerDialog = new PluginManagerDialog(stage, pluginManager);

            // Register plugin manager command in Command Palette
            commandPalette.addCommand(new CommandPalette.Command(
                    "cmd.plugins.manage",
                    "Plugins: Manage Plugins",
                    "Open plugin manager to enable/disable plugins",
                    "Ctrl+Shift+P",
                    "=",
                    "Tools",
                    this::showPluginManager));

            // Subscribe to plugin events
            pluginLifecycleWorkflow.subscribePluginUiEvents(
                    eventBus,
                    () -> Platform.runLater(() -> {
                        sidebarController.loadRecentNotes();
                        sidebarController.loadTags();
                        sidebarController.loadFavorites();
                    }),
                    logger::info);

            if (!pluginLoadResult.loadFailures().isEmpty()) {
                for (String failure : pluginLoadResult.loadFailures()) {
                    logger.warning("Plugin load warning: " + failure);
                }
            }
            logger.info("Plugin system initialized with " + pluginManager.getPluginCount() + " plugins");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to initialize plugin system", e);
        }
    }

    /**
     * Subscribe to events from plugins.
     */
    private void subscribeToUIEvents() {
        if (eventBus == null) {
            return;
        }

        uiEventSubscriptionWorkflow.subscribeUiEvents(eventBus, new UiEventSubscriptionWorkflow.Port() {
            @Override
            public void applyTheme(String theme) {
                currentTheme = theme;
                prefs.put("theme", currentTheme);
                MainController.this.applyTheme();
            }

            @Override
            public void updateThemeMenuSelection() {
                MainController.this.updateThemeMenuSelection();
            }

            @Override
            public void loadNoteInEditor(Note note) {
                MainController.this.loadNoteInEditor(note);
            }

            @Override
            public void handleNotesLoaded(NoteEvents.NotesLoadedEvent event) {
                MainController.this.handleUiNotesLoaded(event);
            }

            @Override
            public void updateStatus(String message) {
                MainController.this.updateStatus(message);
            }

            @Override
            public void showCommandPalette() {
                MainController.this.showCommandPalette();
            }

            @Override
            public void showQuickSwitcher() {
                MainController.this.showQuickSwitcher();
            }

            @Override
            public void handleNoteDeleted(String noteId) {
                MainController.this.handleUiNoteDeleted(noteId);
            }

            @Override
            public void handleFolderDeleted(String folderId) {
                MainController.this.handleUiFolderDeleted(folderId);
            }

            @Override
            public void handleTrashItemDeleted() {
                MainController.this.handleUiTrashItemDeleted();
            }

            @Override
            public void handleFolderSelected(Folder folder) {
                MainController.this.handleUiFolderSelected(folder);
            }

            @Override
            public void handleTagSelected(Tag tag) {
                MainController.this.handleUiTagSelected(tag);
            }

            @Override
            public void handleNoteOpenRequest(Note note) {
                MainController.this.handleUiNoteOpenRequest(note);
            }

            @Override
            public void handleTrashItemSelected(Component component) {
                MainController.this.handleUiTrashItemSelected(component);
            }
        });
    }

    private void handleUiNotesLoaded(NoteEvents.NotesLoadedEvent event) {
        uiEventHandlerWorkflow.onNotesLoaded(event, noteCountLabel, currentNotesViewMode == NotesViewMode.GRID,
                this::refreshGridView);
    }

    private void handleUiNoteDeleted(String noteId) {
        uiEventHandlerWorkflow.onNoteDeleted(noteId, this::getCurrentNote, editorController, tagsFlowPane, previewWebView,
                this::refreshNotesList, sidebarController);
    }

    private void handleUiFolderDeleted(String folderId) {
        currentFolder = uiEventHandlerWorkflow.onFolderDeleted(folderId, currentFolder, folderTreeView, sidebarController);
    }

    private void handleUiTrashItemDeleted() {
        uiEventHandlerWorkflow.onTrashItemDeleted(sidebarController);
    }

    private void handleUiFolderSelected(Folder selectedFolder) {
        currentFolder = uiEventHandlerWorkflow.onFolderSelected(selectedFolder, notesListController,
                this::handleFolderSelection);
    }

    private void handleUiTagSelected(Tag tag) {
        tagWorkflow = uiEventHandlerWorkflow.onTagSelected(tag, tagWorkflow, folder -> currentFolder = folder,
                selectedTag -> currentTag = selectedTag, filterType -> currentFilterType = filterType,
                notesListController);
    }

    private void handleUiNoteOpenRequest(Note note) {
        Note noteToOpen = uiEventHandlerWorkflow.resolveNoteToOpen(note, () -> noteService.getAllNotes());
        uiEventHandlerWorkflow.onNoteOpenRequest(noteToOpen, this::loadNoteInEditor, notesListView);
    }

    private void handleUiTrashItemSelected(Component component) {
        uiEventHandlerWorkflow.onTrashItemSelected(component, this::loadNoteInEditor, this::handleFolderSelection);
    }

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

    @FXML
    private void handleCommandPalette(ActionEvent event) {
        showCommandPalette();
    }

    @FXML
    private void handleQuickSwitcher(ActionEvent event) {
        showQuickSwitcher();
    }

    @FXML
    private void handlePluginManager(ActionEvent event) {
        showPluginManager();
    }
    @Override
    public void registerMenuItem(String pluginId, String category, String itemName, Runnable action) {
        registerMenuItem(pluginId, category, itemName, null, action);
    }

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
                    logger.log(Level.WARNING, "Invalid shortcut for menu item: " + shortcut, e);
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
        CommandRoutingWorkflow.DispatchResult result = commandRoutingWorkflow
                .dispatch(commandName, this::executePluginCommandToken);
        logger.info("Executing command: " + result.resolvedToken());
        if (result.handled()) {
            return;
        }
        logger.warning("Unknown command: " + result.resolvedToken());
        updateStatus(java.text.MessageFormat.format(getString("status.unknown_command"), result.resolvedToken()));
    }

    private boolean executePluginCommandToken(String token) {
        if (commandPalette == null || token == null || token.isBlank()) {
            return false;
        }
        CommandPalette.Command cmd = commandPalette.findCommand(token);
        if (cmd == null) {
            cmd = commandPalette.findCommandById(token);
        }
        if (cmd == null) {
            return false;
        }
        cmd.execute();
        return true;
    }

    private void initializeCommandRouting() {
        if (!commandRoutingWorkflow.isEmpty()) {
            return;
        }
        commandRegistryWorkflow.registerDefaultRoutes(
                this::registerCommandRoute,
                commandRoutingWorkflow::registerAlias,
                this::resolveCommandAction);
    }

    private Runnable resolveCommandAction(String commandId) {
        if (commandId == null) {
            return null;
        }
        switch (commandId) {
            case "cmd.new_note":
                return () -> handleNewNote(null);
            case "cmd.new_folder":
                return () -> handleNewFolder(null);
            case "cmd.save":
                return () -> handleSave(null);
            case "cmd.save_all":
                return () -> handleSaveAll(null);
            case "cmd.import":
                return () -> handleImport(null);
            case "cmd.export":
                return () -> handleExport(null);
            case "cmd.delete_note":
                return () -> handleDelete(null);
            case "cmd.undo":
                return () -> handleUndo(null);
            case "cmd.redo":
                return () -> handleRedo(null);
            case "cmd.find":
                return () -> handleFind(null);
            case "cmd.replace":
                return () -> handleReplace(null);
            case "cmd.cut":
                return () -> handleCut(null);
            case "cmd.copy":
                return () -> handleCopy(null);
            case "cmd.paste":
                return () -> handlePaste(null);
            case "cmd.bold":
                return () -> handleBold(null);
            case "cmd.italic":
                return () -> handleItalic(null);
            case "cmd.underline":
                return () -> handleUnderline(null);
            case "cmd.insert_link":
                return () -> handleLink(null);
            case "cmd.insert_image":
                return () -> handleImage(null);
            case "cmd.insert_todo":
                return () -> handleTodoList(null);
            case "cmd.insert_list":
                return () -> handleNumberedList(null);
            case "cmd.toggle_sidebar":
                return () -> handleToggleSidebar(null);
            case "cmd.toggle_info_panel":
                return () -> handleToggleRightPanel(null);
            case "cmd.editor_mode":
                return () -> handleEditorOnlyMode(null);
            case "cmd.preview_mode":
                return () -> handlePreviewOnlyMode(null);
            case "cmd.split_mode":
                return () -> handleSplitViewMode(null);
            case "cmd.zoom_in":
                return () -> handleZoomIn(null);
            case "cmd.zoom_out":
                return () -> handleZoomOut(null);
            case "cmd.reset_zoom":
                return () -> handleResetZoom(null);
            case "cmd.theme_light":
                return () -> handleLightTheme(null);
            case "cmd.theme_dark":
                return () -> handleDarkTheme(null);
            case "cmd.theme_system":
                return () -> handleSystemTheme(null);
            case "cmd.quick_switcher":
                return this::showQuickSwitcher;
            case "cmd.global_search":
                return () -> handleSearch(null);
            case "cmd.goto_all_notes":
                return this::goToAllNotes;
            case "cmd.goto_favorites":
                return () -> {
                    if (sidebarController != null) {
                        sidebarController.loadFavorites();
                    }
                };
            case "cmd.goto_recent":
                return () -> {
                    if (sidebarController != null) {
                        sidebarController.loadRecentNotes();
                    }
                };
            case "cmd.tag_manager":
                return () -> handleTagsManager(null);
            case "cmd.preferences":
                return () -> handlePreferences(null);
            case "cmd.toggle_favorite":
                return () -> handleToggleFavorite(null);
            case "cmd.refresh":
                return () -> handleRefresh(null);
            case "cmd.plugins.manage":
                return this::showPluginManager;
            case "cmd.keyboard_shortcuts":
                return () -> handleKeyboardShortcuts(null);
            case "cmd.documentation":
                return () -> handleDocumentation(null);
            case "cmd.about":
                return () -> handleAbout(null);
            default:
                return null;
        }
    }

    private void registerCommandRoute(String id, String legacyName, Runnable action) {
        commandRoutingWorkflow.registerRoute(id, legacyName, action);
    }

    private void initializeSystemActionHandlers() {
        if (!systemActionHandlers.isEmpty()) {
            return;
        }

        systemActionHandlers.put(SystemActionEvent.ActionType.NEW_FOLDER, () -> handleNewFolder(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.NEW_TAG, () -> handleNewTag(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SAVE_ALL, () -> handleSaveAll(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.IMPORT, () -> handleImport(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EXPORT, () -> handleExport(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EXIT, () -> handleExit(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.UNDO, () -> handleUndo(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.REDO, () -> handleRedo(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.CUT, () -> handleCut(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.COPY, () -> handleCopy(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.PASTE, () -> handlePaste(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.FIND, () -> handleFind(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.REPLACE, () -> handleReplace(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_SIDEBAR, () -> handleToggleSidebar(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_NOTES_LIST, () -> handleToggleNotesPanel(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SWITCH_LAYOUT, () -> handleViewLayoutSwitch(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.ZOOM_IN, () -> handleZoomIn(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.ZOOM_OUT, () -> handleZoomOut(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.RESET_ZOOM, () -> handleResetZoom(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.ZOOM_EDITOR_IN, () -> handleEditorZoomIn(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.ZOOM_EDITOR_OUT, () -> handleEditorZoomOut(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.RESET_EDITOR_ZOOM, () -> handleEditorResetZoom(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.LIST_VIEW, () -> handleListView(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.GRID_VIEW, () -> handleGridView(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TAGS_MANAGER, () -> handleTagsManager(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.PLUGIN_MANAGER, () -> handlePluginManager(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.PREFERENCES, () -> handlePreferences(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SWITCH_STORAGE, this::handleSwitchStorage);
        systemActionHandlers.put(SystemActionEvent.ActionType.DOCUMENTATION, () -> handleDocumentation(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.ABOUT, () -> handleAbout(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SORT_FOLDERS, () -> sidebarController.handleSortFolders(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EXPAND_ALL_FOLDERS,
                () -> sidebarController.handleExpandAllFolders(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.COLLAPSE_ALL_FOLDERS,
                () -> sidebarController.handleCollapseAllFolders(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SORT_TRASH, () -> sidebarController.handleSortTrash(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EMPTY_TRASH, () -> sidebarController.handleEmptyTrash(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.REFRESH_NOTES, () -> handleRefresh(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_TAGS, () -> handleToggleTags(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EDITOR_ONLY_MODE, () -> handleEditorOnlyMode(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SPLIT_VIEW_MODE, () -> handleSplitViewMode(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.PREVIEW_ONLY_MODE, () -> handlePreviewOnlyMode(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_PIN, () -> handleTogglePin(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_FAVORITE, () -> handleToggleFavorite(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_RIGHT_PANEL, () -> handleToggleRightPanel(null));
    }

    private Stage getPrimaryStage() {
        if (mainSplitPane != null && mainSplitPane.getScene() != null) {
            javafx.stage.Window window = mainSplitPane.getScene().getWindow();
            if (window instanceof Stage stage) {
                return stage;
            }
        }
        return null;
    }

    private void ensureCommandUisInitialized(Stage stage) {
        CommandUIWorkflow.CommandUiComponents components = commandUIWorkflow.ensureCommandUiComponents(
                stage,
                commandPalette,
                quickSwitcher,
                this::executeCommand,
                this::loadNoteInEditor);
        commandPalette = components.commandPalette();
        quickSwitcher = components.quickSwitcher();
    }

    /**
     * Initialize sort options.
     */
    private void initializeSortOptions() {
        uiInitializationWorkflow.initializeSortOptions(sortComboBox, this::getString, this::sortNotes);
    }

    /**
     * Initialize view mode toggle buttons (Modern-style).
     */
    private void initializeViewModeButtons() {
        uiInitializationWorkflow.initializeViewModeButtons(
                editorOnlyButton,
                splitViewButton,
                previewOnlyButton,
                toolbarController,
                this::initializeGridView,
                this::applyViewMode);
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
        notesPanelContainer = notesGridWorkflow.applyNotesViewMode(
                currentNotesViewMode == NotesViewMode.GRID,
                notesListView,
                gridScrollPane,
                notesPanelContainer,
                this::refreshGridView,
                logger::warning);
    }

    /**
     * Refresh the grid view with current notes.
     */
    private void refreshGridView() {
        notesGridWorkflow.refreshGridView(
                notesGridPane,
                notesListView,
                isDarkThemeActive(),
                this::getString,
                this::loadNoteInEditor,
                this::updateStatus);
    }

    /**
     * Setup responsive behavior for the toolbar.
     */
    private void setupToolbarResponsiveness() {
        uiInitializationWorkflow.setupToolbarResponsiveness(toolbarController, this::updateToolbarOverflow);
    }

    /**
     * Update toolbar items based on available width.
     */
    private void updateToolbarOverflow(double width) {
        uiInitializationWorkflow.updateToolbarOverflow(
                toolbarController,
                width,
                this::getString,
                new UiInitializationWorkflow.ToolbarOverflowActions() {
                    @Override
                    public void focusSearch() {
                        toolbarController.getSearchField().requestFocus();
                    }

                    @Override
                    public void newNote() {
                        handleNewNote(null);
                    }

                    @Override
                    public void newFolder() {
                        handleNewFolder(null);
                    }

                    @Override
                    public void newTag() {
                        handleNewTag(null);
                    }

                    @Override
                    public void save() {
                        handleSave(null);
                    }

                    @Override
                    public void delete() {
                        handleDelete(null);
                    }

                    @Override
                    public void toggleSidebar() {
                        handleToggleSidebar(null);
                    }

                    @Override
                    public void toggleNotesPanel() {
                        handleToggleNotesPanel(null);
                    }

                    @Override
                    public void switchLayout() {
                        handleViewLayoutSwitch(null);
                    }
                });
    }

    /**
     * Apply the current view mode to the UI.
     */
    private void applyViewMode() {
        uiLayoutWorkflow.applyViewMode(
                currentViewMode,
                editorPreviewSplitPane,
                editorPane,
                previewPane,
                editorOnlyButton,
                splitViewButton,
                previewOnlyButton,
                this::updatePreview);
    }

    /**
     * Handle editor-only mode button click.
     */
    @FXML
    private void handleEditorOnlyMode(ActionEvent event) {
        currentViewMode = UiLayoutWorkflow.ViewMode.EDITOR_ONLY;
        applyViewMode();
        updateStatus(getString("status.mode_editor"));
    }

    /**
     * Handle split view mode button click.
     */
    @FXML
    private void handleSplitViewMode(ActionEvent event) {
        currentViewMode = UiLayoutWorkflow.ViewMode.SPLIT;
        applyViewMode();
        updateStatus(getString("status.mode_split"));
    }

    /**
     * Handle preview-only mode button click.
     */
    @FXML
    private void handlePreviewOnlyMode(ActionEvent event) {
        currentViewMode = UiLayoutWorkflow.ViewMode.PREVIEW_ONLY;
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
        uiLayoutWorkflow.toggleRightPanel(rightPanel, infoButton, getCurrentNote(), this::updateNoteInfoPanel);
    }

    /**
     * Handle close right panel button.
     */
    @FXML
    private void handleCloseRightPanel(ActionEvent event) {
        uiLayoutWorkflow.closeRightPanel(rightPanel, infoButton);
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
        uiInitializationWorkflow.initializeRightPanelSections(
                noteInfoHeader,
                noteInfoContent,
                noteInfoCollapseIcon,
                pluginPanelsContainer);
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
            notesListController.loadAllNotes();
        }
    }

    private void goToAllNotes() {
        if (folderTreeView != null && folderTreeView.getRoot() != null) {
            for (TreeItem<Folder> child : folderTreeView.getRoot().getChildren()) {
                Folder folder = child.getValue();
                if (folder != null && "ALL_NOTES_VIRTUAL".equals(folder.getId())) {
                    folderTreeView.getSelectionModel().select(child);
                    child.setExpanded(true);
                    return;
                }
            }
        }
        loadAllNotes();
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
            wordCountLabel.setText(java.text.MessageFormat.format(getString("info.words_count"), wordCount));
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
        tagManagementWorkflow.handleAddTagToNote(
                getCurrentNote(),
                tagService,
                noteDAO,
                this::getString,
                this::updateStatus,
                () -> {
                    if (sidebarController != null) {
                        sidebarController.loadTags();
                    }
                },
                this::loadNoteTags);
    }

    /**
     * Remove tag from note.
     */
    private void removeTagFromNote(Tag tag) {
        tagManagementWorkflow.removeTagFromNote(
                getCurrentNote(),
                tag,
                noteDAO,
                this::getString,
                this::updateStatus,
                this::loadNoteTags);
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


    @FXML
    private void handleNewNote(ActionEvent event) {
        try {
            if (noteWorkflow == null) {
                noteWorkflow = new NoteWorkflow(noteDAO);
            }
            fileCommandWorkflow.handleNewNote(noteWorkflow, noteService, folderService, currentFolder, this::getString,
                    new FileCommandWorkflow.NoteCreationUiPort() {
                        @Override
                        public void onCreated(Note note) {
                            notesListView.getItems().add(0, note);
                            notesListView.getSelectionModel().select(note);
                            loadNoteInEditor(note);
                            if (eventBus != null) {
                                eventBus.publish(new NoteEvents.NoteCreatedEvent(note));
                            }
                        }

                        @Override
                        public void onAfterCreate() {
                            if (sidebarController != null) {
                                sidebarController.loadRecentNotes();
                            }
                            if (folderTreeView != null) {
                                folderTreeView.refresh();
                            }
                            updateStatus(getString("status.note_created"));
                        }

                        @Override
                        public void onError(String statusKey) {
                            updateStatus(getString(statusKey));
                        }
                    });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create new note", e);
            updateStatus(getString("status.error_creating_note"));
        }
    }

    @FXML
    private void handleNewFolder(ActionEvent event) {
        if (folderWorkflow == null) {
            folderWorkflow = new FolderWorkflow();
        }
        fileCommandWorkflow.handleNewFolder(folderDAO, folderWorkflow, currentFolder, this::getString,
                new FileCommandWorkflow.FolderCreationUiPort() {
                    @Override
                    public void refreshFolders() {
                        if (sidebarController != null) {
                            sidebarController.loadFolders();
                        }
                    }

                    @Override
                    public void refreshTree() {
                        if (folderTreeView != null) {
                            folderTreeView.refresh();
                        }
                    }

                    @Override
                    public void onStatus(String message) {
                        updateStatus(message);
                    }
                });
    }

    /**
     * Handle creating a new subfolder in the currently selected folder.
     */
    private void handleNewSubfolder(ActionEvent event) {
        if (folderWorkflow == null) {
            folderWorkflow = new FolderWorkflow();
        }
        fileCommandWorkflow.handleNewSubfolder(folderDAO, folderWorkflow, currentFolder, this::getString,
                new FileCommandWorkflow.FolderCreationUiPort() {
                    @Override
                    public void refreshFolders() {
                        if (sidebarController != null) {
                            sidebarController.loadFolders();
                        }
                    }

                    @Override
                    public void refreshTree() {
                        if (folderTreeView != null) {
                            folderTreeView.refresh();
                        }
                    }

                    @Override
                    public void onStatus(String message) {
                        updateStatus(message);
                    }
                }, () -> handleNewFolder(event));
    }

    @FXML
    private void handleImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileCommandWorkflow.handleImport(fileChooser, () -> mainSplitPane.getScene().getWindow(), documentIOWorkflow,
                noteService, folderService, currentFolder, this::getString, new FileCommandWorkflow.ImportUiPort() {
                    @Override
                    public void refreshAfterImport() {
                        refreshNotesList();
                        if (sidebarController != null) {
                            sidebarController.loadRecentNotes();
                        }
                    }

                    @Override
                    public void onStatus(String message) {
                        updateStatus(message);
                    }

                    @Override
                    public void showInfo(String title, String header, String content) {
                        showAlert(Alert.AlertType.INFORMATION, title, header, content);
                    }
                });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        fileCommandWorkflow.handleSave(v -> {
            if (editorController != null) {
                editorController.handleSave();
            }
        }, () -> {
            refreshNotesList();
            if (sidebarController != null) {
                sidebarController.loadRecentNotes();
                sidebarController.loadFavorites();
            }
        });
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
        fileCommandWorkflow.handleDelete(v -> {
            if (eventBus != null) {
                eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.DELETE));
            }
        });
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
            logger.log(Level.WARNING, "Error during shutdown", e);
        }
    }

    @FXML
    private void handleExport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileCommandWorkflow.handleExport(fileChooser, () -> mainSplitPane.getScene().getWindow(), this::getCurrentNote,
                documentIOWorkflow, this::getString, new FileCommandWorkflow.ExportUiPort() {
                    @Override
                    public void onStatus(String message) {
                        updateStatus(message);
                    }

                    @Override
                    public void showWarning(String title, String header, String content) {
                        showAlert(Alert.AlertType.WARNING, title, header, content);
                    }

                    @Override
                    public void showInfo(String title, String header, String content) {
                        showAlert(Alert.AlertType.INFORMATION, title, header, content);
                    }

                    @Override
                    public void showError(String title, String header, String content) {
                        logger.severe("Failed to export note: " + content);
                        showAlert(Alert.AlertType.ERROR, title, header, content);
                    }
                });
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
        editorCommandWorkflow.handleUndo(noteContentArea);
    }

    @FXML
    private void handleRedo(ActionEvent event) {
        editorCommandWorkflow.handleRedo(this::getString, this::updateStatus);
    }

    @FXML
    private void handleCut(ActionEvent event) {
        editorCommandWorkflow.handleCut(noteContentArea, noteTitleField);
    }

    @FXML
    private void handleCopy(ActionEvent event) {
        editorCommandWorkflow.handleCopy(noteContentArea, noteTitleField);
    }

    @FXML
    private void handlePaste(ActionEvent event) {
        editorCommandWorkflow.handlePaste(noteContentArea, noteTitleField);
    }

    @FXML
    private void handleFind(ActionEvent event) {
        editorCommandWorkflow.handleFind(noteContentArea, this::getString, this::updateStatus);
    }

    @FXML
    private void handleReplace(ActionEvent event) {
        editorCommandWorkflow.handleReplace(noteContentArea, this::getString, this::updateStatus);
    }

    @FXML
    private void handleToggleSidebar(ActionEvent event) {
        navigationCommandWorkflow.toggleSidebar(
                isStackedLayout,
                navSplitPane,
                sidebarPane,
                mainSplitPane,
                toolbarController,
                this::getString,
                this::updateStatus);
    }

    @FXML
    private void handleToggleNotesPanel(ActionEvent event) {
        navigationCommandWorkflow.toggleNotesPanel(
                isStackedLayout,
                notesPanel,
                contentSplitPane,
                toolbarController,
                () -> handleToggleSidebar(event),
                this::getString,
                this::updateStatus);
    }

    @FXML
    private void handleZoomIn(ActionEvent event) {
        uiFontSize = navigationCommandWorkflow.zoomIn(uiFontSize);
        applyUiZoom();
    }

    @FXML
    private void handleZoomOut(ActionEvent event) {
        uiFontSize = navigationCommandWorkflow.zoomOut(uiFontSize);
        applyUiZoom();
    }

    @FXML
    private void handleResetZoom(ActionEvent event) {
        uiFontSize = navigationCommandWorkflow.resetUiZoom();
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
        isStackedLayout = navigationCommandWorkflow.switchLayout(
                isStackedLayout,
                mainSplitPane,
                contentSplitPane,
                navSplitPane,
                sidebarPane,
                notesPanel,
                editorContainer,
                toolbarController,
                this::getString,
                this::updateStatus);
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
        currentTheme = themeCommandWorkflow.setLightTheme(prefs);
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(getString("status.theme_light"));
    }

    @FXML
    private void handleDarkTheme(ActionEvent event) {
        currentTheme = themeCommandWorkflow.setDarkTheme(prefs);
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(getString("status.theme_dark"));
    }

    @FXML
    private void handleSystemTheme(ActionEvent event) {
        ThemeCommandWorkflow.SystemThemeResult result = themeCommandWorkflow.setSystemTheme(
                prefs,
                this::detectWindowsTheme,
                e -> logger.log(Level.WARNING, "Could not detect macOS theme", e));
        currentTheme = result.currentTheme();
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(java.text.MessageFormat.format(getString("status.theme_system"), result.detectedTheme()));
    }

    /**
     * Detect Windows theme (simplified approach).
     * In production, use JNA or similar library for better detection.
     */
    private boolean detectWindowsTheme() {
        return themeCommandWorkflow.detectWindowsTheme();
    }

    private void applyTheme() {
        javafx.scene.Scene scene = mainSplitPane != null ? mainSplitPane.getScene() : null;
        themeCommandWorkflow.applyTheme(
                scene,
                currentTheme,
                this::resolveThemeToApply,
                theme -> "dark".equalsIgnoreCase(theme)
                        ? getClass().getResource("/com/example/forevernote/ui/css/dark-theme.css")
                        : getClass().getResource("/com/example/forevernote/ui/css/modern-theme.css"),
                previewWebView,
                () -> {
                    if (getCurrentNote() != null) {
                        updatePreview();
                    }
                },
                logger::info,
                logger::warning);
    }

    /**
     * Detect system theme.
     */
    private String detectSystemTheme() {
        return themeCommandWorkflow.detectSystemTheme(
                this::detectWindowsTheme,
                e -> logger.log(Level.WARNING, "Could not detect macOS theme", e));
    }

    private String resolveThemeToApply() {
        return themeCommandWorkflow.resolveThemeToApply(currentTheme, this::detectSystemTheme);
    }

    private boolean isDarkThemeActive() {
        return themeCommandWorkflow.isDarkThemeActive(currentTheme, this::detectSystemTheme);
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        navigationCommandWorkflow.handleSearch(toolbarController, this::getString, this::updateStatus);
    }

    @FXML
    private void handleTagsManager(ActionEvent event) {
        tagManagementWorkflow.showTagsManager(
                tagService,
                this::getString,
                () -> {
                    if (sidebarController != null) {
                        sidebarController.loadTags();
                    }
                },
                this::updateStatus);
    }

    @FXML
    private void handlePreferences(ActionEvent event) {
        uiDialogWorkflow.showPreferences(this::getString);
    }

    @FXML
    private void handleDocumentation(ActionEvent event) {
        uiDialogWorkflow.showDocumentation(this::getString);
    }

    @FXML
    private void handleKeyboardShortcuts(ActionEvent event) {
        uiDialogWorkflow.showKeyboardShortcuts(this::getString);
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        uiDialogWorkflow.showAbout(this::getString);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        navigationCommandWorkflow.refreshByContext(
                currentFilterType,
                currentFolder,
                currentTag,
                noteService,
                notesListView,
                sortComboBox,
                noteCountLabel,
                this::refreshNotesList,
                this::handleFolderSelection,
                this::loadNotesForTag,
                filterType -> toolbarController != null && toolbarController.getSearchField() != null
                        ? toolbarController.getSearchField().getText()
                        : "",
                this::performSearch,
                this::getString,
                message -> {
                    if ("favorites".equals(currentFilterType)) {
                        currentFilterType = "favorites";
                        currentFolder = null;
                        currentTag = null;
                        sortNotes(sortComboBox.getValue());
                    }
                    updateStatus(message);
                },
                e -> logger.log(Level.SEVERE, "Failed to refresh", e));
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
        if (noteWorkflow == null) {
            noteWorkflow = new NoteWorkflow(noteDAO);
        }
        NoteWorkflow.NoteToggleResult result = noteWorkflow.toggleFavorite(note, noteService::updateNote);
        if (!result.success()) {
            updateStatus(getString("status.fav_error"));
            return;
        }
        if (getCurrentNote() != null && getCurrentNote().getId().equals(note.getId())) {
            updateFavoriteButtonIcon();
        }
        refreshNotesList();
        sidebarController.loadFavorites();
        updateStatus(getString(result.successStatusKey()));
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
        if (noteWorkflow == null) {
            noteWorkflow = new NoteWorkflow(noteDAO);
        }
        NoteWorkflow.NoteToggleResult result = noteWorkflow.togglePin(note, noteService::updateNote);
        if (!result.success()) {
            updateStatus(getString("status.pin_error"));
            return;
        }
        if (getCurrentNote() != null && getCurrentNote().getId().equals(note.getId())) {
            updatePinnedButtonIcon();
        }
        refreshNotesList();
        updateStatus(getString(result.successStatusKey()));
    }

    private void updatePinnedButtonIcon() {
        if (pinButton == null)
            return;

            if (getCurrentNote() != null && getCurrentNote().isPinned()) {
                pinButton.setSelected(true);
            pinButton.setTooltip(new Tooltip(getString("action.unpin_note")));
        } else {
            pinButton.setSelected(false);
            pinButton.setTooltip(new Tooltip(getString("tooltip.pin_note")));
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
        uiDialogWorkflow.handleNewTag(
                tagService,
                this::getString,
                () -> {
                    if (sidebarController != null) {
                        sidebarController.loadTags();
                    }
                },
                this::updateStatus,
                e -> logger.log(Level.SEVERE, "Failed to create tag", e));
    }

    @FXML
    private void handleBold(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.BOLD);
    }

    @FXML
    private void handleItalic(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.ITALIC);
    }

    @FXML
    private void handleUnderline(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.STRIKE);
    }

    @FXML
    private void handleLink(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.LINK);
    }

    @FXML
    private void handleImage(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.IMAGE);
    }

    @FXML
    private void handleAttachment(ActionEvent event) {
        editorCommandWorkflow.handleAttachmentNotSupported(this::getString, this::updateStatus);
    }

    @FXML
    private void handleTodoList(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.TODO_LIST);
    }

    @FXML
    private void handleNumberedList(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.NUMBERED_LIST);
    }

    @FXML
    private void handleSaveAll(ActionEvent event) {
        fileCommandWorkflow.handleSaveAll(
                () -> {
                    if (editorController != null) {
                        editorController.handleSave();
                    }
                },
                this::getString,
                this::updateStatus);
    }

    @FXML
    private void handleHeading1(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.HEADING1);
    }

    @FXML
    private void handleHeading2(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.HEADING2);
    }

    @FXML
    private void handleBulletList(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.BULLET_LIST);
    }

    @FXML
    private void handleCode(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.CODE);
    }

    @FXML
    private void handleQuote(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.QUOTE);
    }

    @FXML
    private void handleHeading3(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.HEADING3);
    }

    @FXML
    private void handleRealUnderline(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.UNDERLINE);
    }

    @FXML
    private void handleHighlight(ActionEvent event) {
        editorCommandWorkflow.publishAction(eventBus, SystemActionEvent.ActionType.HIGHLIGHT);
    }

    private void handleSystemAction(SystemActionEvent event) {
        javafx.application.Platform.runLater(() -> {
            if (event == null || event.getActionType() == null) {
                return;
            }
            Runnable handler = systemActionHandlers.get(event.getActionType());
            if (handler != null) {
                handler.run();
            }
        });
    }
}
