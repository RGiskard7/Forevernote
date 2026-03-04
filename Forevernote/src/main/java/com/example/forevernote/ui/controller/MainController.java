package com.example.forevernote.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.util.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.example.forevernote.ui.workflow.ThemeCatalogWorkflow;
import com.example.forevernote.ui.workflow.PluginUiWorkflow;
import com.example.forevernote.ui.workflow.AppSettingsWorkflow;
import com.example.forevernote.ui.workflow.UiPreferencesWorkflow;
import com.example.forevernote.ui.workflow.TabCommandWorkflow;
import com.example.forevernote.ui.workflow.graph.GraphWorkflow;
import com.example.forevernote.service.tabs.TabSessionService;
import com.example.forevernote.service.links.LinkIndexService;

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
    private TabPane noteTabsPane;
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
    private VBox rightPanel;
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
    private final Map<String, Menu> pluginCategoryMenus = new HashMap<>();
    private final Map<String, List<MenuItem>> pluginMenuItems = new HashMap<>();

    @FXML
    private VBox pluginPanelsContainer;
    @FXML
    private VBox rightPanelContent;
    private ListView<Note> backlinksListView;
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
    private final ThemeCatalogWorkflow themeCatalogWorkflow = new ThemeCatalogWorkflow();
    private final UiPreferencesWorkflow uiPreferencesWorkflow = new UiPreferencesWorkflow();
    private final PluginUiWorkflow pluginUiWorkflow = new PluginUiWorkflow();
    private final AppSettingsWorkflow appSettingsWorkflow = new AppSettingsWorkflow();
    private final TabCommandWorkflow tabCommandWorkflow = new TabCommandWorkflow();
    private final GraphWorkflow graphWorkflow = new GraphWorkflow();
    private final TabSessionService tabSessionService = new TabSessionService();
    private final LinkIndexService linkIndexService = new LinkIndexService();
    private final List<EventBus.Subscription> uiEventSubscriptions = new ArrayList<>();
    private EventBus.Subscription systemActionSubscription = EventBus.Subscription.NO_OP;

    @FXML
    private java.util.ResourceBundle resources;

    private double uiFontSize = 13.0;
    private double editorFontSize = 14.0;
    private final PauseTransition noteModifiedDebounce = new PauseTransition(Duration.millis(220));
    private final PauseTransition toolbarSearchDebounce = new PauseTransition(Duration.millis(180));
    private final PauseTransition autosaveDebounce = new PauseTransition(
            Duration.millis(UiPreferencesWorkflow.DEFAULT_AUTOSAVE_IDLE_MS));
    private String pendingModifiedNoteId;
    private String pendingSearchText = "";
    private boolean searchListenerBound = false;
    private final ExecutorService quickSwitcherExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "forevernote-quick-switcher-loader");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong quickSwitcherLoadVersion = new AtomicLong(0);
    private volatile List<Note> quickSwitcherNotesCache = List.of();
    private String sidebarTabsMode = UiPreferencesWorkflow.MODE_TEXT;
    private String editorViewButtonsMode = UiPreferencesWorkflow.MODE_TEXT;
    private boolean autosaveEnabled = true;
    private String previewStorageType = "sqlite";
    private String previewFileSystemRootDirectory = "";
    private int autosaveIdleMs = UiPreferencesWorkflow.DEFAULT_AUTOSAVE_IDLE_MS;
    private boolean autosaveRunning = false;
    private String themeSource = UiPreferencesWorkflow.THEME_SOURCE_BUILTIN;
    private String externalThemeId = "";
    private boolean customAccentEnabled = false;
    private String customAccentColor = "#7c3aed";
    private String lastPreviewRenderKey = "";
    private boolean featureTabsEnabled = true;
    private boolean featureGraphEnabled = true;
    private boolean featureObsidianLinksEnabled = true;
    private boolean switchingTabSelection = false;
    private final Map<String, Tab> uiTabsById = new HashMap<>();
    private boolean previewLinkHandlerBound = false;
    private Canvas graphCanvas;
    private ComboBox<String> graphModeCombo;
    private Spinner<Integer> graphDepthSpinner;
    private CheckBox graphUnresolvedCheck;
    private final Map<String, GraphWorkflow.GraphNode> graphNodeHitMap = new HashMap<>();
    private VBox graphWorkspaceContainer;
    private Canvas graphWorkspaceCanvas;
    private ComboBox<String> graphWorkspaceModeCombo;
    private Spinner<Integer> graphWorkspaceDepthSpinner;
    private CheckBox graphWorkspaceUnresolvedCheck;
    private double graphWorkspaceScale = 1.0;
    private double graphWorkspaceOffsetX = 0.0;
    private double graphWorkspaceOffsetY = 0.0;
    private double graphWorkspaceDragStartX = 0.0;
    private double graphWorkspaceDragStartY = 0.0;
    private GraphWorkflow.GraphData graphWorkspaceData = new GraphWorkflow.GraphData(List.of(), List.of());
    private String graphWorkspaceHoverNodeId;
    private Label graphWorkspaceStatsLabel;
    private boolean graphWorkspaceVisible = false;
    private boolean graphWorkspacePrevRightPanelVisible = true;
    private static final String PREF_TABS_SESSION_IDS = "tabs.session.ids";
    private static final String PREF_TABS_SESSION_ACTIVE_ID = "tabs.session.active_note_id";

    private enum SaveDialogDecision {
        SAVE,
        DONT_SAVE,
        CANCEL
    }

    private String getString(String key) {
        if (resources != null && resources.containsKey(key)) {
            return resources.getString(key);
        }
        return key; // Fallback to key if not found
    }

    private boolean isAllNotesVirtualFolder(Folder folder) {
        return folder != null && "ALL_NOTES_VIRTUAL".equals(folder.getId());
    }

    @FXML
    public void initialize() {
        try {
            configureNoteModifiedDebounce();
            configureToolbarSearchDebounce();
            configureAutosaveDebounce();
            navSplitPane = new SplitPane();
            navSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);

            initializeDatabase();

            if (toolbarController != null) {
                toolbarController.setEventBus(eventBus);
            }
            initializeCommandRouting();
            initializeSystemActionHandlers();
            if (eventBus != null) {
                systemActionSubscription.cancel();
                systemActionSubscription = eventBus.subscribe(SystemActionEvent.class, this::handleSystemAction);
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
                editorController.setBundle(resources);
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
                noteTabsPane = editorController.getNoteTabsPane();
                if (toggleTagsBtn != null) {
                    toggleTagsBtn.setSelected(false);
                }
                if (tagsContainer != null) {
                    tagsContainer.setVisible(false);
                    tagsContainer.setManaged(false);
                }
            }

            bindToolbarSearchFieldDebounced();

            initializeSortOptions();
            initializeViewModeButtons();
            initializeIcons();
            initializeRightPanelSections();
            setupToolbarResponsiveness();
            initializeFeatureFlags();
            initializeNoteTabs();
            initializePreviewLinkHandler();
            initializeBacklinksPanel();
            initializeGraphPanel();
            initializeThemeMenu();
            initializeLanguageMenu();
            applyUiPreferencesFromStore();

            sidebarController.loadFolders();
            sidebarController.loadTags();
            sidebarController.loadRecentNotes();
            sidebarController.loadFavorites();
            sidebarController.loadTrashTree();
            rebuildLinkIndex();
            restoreTabSession();

            Platform.runLater(this::initializeKeyboardShortcuts);

            Platform.runLater(this::initializePluginSystem);

            updateStatus(getString("status.ready"));
            logger.info("MainController initialized successfully");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize MainController", e);
            updateStatus(java.text.MessageFormat.format(getString("status.error_details"), e.getMessage()));
        }
    }

    private void initializeDatabase() {
        try {

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
                previewStorageType = "filesystem";
                previewFileSystemRootDirectory = dataDir;
            } else {
                SQLiteDB db = SQLiteDB.getInstance();
                connection = db.openConnection();
                factoryDAO = FactoryDAO.getFactory(FactoryDAO.SQLITE_FACTORY, connection);
                logger.info("Initialized SQLite Storage");
                previewStorageType = "sqlite";
                previewFileSystemRootDirectory = "";
            }

            folderDAO = factoryDAO.getFolderDAO();
            noteDAO = factoryDAO.getNoteDAO();
            tagDAO = factoryDAO.getLabelDAO();
            noteWorkflow = new NoteWorkflow(noteDAO);
            folderWorkflow = new FolderWorkflow();
            tagWorkflow = new TagWorkflow();
            previewWorkflow = new PreviewWorkflow();

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
        appSettingsWorkflow.handleSwitchStorage(
                mainSplitPane != null && mainSplitPane.getScene() != null ? mainSplitPane.getScene().getWindow() : null,
                this::getString,
                prefs);
    }

    private void initializeThemeMenu() {
        themeToggleGroup = appSettingsWorkflow.initializeThemeMenu(
                toolbarController,
                currentTheme,
                this::detectSystemTheme,
                theme -> currentTheme = theme,
                this::updateThemeMenuSelection,
                this::applyTheme);
    }

    private void initializeLanguageMenu() {
        languageToggleGroup = appSettingsWorkflow.initializeLanguageMenu(toolbarController, prefs);
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
        if (appSettingsWorkflow.changeLanguage(lang, prefs)) {
            showAlert(Alert.AlertType.INFORMATION,
                    getString("app.restart_required"),
                    getString("app.restart_required"),
                    getString("app.restart_message"));
        }
    }

    private void updateThemeMenuSelection() {
        appSettingsWorkflow.updateThemeMenuSelection(toolbarController, themeToggleGroup, currentTheme);
    }

    private void initializeKeyboardShortcuts() {
        try {
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

    public void showQuickSwitcher() {
        ensureCommandUisInitialized(getPrimaryStage());
        if (quickSwitcher != null) {
            quickSwitcher.setDarkTheme(isDarkThemeActive());
            if (!quickSwitcherNotesCache.isEmpty()) {
                quickSwitcher.setNotes(quickSwitcherNotesCache);
            }
            loadQuickSwitcherNotesAsync();
            quickSwitcher.show();
        }
    }

    private void loadQuickSwitcherNotesAsync() {
        if (noteService == null || quickSwitcher == null) {
            return;
        }
        final long requestId = quickSwitcherLoadVersion.incrementAndGet();
        quickSwitcherExecutor.submit(() -> {
            try {
                List<Note> allNotes = noteService.getAllNotes();
                quickSwitcherNotesCache = List.copyOf(allNotes);
                Platform.runLater(() -> {
                    if (requestId != quickSwitcherLoadVersion.get() || quickSwitcher == null) {
                        return;
                    }
                    quickSwitcher.setNotes(quickSwitcherNotesCache);
                });
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load notes for quick switcher", e);
            }
        });
    }

    private void initializePluginSystem() {
        try {
            if (commandPalette == null) {
                logger.warning("CommandPalette not available, delaying plugin initialization");
                return;
            }

            pluginManager = new PluginManager(noteService, folderService, tagService, eventBus, commandPalette, this,
                    this, this);

            PluginLifecycleWorkflow.LoadResult pluginLoadResult = pluginLifecycleWorkflow
                    .registerCoreAndExternalPlugins(pluginManager, logger::warning);

            pluginManager.initializeAll();

            Stage stage = mainSplitPane != null && mainSplitPane.getScene() != null
                    ? (Stage) mainSplitPane.getScene().getWindow()
                    : null;
            pluginManagerDialog = new PluginManagerDialog(stage, pluginManager);

            commandPalette.addCommand(new CommandPalette.Command(
                    "cmd.plugins.manage",
                    "Plugins: Manage Plugins",
                    "Open plugin manager to enable/disable plugins",
                    "Ctrl+Shift+P",
                    "=",
                    "Tools",
                    this::showPluginManager));

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

    private void subscribeToUIEvents() {
        if (eventBus == null) {
            return;
        }
        uiEventSubscriptions.forEach(EventBus.Subscription::cancel);
        uiEventSubscriptions.clear();
        uiEventSubscriptions
                .addAll(uiEventSubscriptionWorkflow.subscribeUiEvents(eventBus, new UiEventSubscriptionWorkflow.Port() {
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

                    @Override
                    public void handleNoteModified(Note note) {
                        MainController.this.handleUiNoteModified(note);
                    }
                }));
    }

    private void handleUiNotesLoaded(NoteEvents.NotesLoadedEvent event) {
        uiEventHandlerWorkflow.onNotesLoaded(event, noteCountLabel, currentNotesViewMode == NotesViewMode.GRID,
                this::refreshGridView);
    }

    private void handleUiNoteDeleted(String noteId) {
        uiEventHandlerWorkflow.onNoteDeleted(noteId, this::getCurrentNote, editorController, tagsFlowPane,
                previewWebView,
                this::refreshNotesList, sidebarController);
        if (noteId != null) {
            tabSessionService.findByNoteId(noteId).ifPresent(tab -> {
                tabSessionService.closeTab(tab.tabId());
                syncTabsUi();
            });
        }
        rebuildLinkIndex();
    }

    private void handleUiFolderDeleted(String folderId) {
        currentFolder = uiEventHandlerWorkflow.onFolderDeleted(folderId, currentFolder, folderTreeView,
                sidebarController);
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
        Note noteToOpen = uiEventHandlerWorkflow.resolveNoteToOpen(note, this::getNoteResolutionSource);
        uiEventHandlerWorkflow.onNoteOpenRequest(noteToOpen, this::loadNoteInEditor, notesListView);
    }

    private List<Note> getNoteResolutionSource() {
        if (!quickSwitcherNotesCache.isEmpty()) {
            return quickSwitcherNotesCache;
        }
        return noteService != null ? noteService.getAllNotes() : List.of();
    }

    private void handleUiTrashItemSelected(Component component) {
        uiEventHandlerWorkflow.onTrashItemSelected(component, this::loadNoteInEditor, this::handleFolderSelection);
    }

    private void handleUiNoteModified(Note note) {
        if (note == null || getCurrentNote() == null || !Objects.equals(note.getId(), getCurrentNote().getId())) {
            return;
        }
        pendingModifiedNoteId = note.getId();
        noteModifiedDebounce.playFromStart();
        markCurrentTabDirty(true);
        if (autosaveEnabled) {
            autosaveDebounce.playFromStart();
        }
    }

    private void configureNoteModifiedDebounce() {
        noteModifiedDebounce.setOnFinished(e -> {
            Note active = getCurrentNote();
            if (active == null || pendingModifiedNoteId == null
                    || !Objects.equals(pendingModifiedNoteId, active.getId())) {
                return;
            }
            updateWordCount();
            updatePreview();
            updateNoteInfoPanel();
        });
    }

    private void configureToolbarSearchDebounce() {
        toolbarSearchDebounce.setOnFinished(e -> {
            if (notesListController == null) {
                return;
            }
            String query = pendingSearchText != null ? pendingSearchText : "";
            if (query.trim().isEmpty()) {
                if ("search".equals(currentFilterType)) {
                    refreshNotesList();
                }
                return;
            }
            performSearch(query);
        });
    }

    private void configureAutosaveDebounce() {
        autosaveDebounce.setOnFinished(e -> {
            if (!autosaveEnabled || autosaveRunning) {
                return;
            }
            Note active = getCurrentNote();
            if (active == null || pendingModifiedNoteId == null
                    || !Objects.equals(pendingModifiedNoteId, active.getId())) {
                return;
            }
            if (!isModified()) {
                return;
            }
            autosaveRunning = true;
            try {
                handleSave(null);
                updateStatus(getString("status.autosave_done"));
            } finally {
                autosaveRunning = false;
            }
        });
    }

    private void applyUiPreferencesFromStore() {
        UiPreferencesWorkflow.UiPreferences uiPrefs = uiPreferencesWorkflow.load(prefs);
        sidebarTabsMode = uiPrefs.sidebarTabsMode();
        editorViewButtonsMode = uiPrefs.editorViewModeButtonsMode();
        autosaveEnabled = uiPrefs.autosaveEnabled();
        autosaveIdleMs = uiPrefs.autosaveIdleMs();
        themeSource = uiPrefs.themeSource();
        externalThemeId = uiPrefs.externalThemeId();
        customAccentEnabled = uiPrefs.accentEnabled();
        customAccentColor = uiPrefs.accentColor();
        autosaveDebounce.setDuration(Duration.millis(autosaveIdleMs));

        if (sidebarController != null) {
            sidebarController.applySidebarTabPresentation(sidebarTabsMode);
        }
        applyEditorButtonsPresentation();
        applyRootInlineStyle();
    }

    private void bindToolbarSearchFieldDebounced() {
        if (searchListenerBound || toolbarController == null || toolbarController.getSearchField() == null) {
            return;
        }
        toolbarController.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> {
            pendingSearchText = newVal != null ? newVal : "";
            toolbarSearchDebounce.playFromStart();
        });
        searchListenerBound = true;
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
        pluginUiWorkflow.registerMenuItem(
                pluginId,
                category,
                itemName,
                shortcut,
                action,
                toolbarController,
                pluginCategoryMenus,
                pluginMenuItems,
                () -> pluginManager,
                this::getString,
                this::updateStatus,
                logger::fine,
                logger::warning,
                e -> logger.log(Level.WARNING, "Invalid shortcut for plugin menu item", e));
    }

    @Override
    public void addMenuSeparator(String pluginId, String category) {
        pluginUiWorkflow.addMenuSeparator(pluginId, category, pluginCategoryMenus, pluginMenuItems);
    }

    @Override
    public void removePluginMenuItems(String pluginId) {
        pluginUiWorkflow.removePluginMenuItems(
                pluginId,
                toolbarController,
                pluginCategoryMenus,
                pluginMenuItems,
                logger::info);
    }

    @Override
    public boolean isPluginEnabled(String pluginId) {
        return pluginManager != null && pluginManager.isPluginEnabled(pluginId);
    }

    @Override
    public void registerSidePanel(String pluginId, String panelId, String title, javafx.scene.Node content,
            String icon) {
        pluginUiWorkflow.registerSidePanel(
                pluginId,
                panelId,
                title,
                content,
                icon,
                pluginPanelsContainer,
                pluginPanels,
                pluginPanelIds,
                logger::fine,
                logger::warning);
    }

    @Override
    public void removeSidePanel(String pluginId, String panelId) {
        pluginUiWorkflow.removeSidePanel(
                pluginId,
                panelId,
                pluginPanelsContainer,
                pluginPanels,
                pluginPanelIds,
                logger::info);
    }

    @Override
    public void removeAllSidePanels(String pluginId) {
        pluginUiWorkflow.removeAllSidePanels(
                pluginId,
                pluginPanelsContainer,
                pluginPanels,
                pluginPanelIds,
                logger::info);
    }

    @Override
    public void setPluginPanelsVisible(boolean visible) {
        pluginUiWorkflow.setPluginPanelsVisible(visible, pluginPanelsContainer);
    }

    @Override
    public boolean isPluginPanelsVisible() {
        return pluginUiWorkflow.isPluginPanelsVisible(pluginPanelsContainer);
    }

    public void registerStatusBarItem(String pluginId, String itemId, javafx.scene.Node content) {
        pluginUiWorkflow.registerStatusBarItem(
                pluginId,
                itemId,
                content,
                pluginStatusBarContainer,
                pluginStatusBarItems,
                pluginStatusBarItemIds,
                logger::fine,
                logger::warning);
    }

    public void removeStatusBarItem(String pluginId, String itemId) {
        pluginUiWorkflow.removeStatusBarItem(
                pluginId,
                itemId,
                pluginStatusBarContainer,
                pluginStatusBarItems,
                pluginStatusBarItemIds);
    }

    public void updateStatusBarItem(String pluginId, String itemId, javafx.scene.Node content) {
        pluginUiWorkflow.updateStatusBarItem(pluginId, itemId, content, pluginStatusBarItems);
    }

    public void removeAllStatusBarItems(String pluginId) {
        pluginUiWorkflow.removeAllStatusBarItems(
                pluginId,
                pluginStatusBarContainer,
                pluginStatusBarItems,
                pluginStatusBarItemIds);
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

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
        registerCommandRoute("cmd.tabs_next", "tab.next", () -> handleNextTab(null));
        registerCommandRoute("cmd.tabs_prev", "tab.prev", () -> handlePreviousTab(null));
        registerCommandRoute("cmd.tabs_close_current", "tab.close", () -> handleCloseCurrentTab(null));
        registerCommandRoute("cmd.tabs_close_others", "tab.close_others", () -> handleCloseOtherTabs(null));
        registerCommandRoute("cmd.tabs_close_all", "tab.close_all", () -> handleCloseAllTabs(null));
        registerCommandRoute("cmd.graph_open", "graph.open", () -> handleOpenGraphPanel(null));
        registerCommandRoute("cmd.graph_refresh", "graph.refresh", () -> handleRefreshGraph(null));
        registerCommandRoute("cmd.links_reindex", "links.reindex", () -> handleReindexLinks(null));
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
            case "cmd.tabs_next":
                return () -> handleNextTab(null);
            case "cmd.tabs_prev":
                return () -> handlePreviousTab(null);
            case "cmd.tabs_close_current":
                return () -> handleCloseCurrentTab(null);
            case "cmd.tabs_close_others":
                return () -> handleCloseOtherTabs(null);
            case "cmd.tabs_close_all":
                return () -> handleCloseAllTabs(null);
            case "cmd.graph_open":
                return () -> handleOpenGraphPanel(null);
            case "cmd.graph_refresh":
                return () -> handleRefreshGraph(null);
            case "cmd.links_reindex":
                return () -> handleReindexLinks(null);
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
        systemActionHandlers.put(SystemActionEvent.ActionType.SORT_FOLDERS,
                () -> sidebarController.handleSortFolders(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EXPAND_ALL_FOLDERS,
                () -> sidebarController.handleExpandAllFolders(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.COLLAPSE_ALL_FOLDERS,
                () -> sidebarController.handleCollapseAllFolders(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SORT_TRASH,
                () -> sidebarController.handleSortTrash(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EMPTY_TRASH,
                () -> sidebarController.handleEmptyTrash(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.REFRESH_NOTES, () -> handleRefresh(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_TAGS, () -> handleToggleTags(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.EDITOR_ONLY_MODE, () -> handleEditorOnlyMode(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.SPLIT_VIEW_MODE, () -> handleSplitViewMode(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.PREVIEW_ONLY_MODE, () -> handlePreviewOnlyMode(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_PIN, () -> handleTogglePin(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_FAVORITE, () -> handleToggleFavorite(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TOGGLE_RIGHT_PANEL, () -> handleToggleRightPanel(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TABS_NEXT, () -> handleNextTab(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TABS_PREVIOUS, () -> handlePreviousTab(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TAB_CLOSE_CURRENT, () -> handleCloseCurrentTab(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TAB_CLOSE_OTHERS, () -> handleCloseOtherTabs(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.TAB_CLOSE_ALL, () -> handleCloseAllTabs(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.GRAPH_OPEN, () -> handleOpenGraphPanel(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.GRAPH_REFRESH, () -> handleRefreshGraph(null));
        systemActionHandlers.put(SystemActionEvent.ActionType.LINKS_REINDEX, () -> handleReindexLinks(null));
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

    private void initializeSortOptions() {
        uiInitializationWorkflow.initializeSortOptions(sortComboBox, this::getString, this::sortNotes);
    }

    private void initializeViewModeButtons() {
        uiInitializationWorkflow.initializeViewModeButtons(
                editorOnlyButton,
                splitViewButton,
                previewOnlyButton,
                toolbarController,
                this::initializeGridView,
                this::applyViewMode);
        applyEditorButtonsPresentation();
    }

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

        if (notesListView != null && notesListView.getParent() instanceof VBox) {
            notesPanelContainer = (VBox) notesListView.getParent();
        }
    }

    @FXML
    private void handleListView(ActionEvent event) {
        if (currentNotesViewMode != NotesViewMode.LIST) {
            currentNotesViewMode = NotesViewMode.LIST;
            applyNotesViewMode();
            updateStatus(getString("status.view_list"));
        }
    }

    @FXML
    private void handleGridView(ActionEvent event) {
        if (currentNotesViewMode != NotesViewMode.GRID) {
            currentNotesViewMode = NotesViewMode.GRID;
            applyNotesViewMode();
            updateStatus(getString("status.view_grid"));
        }
    }

    private void applyNotesViewMode() {
        notesPanelContainer = notesGridWorkflow.applyNotesViewMode(
                currentNotesViewMode == NotesViewMode.GRID,
                notesListView,
                gridScrollPane,
                notesPanelContainer,
                this::refreshGridView,
                logger::warning);
    }

    private void refreshGridView() {
        notesGridWorkflow.refreshGridView(
                notesGridPane,
                notesListView,
                isDarkThemeActive(),
                this::getString,
                this::loadNoteInEditor,
                this::updateStatus);
    }

    private void setupToolbarResponsiveness() {
        uiInitializationWorkflow.setupToolbarResponsiveness(toolbarController, this::updateToolbarOverflow);
        if (editorContainer != null) {
            editorContainer.widthProperty().addListener((obs, oldVal, newVal) -> applyEditorButtonsPresentation());
        }
    }

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

    private void applyEditorButtonsPresentation() {
        if (editorController == null) {
            return;
        }
        double width = editorContainer != null ? editorContainer.getWidth() : 1200.0;
        editorController.applyViewModeButtonsPresentation(editorViewButtonsMode, width);
    }

    private void initializeFeatureFlags() {
        featureTabsEnabled = prefs.getBoolean("feature.tabs.enabled", true);
        featureGraphEnabled = prefs.getBoolean("feature.graph.enabled", true);
        featureObsidianLinksEnabled = prefs.getBoolean("feature.obsidian_links.enabled", true);
    }

    private void initializeNoteTabs() {
        if (noteTabsPane == null) {
            return;
        }
        noteTabsPane.setVisible(featureTabsEnabled);
        noteTabsPane.setManaged(featureTabsEnabled);
        if (!featureTabsEnabled) {
            return;
        }
        noteTabsPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (switchingTabSelection || newTab == null) {
                return;
            }
            String tabId = (String) newTab.getUserData();
            if (tabId == null) {
                return;
            }
            tabSessionService.activateTab(tabId);
            tabSessionService.findByTabId(tabId).ifPresent(state -> {
                Note note = noteService != null ? noteService.getNoteById(state.noteRef().noteId()).orElse(null) : null;
                if (note != null) {
                    loadNoteInEditor(note);
                }
            });
            if (eventBus != null) {
                eventBus.publish(new TabEvents.TabStateChangedEvent());
            }
        });
        noteTabsPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
    }

    private void restoreTabSession() {
        if (!featureTabsEnabled || noteService == null || noteTabsPane == null) {
            return;
        }
        String serialized = prefs.get(PREF_TABS_SESSION_IDS, "");
        if (serialized == null || serialized.isBlank()) {
            return;
        }
        String[] noteIds = serialized.split("\\R");
        for (String noteId : noteIds) {
            if (noteId == null || noteId.isBlank()) {
                continue;
            }
            Note note = noteService.getNoteById(noteId).orElse(null);
            if (note != null) {
                tabSessionService.openNote(
                        new TabSessionService.NoteRef(note.getId(), note.getTitle()),
                        TabSessionService.OpenMode.ACTIVATE_OR_OPEN);
            }
        }
        String activeNoteId = prefs.get(PREF_TABS_SESSION_ACTIVE_ID, "");
        if (activeNoteId != null && !activeNoteId.isBlank()) {
            tabSessionService.findByNoteId(activeNoteId).ifPresent(tab -> tabSessionService.activateTab(tab.tabId()));
        }
        syncTabsUi();
        tabSessionService.getActiveTab()
                .flatMap(tab -> noteService.getNoteById(tab.noteRef().noteId()))
                .ifPresent(this::loadNoteInEditor);
    }

    private void syncTabsUi() {
        if (noteTabsPane == null || !featureTabsEnabled) {
            return;
        }
        List<TabSessionService.TabState> states = tabSessionService.listTabs();
        Set<String> wanted = new HashSet<>();
        for (TabSessionService.TabState state : states) {
            wanted.add(state.tabId());
            Tab tab = uiTabsById.get(state.tabId());
            if (tab == null) {
                tab = new Tab();
                tab.setClosable(true);
                tab.setUserData(state.tabId());
                String tabId = state.tabId();
                tab.setOnClosed(e -> {
                    tabSessionService.closeTab(tabId);
                    uiTabsById.remove(tabId);
                    syncTabsUi();
                });
                uiTabsById.put(state.tabId(), tab);
            }
            String title = state.noteRef() != null ? state.noteRef().title() : getString("app.untitled");
            tab.setText((state.dirty() ? "* " : "") + (title == null || title.isBlank() ? getString("app.untitled") : title));
            if (!noteTabsPane.getTabs().contains(tab)) {
                noteTabsPane.getTabs().add(tab);
            }
        }
        List<String> toRemove = uiTabsById.keySet().stream().filter(id -> !wanted.contains(id)).toList();
        for (String id : toRemove) {
            Tab tab = uiTabsById.remove(id);
            if (tab != null) {
                noteTabsPane.getTabs().remove(tab);
            }
        }
        tabSessionService.getActiveTab().ifPresent(active -> {
            Tab activeTab = uiTabsById.get(active.tabId());
            if (activeTab != null) {
                switchingTabSelection = true;
                noteTabsPane.getSelectionModel().select(activeTab);
                switchingTabSelection = false;
            }
        });
        persistTabSession();
    }

    private void persistTabSession() {
        if (!featureTabsEnabled) {
            prefs.remove(PREF_TABS_SESSION_IDS);
            prefs.remove(PREF_TABS_SESSION_ACTIVE_ID);
            return;
        }
        List<String> noteIds = tabSessionService.listTabs().stream()
                .map(tab -> tab.noteRef() != null ? tab.noteRef().noteId() : null)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .toList();
        if (noteIds.isEmpty()) {
            prefs.remove(PREF_TABS_SESSION_IDS);
            prefs.remove(PREF_TABS_SESSION_ACTIVE_ID);
            return;
        }
        prefs.put(PREF_TABS_SESSION_IDS, String.join("\n", noteIds));
        String activeNoteId = tabSessionService.getActiveTab()
                .map(tab -> tab.noteRef() != null ? tab.noteRef().noteId() : null)
                .orElse("");
        if (activeNoteId != null && !activeNoteId.isBlank()) {
            prefs.put(PREF_TABS_SESSION_ACTIVE_ID, activeNoteId);
        } else {
            prefs.remove(PREF_TABS_SESSION_ACTIVE_ID);
        }
    }

    private void openNoteInTabs(Note note) {
        if (!featureTabsEnabled || note == null || note.getId() == null || noteTabsPane == null) {
            return;
        }
        tabSessionService.openNote(
                new TabSessionService.NoteRef(note.getId(), note.getTitle()),
                TabSessionService.OpenMode.ACTIVATE_OR_OPEN);
        syncTabsUi();
        if (eventBus != null) {
            eventBus.publish(new TabEvents.TabStateChangedEvent());
        }
    }

    private void markCurrentTabDirty(boolean dirty) {
        if (!featureTabsEnabled) {
            return;
        }
        tabSessionService.getActiveTab().ifPresent(active -> {
            tabSessionService.markDirty(active.tabId(), dirty);
            syncTabsUi();
        });
    }

    private void initializePreviewLinkHandler() {
        if (!featureObsidianLinksEnabled || previewWebView == null || previewLinkHandlerBound) {
            return;
        }
        previewWebView.getEngine().locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc == null || !newLoc.startsWith("forevernote://note/")) {
                return;
            }
            String encoded = newLoc.substring("forevernote://note/".length());
            String target = java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
            LinkIndexService.Resolution resolution = linkIndexService.resolveTarget(target,
                    getCurrentNote() != null ? getCurrentNote().getId() : null);
            if (resolution.targetNoteId() != null && noteService != null) {
                Note resolved = noteService.getNoteById(resolution.targetNoteId()).orElse(null);
                if (resolved != null) {
                    Platform.runLater(() -> loadNoteInEditor(resolved));
                }
            } else {
                updateStatus(getString("status.no_note_selected") + " (" + target + ")");
            }
            previewWebView.getEngine().getLoadWorker().cancel();
        });
        previewLinkHandlerBound = true;
    }

    private void rebuildLinkIndex() {
        if (!featureObsidianLinksEnabled || noteService == null) {
            return;
        }
        try {
            List<Note> rawNotes = noteService.getAllNotes();
            List<Note> fullNotes = new ArrayList<>(rawNotes.size());
            for (Note note : rawNotes) {
                if (note == null) {
                    continue;
                }
                String content = note.getContent();
                if ((content == null || content.isBlank()) && note.getId() != null && !note.getId().isBlank()) {
                    Note full = noteService.getNoteById(note.getId()).orElse(note);
                    fullNotes.add(full);
                } else {
                    fullNotes.add(note);
                }
            }
            linkIndexService.rebuildIndex(fullNotes);
            int links = linkIndexService.outgoingIndexSnapshot().values().stream().mapToInt(List::size).sum();
            logger.info("Link index rebuilt: notes=" + fullNotes.size() + ", links=" + links);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to rebuild link index", e);
        }
        redrawGraph();
        updateBacklinksPanel();
    }

    private void reindexCurrentNoteLinks() {
        if (!featureObsidianLinksEnabled || getCurrentNote() == null) {
            return;
        }
        try {
            linkIndexService.reindexNote(getCurrentNote());
            if (eventBus != null) {
                eventBus.publish(new LinkEvents.NoteLinksChangedEvent(getCurrentNote().getId()));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to reindex note links", e);
        }
        redrawGraph();
        updateBacklinksPanel();
    }

    private void initializeBacklinksPanel() {
        if (!featureObsidianLinksEnabled || rightPanelContent == null) {
            return;
        }
        VBox section = new VBox(8);
        section.getStyleClass().add("panel-section");
        Label title = new Label(getString("section.backlinks"));
        title.getStyleClass().add("section-title");

        backlinksListView = new ListView<>();
        backlinksListView.setPrefHeight(140);
        backlinksListView.setPlaceholder(new Label(getString("backlinks.empty")));
        backlinksListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.getTitle() == null || item.getTitle().isBlank()
                        ? getString("app.untitled")
                        : item.getTitle()));
            }
        });
        backlinksListView.setOnMouseClicked(e -> {
            if (e.getClickCount() < 2) {
                return;
            }
            Note selected = backlinksListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                loadNoteInEditor(selected);
            }
        });
        section.getChildren().addAll(title, backlinksListView);
        rightPanelContent.getChildren().add(section);
    }

    private void updateBacklinksPanel() {
        if (!featureObsidianLinksEnabled || backlinksListView == null || noteService == null || getCurrentNote() == null) {
            if (backlinksListView != null) {
                backlinksListView.getItems().clear();
            }
            return;
        }
        List<Note> incomingNotes = linkIndexService.getIncoming(getCurrentNote().getId()).stream()
                .map(LinkIndexService.LinkEdge::sourceNoteId)
                .distinct()
                .map(id -> noteService.getNoteById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        backlinksListView.getItems().setAll(incomingNotes);
    }

    private void initializeGraphPanel() {
        if (!featureGraphEnabled || rightPanelContent == null) {
            return;
        }
        VBox section = new VBox(8);
        section.getStyleClass().add("panel-section");

        Label title = new Label(getString("section.note_graph"));
        title.getStyleClass().add("section-title");

        HBox controls = new HBox(8);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        graphModeCombo = new ComboBox<>();
        graphModeCombo.getItems().addAll(getString("graph.mode.local"), getString("graph.mode.global"));
        graphModeCombo.getSelectionModel().select(0);
        graphModeCombo.valueProperty().addListener((obs, oldV, newV) -> redrawGraph());
        graphDepthSpinner = new Spinner<>(1, 6, 2);
        graphDepthSpinner.setEditable(false);
        graphDepthSpinner.valueProperty().addListener((obs, oldV, newV) -> redrawGraph());
        graphUnresolvedCheck = new CheckBox(getString("graph.include_unresolved"));
        graphUnresolvedCheck.setSelected(true);
        graphUnresolvedCheck.selectedProperty().addListener((obs, oldV, newV) -> redrawGraph());
        Button refreshGraphBtn = new Button(getString("action.refresh"));
        refreshGraphBtn.setOnAction(e -> redrawGraph());
        controls.getChildren().addAll(graphModeCombo, graphDepthSpinner, graphUnresolvedCheck, refreshGraphBtn);

        graphCanvas = new Canvas(280, 220);
        graphCanvas.widthProperty().bind(section.widthProperty().subtract(4));
        graphCanvas.heightProperty().set(220);
        graphCanvas.setOnMouseClicked(e -> {
            String nodeId = findGraphNodeAt(e.getX(), e.getY());
            if (nodeId == null || nodeId.startsWith("unresolved::") || noteService == null) {
                return;
            }
            Note note = noteService.getNoteById(nodeId).orElse(null);
            if (note != null) {
                loadNoteInEditor(note);
                if (eventBus != null) {
                    eventBus.publish(new GraphEvents.GraphSelectionChangedEvent(nodeId));
                }
            }
        });

        section.getChildren().addAll(title, controls, graphCanvas);
        rightPanelContent.getChildren().add(section);
    }

    private void redrawGraph() {
        if (!featureGraphEnabled || graphCanvas == null || noteService == null) {
            return;
        }
        List<Note> notes = noteService.getAllNotes();
        GraphWorkflow.GraphFilter filter = new GraphWorkflow.GraphFilter(
                Set.of(),
                Set.of(),
                graphUnresolvedCheck != null && graphUnresolvedCheck.isSelected(),
                220);
        GraphWorkflow.GraphData data;
        boolean global = graphModeCombo != null && graphModeCombo.getSelectionModel().getSelectedIndex() == 1;
        if (global) {
            data = graphWorkflow.buildGlobalGraph(notes, linkIndexService, filter);
        } else {
            String center = getCurrentNote() != null ? getCurrentNote().getId() : null;
            int depth = graphDepthSpinner != null ? graphDepthSpinner.getValue() : 2;
            data = graphWorkflow.buildLocalGraph(center, depth, notes, linkIndexService, filter);
        }
        renderGraph(data);
    }

    private void renderGraph(GraphWorkflow.GraphData data) {
        if (graphCanvas == null) {
            return;
        }
        GraphicsContext gc = graphCanvas.getGraphicsContext2D();
        double w = graphCanvas.getWidth();
        double h = graphCanvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFill(isDarkThemeActive() ? Color.web("#111827") : Color.web("#f8fafc"));
        gc.fillRect(0, 0, w, h);

        graphNodeHitMap.clear();
        if (data == null || data.nodes().isEmpty()) {
            gc.setFill(isDarkThemeActive() ? Color.web("#9ca3af") : Color.web("#6b7280"));
            gc.fillText(getString("graph.empty"), 12, 20);
            return;
        }

        gc.setLineWidth(1.2);
        Map<String, GraphWorkflow.GraphNode> nodeById = new HashMap<>(data.nodes().size() * 2);
        for (GraphWorkflow.GraphNode n : data.nodes()) {
            nodeById.put(n.id(), n);
        }
        for (GraphWorkflow.GraphEdge edge : data.edges()) {
            GraphWorkflow.GraphNode source = nodeById.get(edge.sourceId());
            GraphWorkflow.GraphNode target = nodeById.get(edge.targetId());
            if (source == null || target == null) {
                continue;
            }
            gc.setStroke(edge.unresolved() ? Color.web("#f59e0b") : (isDarkThemeActive() ? Color.web("#374151") : Color.web("#94a3b8")));
            gc.strokeLine(source.x() % w, source.y() % h, target.x() % w, target.y() % h);
        }

        for (GraphWorkflow.GraphNode node : data.nodes()) {
            double x = node.x() % w;
            double y = node.y() % h;
            double r = node.unresolved() ? 4.0 : 5.5;
            gc.setFill(node.unresolved() ? Color.web("#f59e0b") : Color.web(customAccentEnabled ? normalizeHexColor(customAccentColor) : (isDarkThemeActive() ? "#10b981" : "#2563eb")));
            gc.fillOval(x - r, y - r, r * 2, r * 2);
            gc.setFill(isDarkThemeActive() ? Color.web("#e5e7eb") : Color.web("#111827"));
            gc.fillText(shortGraphTitle(node.title()), x + 7, y + 4);
            graphNodeHitMap.put(node.id(), node);
        }
    }

    private String findGraphNodeAt(double x, double y) {
        for (GraphWorkflow.GraphNode node : graphNodeHitMap.values()) {
            double dx = (node.x() % graphCanvas.getWidth()) - x;
            double dy = (node.y() % graphCanvas.getHeight()) - y;
            if (Math.hypot(dx, dy) <= 8) {
                return node.id();
            }
        }
        return null;
    }

    private String shortGraphTitle(String title) {
        if (title == null || title.isBlank()) {
            return "?";
        }
        return title.length() > 22 ? title.substring(0, 22) + "..." : title;
    }

    private void ensureGraphWorkspace() {
        if (graphWorkspaceContainer != null || editorContainer == null) {
            return;
        }
        graphWorkspaceContainer = new VBox(10);
        graphWorkspaceContainer.getStyleClass().add("graph-workspace");
        graphWorkspaceContainer.setFillWidth(true);
        graphWorkspaceContainer.setVisible(false);
        graphWorkspaceContainer.setManaged(false);
        VBox.setVgrow(graphWorkspaceContainer, Priority.ALWAYS);

        HBox toolbar = new HBox(8);
        toolbar.setPadding(new javafx.geometry.Insets(10));
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("graph-workspace-toolbar");

        Label title = new Label(getString("section.note_graph"));
        title.getStyleClass().add("section-title");

        graphWorkspaceModeCombo = new ComboBox<>();
        graphWorkspaceModeCombo.getItems().addAll(getString("graph.mode.local"), getString("graph.mode.global"));
        graphWorkspaceModeCombo.getSelectionModel().select(graphModeCombo != null ? graphModeCombo.getSelectionModel().getSelectedIndex() : 1);
        graphWorkspaceModeCombo.setPrefWidth(150);
        graphWorkspaceModeCombo.valueProperty().addListener((obs, oldV, newV) -> {
            boolean global = graphWorkspaceModeCombo.getSelectionModel().getSelectedIndex() == 1;
            if (graphWorkspaceDepthSpinner != null) {
                graphWorkspaceDepthSpinner.setDisable(global);
                graphWorkspaceDepthSpinner.setOpacity(global ? 0.65 : 1.0);
            }
            redrawGraphWorkspace();
        });

        graphWorkspaceDepthSpinner = new Spinner<>(1, 6, graphDepthSpinner != null ? graphDepthSpinner.getValue() : 2);
        graphWorkspaceDepthSpinner.setEditable(false);
        graphWorkspaceDepthSpinner.setPrefWidth(80);
        graphWorkspaceDepthSpinner.valueProperty().addListener((obs, oldV, newV) -> redrawGraphWorkspace());

        graphWorkspaceUnresolvedCheck = new CheckBox(getString("graph.include_unresolved"));
        graphWorkspaceUnresolvedCheck.setSelected(graphUnresolvedCheck == null || graphUnresolvedCheck.isSelected());
        graphWorkspaceUnresolvedCheck.selectedProperty().addListener((obs, oldV, newV) -> redrawGraphWorkspace());
        graphWorkspaceDepthSpinner.setDisable(graphWorkspaceModeCombo.getSelectionModel().getSelectedIndex() == 1);
        graphWorkspaceDepthSpinner.setOpacity(graphWorkspaceDepthSpinner.isDisable() ? 0.65 : 1.0);

        Button fitBtn = new Button(getString("action.reset_zoom"));
        fitBtn.setOnAction(e -> {
            fitGraphWorkspaceToData();
            renderGraphWorkspace();
        });
        Button refreshBtn = new Button(getString("action.refresh"));
        refreshBtn.setOnAction(e -> redrawGraphWorkspace());
        Button closeBtn = new Button(getString("action.close"));
        closeBtn.setOnAction(e -> hideGraphWorkspace());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        graphWorkspaceStatsLabel = new Label("");
        graphWorkspaceStatsLabel.getStyleClass().add("meta-label");
        toolbar.getChildren().addAll(title, graphWorkspaceModeCombo, graphWorkspaceDepthSpinner, graphWorkspaceUnresolvedCheck,
                fitBtn, refreshBtn, graphWorkspaceStatsLabel, spacer, closeBtn);

        graphWorkspaceCanvas = new Canvas(1400, 900);
        StackPane canvasHost = new StackPane(graphWorkspaceCanvas);
        VBox.setVgrow(canvasHost, Priority.ALWAYS);
        canvasHost.setMinHeight(260);
        canvasHost.setPrefHeight(900);
        canvasHost.setPrefWidth(1200);

        graphWorkspaceCanvas.setOnScroll(e -> {
            double oldScale = graphWorkspaceScale;
            graphWorkspaceScale = Math.max(0.20, Math.min(6.0, graphWorkspaceScale * (e.getDeltaY() > 0 ? 1.1 : 0.9)));
            double mx = e.getX();
            double my = e.getY();
            graphWorkspaceOffsetX = mx - ((mx - graphWorkspaceOffsetX) * (graphWorkspaceScale / oldScale));
            graphWorkspaceOffsetY = my - ((my - graphWorkspaceOffsetY) * (graphWorkspaceScale / oldScale));
            renderGraphWorkspace();
            e.consume();
        });
        graphWorkspaceCanvas.setOnMousePressed(e -> {
            graphWorkspaceDragStartX = e.getX();
            graphWorkspaceDragStartY = e.getY();
        });
        graphWorkspaceCanvas.setOnMouseDragged(e -> {
            graphWorkspaceOffsetX += e.getX() - graphWorkspaceDragStartX;
            graphWorkspaceOffsetY += e.getY() - graphWorkspaceDragStartY;
            graphWorkspaceDragStartX = e.getX();
            graphWorkspaceDragStartY = e.getY();
            renderGraphWorkspace();
        });
        graphWorkspaceCanvas.setOnMouseMoved(e -> {
            graphWorkspaceHoverNodeId = findGraphWorkspaceNodeAt(e.getX(), e.getY());
            renderGraphWorkspace();
        });
        graphWorkspaceCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() < 1) {
                return;
            }
            String nodeId = findGraphWorkspaceNodeAt(e.getX(), e.getY());
            if (nodeId == null || nodeId.startsWith("unresolved::") || noteService == null) {
                return;
            }
            Note note = noteService.getNoteById(nodeId).orElse(null);
            if (note != null) {
                loadNoteInEditor(note);
                if (eventBus != null) {
                    eventBus.publish(new GraphEvents.GraphSelectionChangedEvent(nodeId));
                }
                hideGraphWorkspace();
            }
        });

        graphWorkspaceContainer.getChildren().addAll(toolbar, canvasHost);
        editorContainer.getChildren().add(graphWorkspaceContainer);
        graphWorkspaceContainer.layoutBoundsProperty().addListener((obs, oldB, newB) -> {
            if (newB == null) {
                return;
            }
            double targetH = Math.max(260, newB.getHeight() - toolbar.getHeight() - 8);
            canvasHost.setPrefHeight(targetH);
            canvasHost.setMinHeight(Math.min(targetH, 260));
            canvasHost.setPrefWidth(Math.max(400, newB.getWidth()));
            graphWorkspaceCanvas.setWidth(Math.max(400, newB.getWidth() - 2));
            graphWorkspaceCanvas.setHeight(targetH);
            fitGraphWorkspaceToData();
            renderGraphWorkspace();
        });
    }

    private void showGraphWorkspace() {
        ensureGraphWorkspace();
        if (graphWorkspaceContainer == null) {
            return;
        }
        if (graphWorkspaceModeCombo != null) {
            graphWorkspaceModeCombo.getSelectionModel().select(1); // Global by default, Obsidian-like
        }
        graphWorkspaceVisible = true;
        Node header = noteTitleField != null ? noteTitleField.getParent() : null;
        setVisibleManaged(header, false);
        setVisibleManaged(noteTabsPane, false);
        setVisibleManaged(tagsContainer, false);
        setVisibleManaged(editorPreviewSplitPane, false);
        if (rightPanel != null) {
            graphWorkspacePrevRightPanelVisible = rightPanel.isVisible();
            setVisibleManaged(rightPanel, false);
            if (infoButton != null) {
                infoButton.setSelected(false);
            }
        }
        setVisibleManaged(graphWorkspaceContainer, true);
        redrawGraphWorkspace();
        Platform.runLater(() -> {
            fitGraphWorkspaceToData();
            renderGraphWorkspace();
        });
    }

    private void hideGraphWorkspace() {
        if (!graphWorkspaceVisible) {
            return;
        }
        graphWorkspaceVisible = false;
        Node header = noteTitleField != null ? noteTitleField.getParent() : null;
        setVisibleManaged(header, true);
        setVisibleManaged(noteTabsPane, featureTabsEnabled);
        setVisibleManaged(tagsContainer, toggleTagsBtn != null && toggleTagsBtn.isSelected());
        setVisibleManaged(editorPreviewSplitPane, true);
        if (rightPanel != null && graphWorkspacePrevRightPanelVisible) {
            setVisibleManaged(rightPanel, true);
            if (infoButton != null) {
                infoButton.setSelected(true);
            }
        }
        setVisibleManaged(graphWorkspaceContainer, false);
    }

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void redrawGraphWorkspace() {
        if (!featureGraphEnabled || graphWorkspaceCanvas == null || noteService == null) {
            return;
        }
        try {
            List<Note> notes = noteService.getAllNotes();
            GraphWorkflow.GraphFilter filter = new GraphWorkflow.GraphFilter(
                    Set.of(),
                    Set.of(),
                    graphWorkspaceUnresolvedCheck != null && graphWorkspaceUnresolvedCheck.isSelected(),
                    5000);
            boolean global = graphWorkspaceModeCombo != null && graphWorkspaceModeCombo.getSelectionModel().getSelectedIndex() == 1;
            if (global) {
                graphWorkspaceData = graphWorkflow.buildGlobalGraph(notes, linkIndexService, filter);
            } else {
                String center = getCurrentNote() != null ? getCurrentNote().getId() : null;
                int depth = graphWorkspaceDepthSpinner != null ? graphWorkspaceDepthSpinner.getValue() : 2;
                graphWorkspaceData = graphWorkflow.buildLocalGraph(center, depth, notes, linkIndexService, filter);
            }
            if (graphWorkspaceStatsLabel != null) {
                int nodeCount = graphWorkspaceData != null ? graphWorkspaceData.nodes().size() : 0;
                int edgeCount = graphWorkspaceData != null ? graphWorkspaceData.edges().size() : 0;
                graphWorkspaceStatsLabel.setText(nodeCount + " nodos · " + edgeCount + " enlaces");
                updateStatus("Grafo: " + nodeCount + " nodos, " + edgeCount + " enlaces");
            }
            fitGraphWorkspaceToData();
            renderGraphWorkspace();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Graph redraw failed", ex);
            updateStatus("Error grafo: " + ex.getClass().getSimpleName());
        }
    }

    private void fitGraphWorkspaceToData() {
        if (graphWorkspaceCanvas == null || graphWorkspaceData == null || graphWorkspaceData.nodes().isEmpty()) {
            graphWorkspaceScale = 1.0;
            graphWorkspaceOffsetX = 0.0;
            graphWorkspaceOffsetY = 0.0;
            return;
        }
        double minX = graphWorkspaceData.nodes().stream().mapToDouble(GraphWorkflow.GraphNode::x).min().orElse(0);
        double maxX = graphWorkspaceData.nodes().stream().mapToDouble(GraphWorkflow.GraphNode::x).max().orElse(1000);
        double minY = graphWorkspaceData.nodes().stream().mapToDouble(GraphWorkflow.GraphNode::y).min().orElse(0);
        double maxY = graphWorkspaceData.nodes().stream().mapToDouble(GraphWorkflow.GraphNode::y).max().orElse(800);
        double width = Math.max(1.0, maxX - minX);
        double height = Math.max(1.0, maxY - minY);
        double padding = Math.max(120.0, Math.min(graphWorkspaceCanvas.getWidth(), graphWorkspaceCanvas.getHeight()) * 0.12);
        double canvasW = Math.max(100.0, graphWorkspaceCanvas.getWidth() - padding * 2.0);
        double canvasH = Math.max(100.0, graphWorkspaceCanvas.getHeight() - padding * 2.0);
        graphWorkspaceScale = Math.max(0.20, Math.min(2.2, Math.min(canvasW / width, canvasH / height) * 0.92));
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        graphWorkspaceOffsetX = graphWorkspaceCanvas.getWidth() / 2.0 - centerX * graphWorkspaceScale;
        graphWorkspaceOffsetY = graphWorkspaceCanvas.getHeight() / 2.0 - centerY * graphWorkspaceScale;
    }

    private void renderGraphWorkspace() {
        if (graphWorkspaceCanvas == null) {
            return;
        }
        GraphicsContext gc = graphWorkspaceCanvas.getGraphicsContext2D();
        double w = graphWorkspaceCanvas.getWidth();
        double h = graphWorkspaceCanvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFill(isDarkThemeActive() ? Color.web("#111827") : Color.web("#f8fafc"));
        gc.fillRect(0, 0, w, h);

        if (graphWorkspaceData == null || graphWorkspaceData.nodes().isEmpty()) {
            gc.setFill(isDarkThemeActive() ? Color.web("#9ca3af") : Color.web("#6b7280"));
            gc.fillText(getString("graph.empty"), 16, 24);
            return;
        }

        gc.setLineWidth(Math.max(0.5, 0.9 * graphWorkspaceScale));
        Map<String, GraphWorkflow.GraphNode> nodeById = new HashMap<>(graphWorkspaceData.nodes().size() * 2);
        for (GraphWorkflow.GraphNode n : graphWorkspaceData.nodes()) {
            nodeById.put(n.id(), n);
        }
        int totalEdges = graphWorkspaceData.edges().size();
        int edgeStride = 1;
        if (totalEdges > 25000 || graphWorkspaceData.nodes().size() > 2200) {
            edgeStride = Math.max(2, totalEdges / 12000);
        }
        boolean renderEdges = graphWorkspaceScale >= 0.35 || totalEdges <= 8000;
        for (int i = 0; i < totalEdges; i += edgeStride) {
            if (!renderEdges) {
                break;
            }
            GraphWorkflow.GraphEdge edge = graphWorkspaceData.edges().get(i);
            GraphWorkflow.GraphNode source = nodeById.get(edge.sourceId());
            GraphWorkflow.GraphNode target = nodeById.get(edge.targetId());
            if (source == null || target == null) {
                continue;
            }
            double sx = source.x() * graphWorkspaceScale + graphWorkspaceOffsetX;
            double sy = source.y() * graphWorkspaceScale + graphWorkspaceOffsetY;
            double tx = target.x() * graphWorkspaceScale + graphWorkspaceOffsetX;
            double ty = target.y() * graphWorkspaceScale + graphWorkspaceOffsetY;
            gc.setStroke(edge.unresolved()
                    ? Color.web("#f59e0b", isDarkThemeActive() ? 0.82 : 0.85)
                    : (isDarkThemeActive() ? Color.web("#9ca3af", 0.52) : Color.web("#64748b", 0.55)));
            gc.strokeLine(sx, sy, tx, ty);
        }

        boolean drawLabels = graphWorkspaceScale >= 2.0 && graphWorkspaceData.nodes().size() <= 120;
        for (GraphWorkflow.GraphNode node : graphWorkspaceData.nodes()) {
            double x = node.x() * graphWorkspaceScale + graphWorkspaceOffsetX;
            double y = node.y() * graphWorkspaceScale + graphWorkspaceOffsetY;
            double baseRadius = node.unresolved() ? 1.35 : 1.9;
            double r = Math.max(1.15, Math.min(5.0, baseRadius * Math.max(0.75, graphWorkspaceScale)));
            Color fill = node.unresolved()
                    ? Color.web("#f59e0b")
                    : Color.web(customAccentEnabled ? normalizeHexColor(customAccentColor)
                            : (isDarkThemeActive() ? "#10b981" : "#2563eb"));
            Color stroke = isDarkThemeActive() ? Color.web("#0b1220", 0.95) : Color.web("#ffffff", 0.95);
            gc.setFill(fill);
            gc.fillOval(x - r, y - r, r * 2, r * 2);
            gc.setStroke(stroke);
            gc.setLineWidth(Math.max(0.65, r * 0.33));
            gc.strokeOval(x - r, y - r, r * 2, r * 2);
            if (drawLabels || Objects.equals(graphWorkspaceHoverNodeId, node.id())) {
                gc.setFill(isDarkThemeActive() ? Color.web("#e5e7eb") : Color.web("#111827"));
                gc.fillText(shortGraphTitle(node.title()), x + Math.max(8.0, 6.0 * graphWorkspaceScale), y + 3.0);
            }
        }

        // Debug/estado visible para verificar render incluso con datasets grandes
        gc.setFill(isDarkThemeActive() ? Color.web("#9ca3af") : Color.web("#475569"));
        gc.fillText("Nodes: " + graphWorkspaceData.nodes().size() + "  Edges: " + graphWorkspaceData.edges().size(),
                12, Math.max(18, h - 12));
    }

    private String findGraphWorkspaceNodeAt(double x, double y) {
        if (graphWorkspaceData == null || graphWorkspaceCanvas == null) {
            return null;
        }
        for (GraphWorkflow.GraphNode node : graphWorkspaceData.nodes()) {
            double nx = node.x() * graphWorkspaceScale + graphWorkspaceOffsetX;
            double ny = node.y() * graphWorkspaceScale + graphWorkspaceOffsetY;
            double hit = Math.max(6.0, 8.0 * graphWorkspaceScale);
            if (Math.hypot(nx - x, ny - y) <= hit) {
                return node.id();
            }
        }
        return null;
    }

    @FXML
    private void handleNextTab(ActionEvent event) {
        if (!featureTabsEnabled) {
            return;
        }
        tabCommandWorkflow.next(tabSessionService)
                .flatMap(tab -> noteService.getNoteById(tab.noteRef().noteId()))
                .ifPresent(this::loadNoteInEditor);
    }

    @FXML
    private void handlePreviousTab(ActionEvent event) {
        if (!featureTabsEnabled) {
            return;
        }
        tabCommandWorkflow.previous(tabSessionService)
                .flatMap(tab -> noteService.getNoteById(tab.noteRef().noteId()))
                .ifPresent(this::loadNoteInEditor);
    }

    @FXML
    private void handleCloseCurrentTab(ActionEvent event) {
        if (!featureTabsEnabled) {
            return;
        }
        boolean closed = tabCommandWorkflow.closeCurrent(tabSessionService);
        if (closed) {
            syncTabsUi();
            tabSessionService.getActiveTab()
                    .flatMap(tab -> noteService.getNoteById(tab.noteRef().noteId()))
                    .ifPresent(this::loadNoteInEditor);
        }
    }

    @FXML
    private void handleCloseOtherTabs(ActionEvent event) {
        if (!featureTabsEnabled) {
            return;
        }
        tabCommandWorkflow.closeOthers(tabSessionService);
        syncTabsUi();
    }

    @FXML
    private void handleCloseAllTabs(ActionEvent event) {
        if (!featureTabsEnabled) {
            return;
        }
        tabCommandWorkflow.closeAll(tabSessionService);
        syncTabsUi();
    }

    @FXML
    private void handleOpenGraphPanel(ActionEvent event) {
        if (!featureGraphEnabled) {
            updateStatus(getString("status.error"));
            return;
        }
        showGraphWorkspace();
        updateStatus(getString("section.note_graph"));
    }

    @FXML
    private void handleRefreshGraph(ActionEvent event) {
        redrawGraph();
        if (graphWorkspaceVisible) {
            redrawGraphWorkspace();
        }
    }

    @FXML
    private void handleReindexLinks(ActionEvent event) {
        rebuildLinkIndex();
        updateStatus(getString("status.links_reindexed"));
    }

    @FXML
    private void handleEditorOnlyMode(ActionEvent event) {
        currentViewMode = UiLayoutWorkflow.ViewMode.EDITOR_ONLY;
        applyViewMode();
        updateStatus(getString("status.mode_editor"));
    }

    @FXML
    private void handleSplitViewMode(ActionEvent event) {
        currentViewMode = UiLayoutWorkflow.ViewMode.SPLIT;
        applyViewMode();
        updateStatus(getString("status.mode_split"));
    }

    @FXML
    private void handlePreviewOnlyMode(ActionEvent event) {
        currentViewMode = UiLayoutWorkflow.ViewMode.PREVIEW_ONLY;
        applyViewMode();
        updateStatus(getString("status.mode_preview"));
    }

    private void initializeIcons() {
    }

    @FXML
    private void handleToggleRightPanel(ActionEvent event) {
        uiLayoutWorkflow.toggleRightPanel(rightPanel, infoButton, getCurrentNote(), this::updateNoteInfoPanel);
    }

    @FXML
    private void handleCloseRightPanel(ActionEvent event) {
        uiLayoutWorkflow.closeRightPanel(rightPanel, infoButton);
    }

    @FXML
    private void handleShowNoteInfo(ActionEvent event) {
        handleToggleRightPanel(event);
    }

    private void initializeRightPanelSections() {
        uiInitializationWorkflow.initializeRightPanelSections(
                noteInfoHeader,
                noteInfoContent,
                noteInfoCollapseIcon,
                pluginPanelsContainer);
    }

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

    private void loadNoteInEditor(Note note) {
        if (graphWorkspaceVisible) {
            hideGraphWorkspace();
        }
        if (isModified() && getCurrentNote() != null) {
            SaveDialogDecision decision = showSaveDialog();
            if (decision == SaveDialogDecision.CANCEL) {
                return;
            }
            if (decision == SaveDialogDecision.SAVE) {
                handleSave(new ActionEvent());
            }
        }

        if (editorController != null) {
            editorController.loadNote(note);
        }
        if (note != null) {
            openNoteInTabs(note);
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
            updateBacklinksPanel();
            updateStatus(getString("status.no_note_selected"));
            return;
        }

        loadNoteTags(activeNote);

        updateNoteMetadata(activeNote);

        if (previewWebView != null && !previewWebView.getStyleClass().contains("webview-theme")) {
            previewWebView.getStyleClass().add("webview-theme");
        }

        updatePreview();

        updateFavoriteButtonIcon();

        updatePinnedButtonIcon();
        markCurrentTabDirty(false);
        redrawGraph();
        updateBacklinksPanel();

        updateStatus(java.text.MessageFormat.format(getString("status.note_loaded"), activeNote.getTitle()));
    }

    private void loadNoteTags(Note note) {
        if (noteWorkflow == null) {
            noteWorkflow = new NoteWorkflow(noteDAO);
        }
        noteWorkflow.loadNoteTags(note, tagsFlowPane, this::handleAddTagToNote, this::removeTagFromNote);
    }

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

    private void performSearch(String searchText) {
        if (notesListController != null) {
            currentFilterType = "search";
            notesListController.performSearch(searchText);
        }
    }

    private void sortNotes(String sortOption) {
        if (notesListController != null) {
            notesListController.sortNotes(sortOption);
        }
    }

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

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

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
        if (currentViewMode == UiLayoutWorkflow.ViewMode.EDITOR_ONLY) {
            return;
        }
        if (previewPane != null && (!previewPane.isVisible() || previewPane.getWidth() < 40 || previewPane.getHeight() < 40)) {
            return;
        }
        if (previewWorkflow == null) {
            previewWorkflow = new PreviewWorkflow();
        }
        String content = noteContentArea != null ? noteContentArea.getText() : "";
        boolean isDarkTheme = "dark".equals(resolveThemeToApply());
        Note currentNote = getCurrentNote();
        String previewKey = Integer.toHexString(Objects.hash(
                currentNote != null ? currentNote.getId() : "",
                content,
                isDarkTheme,
                previewStorageType,
                previewFileSystemRootDirectory,
                new TreeSet<>(previewEnhancers.keySet())));
        if (previewKey.equals(lastPreviewRenderKey)) {
            return;
        }
        PreviewWorkflow.LinkResolver linkResolver = null;
        if (featureObsidianLinksEnabled) {
            linkResolver = (rawTarget, sourceId) -> {
                LinkIndexService.Resolution resolution = linkIndexService.resolveTarget(rawTarget, sourceId);
                if (resolution.targetNoteId() != null) {
                    return "forevernote://note/"
                            + java.net.URLEncoder.encode(resolution.targetNoteId(),
                                    java.nio.charset.StandardCharsets.UTF_8);
                }
                return "forevernote://note/"
                        + java.net.URLEncoder.encode(rawTarget, java.nio.charset.StandardCharsets.UTF_8);
            };
        }
        PreviewWorkflow.PreviewContext previewContext = new PreviewWorkflow.PreviewContext(
                previewStorageType,
                previewFileSystemRootDirectory,
                currentNote != null ? currentNote.getId() : null,
                linkResolver);
        String html = (content != null && !content.trim().isEmpty())
                ? previewWorkflow.buildPreviewHtml(content, isDarkTheme, previewEnhancers.values(), previewContext)
                : previewWorkflow.buildEmptyHtml(isDarkTheme);
        previewWebView.getEngine().loadContent(html, "text/html");
        lastPreviewRenderKey = previewKey;
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private SaveDialogDecision showSaveDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(getString("dialog.save_changes.title"));
        alert.setHeaderText(getString("dialog.save_changes.header"));
        alert.setContentText(getString("dialog.save_changes.content"));

        ButtonType saveButton = new ButtonType(getString("action.save"));
        ButtonType dontSaveButton = new ButtonType(getString("action.dont_save"));
        ButtonType cancelButton = new ButtonType(getString("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty()) {
            return SaveDialogDecision.CANCEL;
        }
        if (result.get() == saveButton) {
            return SaveDialogDecision.SAVE;
        }
        if (result.get() == dontSaveButton) {
            return SaveDialogDecision.DONT_SAVE;
        }
        return SaveDialogDecision.CANCEL;
    }

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

    private void removeTagFromNote(Tag tag) {
        tagManagementWorkflow.removeTagFromNote(
                getCurrentNote(),
                tag,
                noteDAO,
                this::getString,
                this::updateStatus,
                this::loadNoteTags);
    }

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
                            refreshNotesList();
                            if (sidebarController != null) {
                                sidebarController.loadRecentNotes();
                                sidebarController.loadFolders();
                            }
                            rebuildLinkIndex();
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
                        rebuildLinkIndex();
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
            if (getCurrentNote() != null) {
                tabSessionService.updateTabTitleForNote(getCurrentNote().getId(), getCurrentNote().getTitle());
                syncTabsUi();
            }
            markCurrentTabDirty(false);
            reindexCurrentNoteLinks();
        });
    }

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
                () -> {
                    refreshGridView();
                    redrawGraph();
                });
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        fileCommandWorkflow.handleDelete(v -> {
            if (eventBus != null) {
                eventBus.publish(new SystemActionEvent(SystemActionEvent.ActionType.DELETE));
            }
            if (getCurrentNote() != null) {
                tabSessionService.findByNoteId(getCurrentNote().getId()).ifPresent(tab -> {
                    tabSessionService.closeTab(tab.tabId());
                    syncTabsUi();
                });
            }
            rebuildLinkIndex();
        });
    }

    @FXML
    private void handleExit(ActionEvent event) {
        if (isModified() && getCurrentNote() != null) {
            SaveDialogDecision decision = showSaveDialog();
            if (decision == SaveDialogDecision.CANCEL) {
                return;
            }
            if (decision == SaveDialogDecision.SAVE) {
                handleSave(event);
            }
        }

        shutdownApplication();
        Platform.exit();
        System.exit(0);
    }

    public void shutdownApplication() {
        try {
            uiEventSubscriptions.forEach(EventBus.Subscription::cancel);
            uiEventSubscriptions.clear();
            if (systemActionSubscription != null) {
                systemActionSubscription.cancel();
            }
            if (pluginManager != null) {
                pluginManager.shutdownAll();
            }
            com.example.forevernote.plugin.PluginLoader.closeAllClassLoaders();
            quickSwitcherExecutor.shutdownNow();

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
        applyRootInlineStyle();
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
        boolean show = true;

        if (toggleTagsBtn != null) {
            show = toggleTagsBtn.isSelected();
        } else if (tagsContainer != null) {
            show = !tagsContainer.isVisible();
        }

        if (tagsContainer != null) {
            tagsContainer.setVisible(show);
            tagsContainer.setManaged(show);
            updateStatus(show ? getString("status.tags_bar_shown") : getString("status.tags_bar_hidden"));
        }
    }

    private static final Preferences prefs = Preferences.userNodeForPackage(MainController.class);

    private String currentTheme = prefs.get("theme", "light"); // Load from preferences

    @FXML
    private void handleLightTheme(ActionEvent event) {
        switchToBuiltinThemeSource();
        currentTheme = themeCommandWorkflow.setLightTheme(prefs);
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(getString("status.theme_light"));
    }

    @FXML
    private void handleDarkTheme(ActionEvent event) {
        switchToBuiltinThemeSource();
        currentTheme = themeCommandWorkflow.setDarkTheme(prefs);
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(getString("status.theme_dark"));
    }

    @FXML
    private void handleSystemTheme(ActionEvent event) {
        switchToBuiltinThemeSource();
        ThemeCommandWorkflow.SystemThemeResult result = themeCommandWorkflow.setSystemTheme(
                prefs,
                this::detectWindowsTheme,
                e -> logger.log(Level.WARNING, "Could not detect macOS theme", e));
        currentTheme = result.currentTheme();
        updateThemeMenuSelection();
        applyTheme();
        updateStatus(java.text.MessageFormat.format(getString("status.theme_system"), result.detectedTheme()));
    }

    private void switchToBuiltinThemeSource() {
        if (!UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL.equals(themeSource)) {
            return;
        }
        themeSource = UiPreferencesWorkflow.THEME_SOURCE_BUILTIN;
        prefs.put(UiPreferencesWorkflow.THEME_SOURCE_KEY, UiPreferencesWorkflow.THEME_SOURCE_BUILTIN);
    }

    private boolean detectWindowsTheme() {
        return themeCommandWorkflow.detectWindowsTheme();
    }

    private void applyTheme() {
        javafx.scene.Scene scene = mainSplitPane != null ? mainSplitPane.getScene() : null;
        if (UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL.equals(themeSource)) {
            List<ThemeCatalogWorkflow.ThemeDescriptor> themes = themeCatalogWorkflow.getAvailableThemes();
            ThemeCatalogWorkflow.ThemeDescriptor external = themeCatalogWorkflow.findById(themes, externalThemeId);
            if (scene != null) {
                scene.getStylesheets().removeIf(stylesheet -> stylesheet.contains("modern-theme.css")
                        || stylesheet.contains("dark-theme.css") || stylesheet.contains("/themes/"));
            }
            if (external != null && external.cssPath() != null && !external.cssPath().isBlank() && scene != null) {
                scene.getStylesheets().add(external.cssPath());
                if (previewWebView != null) {
                    if (!previewWebView.getStyleClass().contains("webview-theme")) {
                        previewWebView.getStyleClass().add("webview-theme");
                    }
                    String bgColor = external.darkLike() ? "#00160c" : "#f5f5f5";
                    previewWebView.getEngine()
                            .executeScript("document.body.style.backgroundColor = '" + bgColor + "';");
                }
                if (getCurrentNote() != null) {
                    updatePreview();
                }
                applyRootInlineStyle();
                logger.info("Applied external theme: " + external.id());
                return;
            }
            logger.warning("External theme not available, falling back to built-in light.");
        }
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
        applyRootInlineStyle();
    }

    private String detectSystemTheme() {
        return themeCommandWorkflow.detectSystemTheme(
                this::detectWindowsTheme,
                e -> logger.log(Level.WARNING, "Could not detect macOS theme", e));
    }

    private String resolveThemeToApply() {
        if (UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL.equals(themeSource)) {
            return "external";
        }
        return themeCommandWorkflow.resolveThemeToApply(currentTheme, this::detectSystemTheme);
    }

    private boolean isDarkThemeActive() {
        if (UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL.equals(themeSource)) {
            ThemeCatalogWorkflow.ThemeDescriptor external = themeCatalogWorkflow.findById(
                    themeCatalogWorkflow.getAvailableThemes(),
                    externalThemeId);
            return external != null && external.darkLike();
        }
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
        UiPreferencesWorkflow.UiPreferences currentUiPrefs = new UiPreferencesWorkflow.UiPreferences(
                sidebarTabsMode,
                editorViewButtonsMode,
                autosaveEnabled,
                autosaveIdleMs,
                themeSource,
                externalThemeId,
                customAccentEnabled,
                customAccentColor);
        List<ThemeCatalogWorkflow.ThemeDescriptor> themes = themeCatalogWorkflow.getAvailableThemes();
        Optional<UiDialogWorkflow.PreferencesDialogResult> result = uiDialogWorkflow.showPreferences(
                this::getString,
                currentUiPrefs,
                themes);
        if (result.isEmpty()) {
            return;
        }
        UiDialogWorkflow.PreferencesDialogResult values = result.get();
        UiPreferencesWorkflow.UiPreferences newPrefs = new UiPreferencesWorkflow.UiPreferences(
                values.sidebarTabsMode(),
                values.editorButtonsMode(),
                values.autosaveEnabled(),
                values.autosaveIdleMs(),
                values.themeSource(),
                values.externalThemeId(),
                values.accentEnabled(),
                values.accentColor());
        uiPreferencesWorkflow.save(prefs, newPrefs);
        applyUiPreferencesFromStore();
        applyTheme();
        updateStatus(getString("status.preferences_saved"));
    }

    private void applyRootInlineStyle() {
        javafx.scene.Scene scene = mainSplitPane != null ? mainSplitPane.getScene() : null;
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        StringBuilder style = new StringBuilder();
        style.append("-fx-font-size: ").append(uiFontSize).append("px;");
        if (customAccentEnabled && isCustomAccentApplicable()) {
            String accent = normalizeHexColor(customAccentColor);
            String accentHover = deriveColor(accent, isDarkThemeActive() ? 1.12 : 0.88);
            String accentStrong = deriveColor(accent, 1.25);
            String accentDim = deriveColor(accent, 0.78);
            String selectedBg = accent;
            String selectedStrongBg = isDarkThemeActive() ? deriveColor(accent, 0.72) : deriveColor(accent, 0.92);
            String accentContrast = isLightColor(accent) ? "#111111" : "#ffffff";
            style.append("-fx-accent: ").append(accent).append(";");
            style.append("-fx-accent-hover: ").append(accentHover).append(";");
            style.append("-fx-accent-strong: ").append(accentStrong).append(";");
            style.append("-fx-accent-dim: ").append(accentDim).append(";");
            style.append("-fx-selected-bg: ").append(selectedBg).append(";");
            style.append("-fx-selected-strong-bg: ").append(selectedStrongBg).append(";");
            style.append("-fx-accent-contrast: ").append(accentContrast).append(";");
            style.append("-fx-focus-color: ").append(accent).append(";");
        }
        scene.getRoot().setStyle(style.toString());
    }

    private boolean isCustomAccentApplicable() {
        if (UiPreferencesWorkflow.THEME_SOURCE_BUILTIN.equals(themeSource)) {
            return true;
        }
        if (!UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL.equals(themeSource)) {
            return false;
        }
        ThemeCatalogWorkflow.ThemeDescriptor external = themeCatalogWorkflow.findById(
                themeCatalogWorkflow.getAvailableThemes(),
                externalThemeId);
        return external != null && external.supportsAccentOverride();
    }

    private String normalizeHexColor(String color) {
        if (color == null) {
            return "#7c3aed";
        }
        String candidate = color.trim();
        if (!candidate.startsWith("#")) {
            candidate = "#" + candidate;
        }
        if (candidate.matches("^#[0-9a-fA-F]{6}$")) {
            return candidate.toLowerCase();
        }
        return "#7c3aed";
    }

    private String deriveColor(String hexColor, double factor) {
        String normalized = normalizeHexColor(hexColor);
        int r = Integer.parseInt(normalized.substring(1, 3), 16);
        int g = Integer.parseInt(normalized.substring(3, 5), 16);
        int b = Integer.parseInt(normalized.substring(5, 7), 16);
        int rr = (int) Math.max(0, Math.min(255, Math.round(r * factor)));
        int gg = (int) Math.max(0, Math.min(255, Math.round(g * factor)));
        int bb = (int) Math.max(0, Math.min(255, Math.round(b * factor)));
        return String.format("#%02x%02x%02x", rr, gg, bb);
    }

    private boolean isLightColor(String hexColor) {
        String normalized = normalizeHexColor(hexColor);
        int r = Integer.parseInt(normalized.substring(1, 3), 16);
        int g = Integer.parseInt(normalized.substring(3, 5), 16);
        int b = Integer.parseInt(normalized.substring(5, 7), 16);
        double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
        return luminance > 0.62;
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
