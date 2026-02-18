package com.example.forevernote.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import org.kordamp.ikonli.javafx.FontIcon;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.SQLiteDB;
import com.example.forevernote.data.dao.abstractLayers.FactoryDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.NoteEvents;
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

import javafx.stage.Stage;

import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.sql.Connection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import javafx.stage.FileChooser;

/**
 * Main controller for the Forevernote application.
 * Handles all UI interactions and manages the application state.
 * Implements PluginMenuRegistry and SidePanelRegistry to allow plugins to
 * register
 * menu items and UI panels dynamically (Obsidian-style).
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
    private Note currentNote;
    private boolean isModified = false;
    private String currentFilterType = "all"; // "all", "folder", "tag", "favorites", "search"
    private Tag currentTag = null;

    // Cached data to avoid recreating listeners
    private List<Note> cachedAllNotes = new ArrayList<>();
    private List<Note> cachedFavoriteNotes = new ArrayList<>();
    private List<Note> cachedTrashNotes = new ArrayList<>();
    private boolean recentListenerAdded = false;
    private boolean favoritesListenerAdded = false;
    private boolean trashListenerAdded = false;

    // FXML UI Components
    @FXML
    private MenuBar menuBar;
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private SplitPane contentSplitPane;
    @FXML
    private SplitPane editorPreviewSplitPane;
    @FXML
    private TabPane navigationTabPane;

    // Collapsible Panels
    @FXML
    private VBox sidebarPane;
    @FXML
    private VBox notesPanel;
    @FXML
    private Label notesPanelTitleLabel;
    @FXML
    private VBox editorContainer;

    // Navigation components
    @FXML
    private TreeView<Folder> folderTreeView;
    @FXML
    private ListView<String> tagListView;
    @FXML
    private ListView<String> recentNotesListView;
    @FXML
    private ListView<String> favoritesListView;
    @FXML
    private ListView<String> trashListView;

    // Layout State
    private boolean isStackedLayout = false;
    private SplitPane navSplitPane;

    // Search and toolbar
    // Search and toolbar
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private ToggleButton listViewButton;
    @FXML
    private ToggleButton gridViewButton;
    @FXML
    private ToggleButton sidebarToggleBtn;
    @FXML
    private ToggleButton notesPanelToggleBtn;
    @FXML
    private Button layoutSwitchBtn;
    @FXML
    private Button newTagBtn;

    // Notes list
    @FXML
    private ListView<Note> notesListView;

    // Editor components
    @FXML
    private TextField noteTitleField;
    @FXML
    private TextArea noteContentArea;
    @FXML
    private FlowPane tagsFlowPane;
    @FXML
    private VBox tagsContainer;
    @FXML
    private ToggleButton toggleTagsBtn;
    @FXML
    private Label modifiedDateLabel;
    @FXML
    private Label wordCountLabel;

    // Editor/Preview panes (Obsidian-style)
    @FXML
    private VBox editorPane;
    @FXML
    private VBox previewPane;
    @FXML
    private ToggleButton editorOnlyButton;
    @FXML
    private ToggleButton splitViewButton;
    @FXML
    private ToggleButton previewOnlyButton;
    @FXML
    private ToggleButton favoriteButton;
    @FXML
    private ToggleButton pinButton;
    @FXML
    private ToggleButton infoButton;

    // Sidebar buttons removed

    // Toolbar buttons
    @FXML
    private Button newNoteBtn;
    @FXML
    private Button newFolderBtn;
    @FXML
    private Button saveBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button refreshBtn;
    @FXML
    private HBox stackedModeHeader;
    @FXML
    private HBox toolbarHBox;
    @FXML
    private MenuButton toolbarOverflowBtn;
    @FXML
    private Separator toolbarSeparator1;

    // Preview Enhancers
    private final Map<String, PreviewEnhancer> previewEnhancers = new HashMap<>();
    @FXML
    private Separator toolbarSeparator2;
    @FXML
    private Separator toolbarSeparator3;

    // Format toolbar buttons
    @FXML
    private Button heading1Btn;
    @FXML
    private Button heading2Btn;
    @FXML
    private Button heading3Btn;
    @FXML
    private Button boldBtn;
    @FXML
    private Button italicBtn;
    @FXML
    private Button strikeBtn;
    @FXML
    private Button underlineBtn;
    @FXML
    private Button highlightBtn;
    @FXML
    private Button linkBtn;
    @FXML
    private Button imageBtn;
    @FXML
    private Button todoBtn;
    @FXML
    private Button bulletBtn;
    @FXML
    private Button numberBtn;
    @FXML
    private Button quoteBtn;
    @FXML
    private Button codeBtn;
    @FXML
    private Button closeRightPanelBtn;

    // Right panel (Obsidian-style with collapsible sections)
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
    private javafx.scene.web.WebView previewWebView;
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

    @FXML
    private RadioMenuItem lightThemeMenuItem;
    @FXML
    private RadioMenuItem darkThemeMenuItem;
    @FXML
    private RadioMenuItem systemThemeMenuItem;
    @FXML
    private RadioMenuItem englishLangMenuItem;
    @FXML
    private RadioMenuItem spanishLangMenuItem;

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

            // Initialize theme and language groups
            initializeThemeMenu();
            initializeLanguageMenu();

            // Initialize UI components
            initializeFolderTree();
            initializeNotesList();
            initializeEditor();
            initializeSearch();
            initializeSortOptions();
            initializeViewModeButtons();
            initializeIcons();
            initializeRightPanelSections();
            setupToolbarResponsiveness();

            // Load initial data
            loadFolders();
            loadRecentNotes();
            loadTags();
            loadFavorites();
            loadTrashNotes();

            // Initialize keyboard shortcuts after scene is ready
            Platform.runLater(this::initializeKeyboardShortcuts);

            // Initialize plugin system after scene is ready
            Platform.runLater(this::initializePluginSystem);

            updateStatus("Ready");
            logger.info("MainController initialized successfully");

        } catch (Exception e) {
            logger.severe("Failed to initialize MainController: " + e.getMessage());
            updateStatus("Error: " + e.getMessage());
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
        ButtonType fsDefaultBtn = new ButtonType(getString("pref.storage.filesystem_default"));
        ButtonType fsCustomBtn = new ButtonType(getString("pref.storage.filesystem_custom"));
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(sqliteBtn, fsDefaultBtn, fsCustomBtn, cancelBtn);

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
            } else if (result.get() == fsDefaultBtn) {
                newType = "filesystem";
                customPath = ""; // Empty means default
                changed = !newType.equals(currentType) || !customPath.equals(currentPath);
            } else if (result.get() == fsCustomBtn) {
                javafx.stage.DirectoryChooser directoryChooser = new javafx.stage.DirectoryChooser();
                directoryChooser.setTitle(getString("pref.storage.browse"));

                // Set initial directory if exists
                if (!currentPath.isEmpty()) {
                    File initialDir = new File(currentPath);
                    if (initialDir.exists()) {
                        directoryChooser.setInitialDirectory(initialDir);
                    }
                }

                File selectedDirectory = directoryChooser.showDialog(menuBar.getScene().getWindow());
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
     * Initialize the folder tree view.
     */
    private void initializeFolderTree() {
        // Create a visible root folder for "All Notes" (like Evernote/Joplin/Obsidian)
        String rootTitle = getString("app.all_notes");
        Folder rootFolder = new Folder(rootTitle, null, null);
        TreeItem<Folder> rootItem = new TreeItem<>(rootFolder);
        rootItem.setExpanded(true);
        folderTreeView.setRoot(rootItem);
        folderTreeView.setShowRoot(true);

        // Handle folder selection
        folderTreeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.getValue() != null) {
                        Folder selectedFolder = newValue.getValue();
                        if (selectedFolder.getTitle().equals(rootTitle) ||
                                selectedFolder.getTitle().equals("All Notes") ||
                                selectedFolder.getTitle().endsWith("All Notes")) {
                            currentFolder = null;
                            loadAllNotes();
                        } else {
                            handleFolderSelection(selectedFolder);
                        }
                    } else {
                        currentFolder = null;
                        loadAllNotes();
                    }
                });

        // Cell factory is set in setupFolderTreeDragAndDrop to include both D&D and
        // context menus
    }

    /**
     * Get the count of notes in a folder (including subfolders recursively).
     */
    /**
     * Get the count of notes in a folder.
     * Optimization: Recursive counting is extremely slow for FileSystem large
     * vaults.
     * We will only count ALL notes for the Root. For subfolders, we return -1
     * (hidden) or 0 to save performance.
     */
    private int getNoteCountForFolder(Folder folder) {
        try {
            if (folder == null)
                return 0;

            // "All Notes" check
            if (folder.getId() == null || folder.getTitle().equals("All Notes")
                    || folder.getTitle().equals("Todas las Notas") || "ROOT".equals(folder.getId())) {
                List<Note> allNotes = noteDAO.fetchAllNotes();
                return allNotes != null ? allNotes.size() : 0;
            }

            // For other folders, return 0 to avoid massive FS recursion lag.
            // If users REALLY want counts, we can implement an async background counter
            // later.
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Initialize the notes list view with drag & drop support.
     */
    private void initializeNotesList() {
        notesListView.setCellFactory(lv -> createNoteListCell());

        // Handle note selection
        notesListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        loadNoteInEditor(newValue);
                    }
                });

        // Setup folder tree for drop target
        setupFolderTreeDragAndDrop();
    }

    /**
     * Creates a ListCell for notes with drag support and context menu.
     */
    private ListCell<Note> createNoteListCell() {
        ListCell<Note> cell = new ListCell<>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    VBox container = new VBox(2);
                    container.getStyleClass().add("note-cell-container");
                    container.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));

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

                    Label titleLabel = new Label(note.getTitle());
                    titleLabel.getStyleClass().add("note-cell-title");
                    titleRow.getChildren().add(titleLabel);

                    String preview = note.getContent() != null && !note.getContent().isEmpty()
                            ? note.getContent().replaceAll("^#+\\s*", "").replaceAll("\\n", " ").trim()
                            : "";
                    if (preview.length() > 60) {
                        preview = preview.substring(0, 57) + "...";
                    }
                    Label previewLabel = new Label(preview);
                    previewLabel.getStyleClass().add("note-cell-preview");

                    String dateText = note.getModifiedDate() != null ? note.getModifiedDate() : note.getCreatedDate();
                    if (dateText != null && dateText.length() > 10) {
                        dateText = dateText.substring(0, 10);
                    }
                    Label dateLabel = new Label(dateText != null ? dateText : "");
                    dateLabel.getStyleClass().add("note-cell-date");

                    container.getChildren().addAll(titleRow, previewLabel, dateLabel);
                    setGraphic(container);
                    setText(null);

                    // Context Menu
                    setContextMenu(createNoteContextMenu(note));
                }
            }
        };

        // Setup drag for this cell
        setupNoteCellDrag(cell);

        return cell;
    }

    /**
     * Setup drag events for note cell.
     */
    private void setupNoteCellDrag(ListCell<Note> cell) {
        // Start drag
        cell.setOnDragDetected(event -> {
            Note note = cell.getItem();
            if (note != null) {
                javafx.scene.input.Dragboard db = cell.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString("note:" + note.getId());
                db.setContent(content);

                // Create drag image
                javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                params.setFill(javafx.scene.paint.Color.TRANSPARENT);
                db.setDragView(cell.snapshot(params, null));

                event.consume();
                updateStatus("Dragging: " + note.getTitle());
            }
        });

        cell.setOnDragDone(event -> {
            if (event.getTransferMode() == javafx.scene.input.TransferMode.MOVE) {
                updateStatus("Note moved successfully");
            }
            event.consume();
        });
    }

    /**
     * Setup folder tree view with context menus and drag & drop support.
     */
    private void setupFolderTreeDragAndDrop() {
        folderTreeView.setCellFactory(tv -> {
            TreeCell<Folder> cell = new TreeCell<>() {
                @Override
                protected void updateItem(Folder folder, boolean empty) {
                    super.updateItem(folder, empty);
                    if (empty || folder == null) {
                        setText(null);
                        setGraphic(null);
                        setContextMenu(null);
                    } else {
                        Label iconLabel = new Label("");
                        iconLabel.getStyleClass().setAll("folder-cell-icon"); // Use setAll to clear previous classes

                        String rootTitle = getString("app.all_notes");
                        boolean isRoot = folder.getTitle().equals("All Notes") || folder.getTitle().equals(rootTitle)
                                || folder.getId() == null;

                        if (isRoot) {
                            iconLabel.setText("[=]");
                            iconLabel.getStyleClass().add("folder-all-notes");
                        } else {
                            TreeItem<Folder> ti = getTreeItem();
                            boolean isExpanded = ti != null && ti.isExpanded();
                            iconLabel.setText(isExpanded ? "[/]" : "[+]");
                            iconLabel.getStyleClass().add(isExpanded ? "folder-expanded" : "folder-collapsed");
                        }

                        int noteCount = 0;
                        try {
                            if (!isRoot) {
                                noteCount = getNoteCountForFolder(folder);
                            } else {
                                noteCount = noteDAO.fetchAllNotes().size();
                            }
                        } catch (Exception e) {
                        }

                        HBox container = new HBox(6);
                        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        Label nameLabel = new Label(folder.getTitle());
                        nameLabel.getStyleClass().add("folder-cell-name");

                        if (noteCount > 0 || isRoot) {
                            Label countLabel = new Label("(" + noteCount + ")");
                            countLabel.getStyleClass().add("folder-cell-count");
                            container.getChildren().addAll(iconLabel, nameLabel, countLabel);
                        } else {
                            container.getChildren().addAll(iconLabel, nameLabel);
                        }

                        setGraphic(container);
                        setText(null);

                        // Context Menu
                        if (!isRoot) {
                            setContextMenu(createFolderContextMenu(folder));
                        } else {
                            setContextMenu(null);
                        }
                    }
                }
            };

            // D&D Logic
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()
                        && event.getDragboard().getString().startsWith("note:")) {
                    Folder folder = cell.getItem();
                    if (folder != null && !folder.getTitle().equals("All Notes")) {
                        event.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
                        cell.getStyleClass().add("folder-cell-drag-over");
                    }
                }
                event.consume();
            });

            cell.setOnDragExited(event -> {
                cell.getStyleClass().remove("folder-cell-drag-over");
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                javafx.scene.input.Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString() && db.getString().startsWith("note:")) {
                    Folder targetFolder = cell.getItem();
                    if (targetFolder != null && !targetFolder.getTitle().equals("All Notes")) {
                        try {
                            String noteId = db.getString().substring(5);
                            Note note = noteDAO.getNoteById(noteId);
                            if (note != null) {
                                folderDAO.addNote(targetFolder, note);
                                success = true;
                                Platform.runLater(() -> {
                                    refreshNotesList();
                                    loadFolders();
                                });
                            }
                        } catch (Exception e) {
                            logger.warning("Failed to move note: " + e.getMessage());
                        }
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return cell;
        });
    }

    /**
     * Initialize the note editor.
     */
    private void initializeEditor() {
        // Auto-save functionality
        noteContentArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (currentNote != null) {
                currentNote.setContent(newValue);
                isModified = true;
                updateWordCount();
                updatePreview();
            }
        });

        noteTitleField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (currentNote != null) {
                currentNote.setTitle(newValue);
                isModified = true;
            }
        });

        // Keyboard shortcuts
        noteContentArea.setOnKeyPressed(this::handleEditorKeyPress);

        // Setup WebView to ensure dark background
        if (previewWebView != null) {
            previewWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    // Ensure background is set after content loads
                    String actualTheme = currentTheme;
                    if ("system".equals(currentTheme)) {
                        actualTheme = detectSystemTheme();
                    }
                    if ("dark".equals(actualTheme)) {
                        previewWebView.getEngine().executeScript(
                                "document.body.style.backgroundColor = '#1E1E1E'; " +
                                        "if (document.documentElement) document.documentElement.style.backgroundColor = '#1E1E1E';");
                    } else {
                        previewWebView.getEngine().executeScript(
                                "document.body.style.backgroundColor = '#FFFFFF'; " +
                                        "if (document.documentElement) document.documentElement.style.backgroundColor = '#FFFFFF';");
                    }
                }
            });
        }
    }

    /**
     * Initialize search functionality.
     */
    private void initializeSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            performSearch(newValue);
        });
    }

    /**
     * Initialize theme menu with toggle group.
     */
    private void initializeThemeMenu() {
        themeToggleGroup = new ToggleGroup();
        if (lightThemeMenuItem != null) {
            lightThemeMenuItem.setToggleGroup(themeToggleGroup);
        }
        if (darkThemeMenuItem != null) {
            darkThemeMenuItem.setToggleGroup(themeToggleGroup);
        }
        if (systemThemeMenuItem != null) {
            systemThemeMenuItem.setToggleGroup(themeToggleGroup);
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
        if (englishLangMenuItem != null) {
            englishLangMenuItem.setToggleGroup(languageToggleGroup);
        }
        if (spanishLangMenuItem != null) {
            spanishLangMenuItem.setToggleGroup(languageToggleGroup);
        }

        String currentLang = prefs.get("language", java.util.Locale.getDefault().getLanguage());
        if (currentLang.startsWith("es")) {
            if (spanishLangMenuItem != null)
                spanishLangMenuItem.setSelected(true);
        } else {
            if (englishLangMenuItem != null)
                englishLangMenuItem.setSelected(true);
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
            if (darkThemeMenuItem != null) {
                darkThemeMenuItem.setSelected(true);
            }
        } else if ("system".equals(currentTheme)) {
            if (systemThemeMenuItem != null) {
                systemThemeMenuItem.setSelected(true);
            }
        } else {
            if (lightThemeMenuItem != null) {
                lightThemeMenuItem.setSelected(true);
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
            if (menuBar != null && menuBar.getScene() != null) {
                scene = menuBar.getScene();
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
            boolean isDark = "dark".equals(currentTheme) ||
                    ("system".equals(currentTheme) && "dark".equals(detectSystemTheme()));
            commandPalette.setDarkTheme(isDark);
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
            boolean isDark = "dark".equals(currentTheme) ||
                    ("system".equals(currentTheme) && "dark".equals(detectSystemTheme()));
            commandPalette.setDarkTheme(isDark);
            commandPalette.show();
            logger.info("Command Palette opened");
        } else {
            logger.warning("Command Palette not initialized yet");
            updateStatus("Command Palette not ready. Please wait...");
        }
    }

    /**
     * Shows the Quick Switcher (Ctrl+O).
     */
    public void showQuickSwitcher() {
        if (quickSwitcher != null) {
            boolean isDark = "dark".equals(currentTheme) ||
                    ("system".equals(currentTheme) && "dark".equals(detectSystemTheme()));
            quickSwitcher.setDarkTheme(isDark);
            // Update notes list before showing
            quickSwitcher.setNotes(noteDAO.fetchAllNotes());
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

            // Create the plugin manager dialog
            Stage stage = (Stage) menuBar.getScene().getWindow();
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
    private void subscribeToPluginEvents() {
        // Listen for note open requests from plugins
        eventBus.subscribe(NoteEvents.NoteOpenRequestEvent.class, event -> {
            Platform.runLater(() -> {
                Note note = event.getNote();
                if (note != null) {
                    loadNoteInEditor(note);
                    // Also refresh notes list to show the note
                    loadAllNotes();
                    notesListView.getSelectionModel().select(note);
                    logger.info("Opened note from plugin: " + note.getTitle());
                }
            });
        });

        // Listen for notes refresh requests from plugins
        eventBus.subscribe(NoteEvents.NotesRefreshRequestedEvent.class, event -> {
            Platform.runLater(() -> {
                loadFolders();
                loadAllNotes();
                loadRecentNotes();
                loadTags();
                loadFavorites();
                logger.info("Refreshed notes from plugin request");
            });
        });
    }

    /**
     * Shows the Plugin Manager dialog (Obsidian-style).
     */
    public void showPluginManager() {
        if (pluginManagerDialog != null) {
            boolean isDark = "dark".equals(currentTheme) ||
                    ("system".equals(currentTheme) && "dark".equals(detectSystemTheme()));
            pluginManagerDialog.setDarkTheme(isDark);
            pluginManagerDialog.show();
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Plugin Manager");
            alert.setHeaderText("Plugin system not initialized");
            alert.setContentText("Please restart the application.");
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
            if (pluginsMenu == null) {
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
                int insertIndex = Math.min(pluginsMenu.getItems().size(), 2);
                pluginsMenu.getItems().add(insertIndex, categoryMenu);
            }

            // Create the menu item
            MenuItem menuItem = new MenuItem(itemName);
            menuItem.setOnAction(e -> {
                if (pluginManager != null && pluginManager.isPluginEnabled(pluginId)) {
                    action.run();
                } else {
                    updateStatus("Plugin '" + pluginId + "' is not enabled");
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
                        pluginsMenu.getItems().remove(menu);
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

    // ==================== SIDE PANEL REGISTRY IMPLEMENTATION (Obsidian-style UI)
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
     * All plugins (including built-in ones) must be in plugins/ as JAR files.
     * The core application is completely decoupled from specific plugins.
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
                searchField.requestFocus();
                break;
            case "Go to All Notes":
                loadAllNotes();
                break;
            case "Go to Favorites":
                loadFavorites();
                break;
            case "Go to Recent":
                loadRecentNotes();
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
     * Initialize view mode toggle buttons (Obsidian-style).
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
        if (listViewButton != null) {
            listViewButton.setToggleGroup(notesViewGroup);
            listViewButton.setSelected(true);
        }
        if (gridViewButton != null) {
            gridViewButton.setToggleGroup(notesViewGroup);
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
        if (toolbarHBox == null || toolbarOverflowBtn == null)
            return;

        toolbarHBox.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateToolbarOverflow(newVal.doubleValue());
        });

        // Initial check
        Platform.runLater(() -> updateToolbarOverflow(toolbarHBox.getWidth()));
    }

    /**
     * Update toolbar items based on available width.
     */
    private void updateToolbarOverflow(double width) {
        if (toolbarHBox == null || toolbarOverflowBtn == null)
            return;

        // Thresholds for different buttons (cumulative widths approx)
        boolean showSearch = width > 750;
        boolean showFileActions = width > 550;
        boolean showLayoutToggles = width > 400;

        // File Actions (New, Folder, Tag, Save, Delete)
        newNoteBtn.setVisible(showFileActions);
        newNoteBtn.setManaged(showFileActions);
        newFolderBtn.setVisible(showFileActions);
        newFolderBtn.setManaged(showFileActions);
        newTagBtn.setVisible(showFileActions);
        newTagBtn.setManaged(showFileActions);
        saveBtn.setVisible(showFileActions);
        saveBtn.setManaged(showFileActions);
        deleteBtn.setVisible(showFileActions);
        deleteBtn.setManaged(showFileActions);

        // Update separator 2 (between file actions and save/delete) - actually between
        // file actions group and search
        toolbarSeparator2.setVisible(showFileActions);
        toolbarSeparator2.setManaged(showFileActions);

        // Search Field
        searchField.setVisible(showSearch);
        searchField.setManaged(showSearch);
        toolbarSeparator3.setVisible(showSearch);
        toolbarSeparator3.setManaged(showSearch);

        // Layout Toggles
        sidebarToggleBtn.setVisible(showLayoutToggles);
        sidebarToggleBtn.setManaged(showLayoutToggles);
        notesPanelToggleBtn.setVisible(showLayoutToggles);
        notesPanelToggleBtn.setManaged(showLayoutToggles);
        layoutSwitchBtn.setVisible(showLayoutToggles);
        layoutSwitchBtn.setManaged(showLayoutToggles);
        toolbarSeparator1.setVisible(showLayoutToggles);
        toolbarSeparator1.setManaged(showLayoutToggles);

        // Manage Overflow Menu
        toolbarOverflowBtn.getItems().clear();
        boolean needsOverflow = !showFileActions || !showSearch || !showLayoutToggles;

        if (needsOverflow) {
            if (!showSearch) {
                MenuItem searchItem = new MenuItem(getString("app.search.placeholder"));
                searchItem.setOnAction(e -> searchField.requestFocus());
                toolbarOverflowBtn.getItems().add(searchItem);
                toolbarOverflowBtn.getItems().add(new SeparatorMenuItem());
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
                toolbarOverflowBtn.getItems().addAll(newNoteItem, newFolderItem, newTagItem, saveItem,
                        new SeparatorMenuItem(), deleteItem);
            }
            if (!showLayoutToggles) {
                if (!toolbarOverflowBtn.getItems().isEmpty())
                    toolbarOverflowBtn.getItems().add(new SeparatorMenuItem());
                MenuItem toggleSidebar = new MenuItem(getString("action.toggle_sidebar"));
                toggleSidebar.setOnAction(e -> handleToggleSidebar(null));
                MenuItem toggleNotes = new MenuItem(getString("action.toggle_notes_list"));
                toggleNotes.setOnAction(e -> handleToggleNotesPanel(null));
                MenuItem switchLayout = new MenuItem(getString("action.switch_layout"));
                switchLayout.setOnAction(e -> handleViewLayoutSwitch(null));
                toolbarOverflowBtn.getItems().addAll(toggleSidebar, toggleNotes, switchLayout);
            }

            toolbarOverflowBtn.setVisible(true);
            toolbarOverflowBtn.setManaged(true);
        } else {
            toolbarOverflowBtn.setVisible(false);
            toolbarOverflowBtn.setManaged(false);
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
        boolean isDark = "dark".equals(currentTheme) ||
                ("system".equals(currentTheme) && "dark".equals(detectSystemTheme()));

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

            if (nextVisible && currentNote != null) {
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
        if (currentNote == null)
            return;

        if (infoCreatedLabel != null && currentNote.getCreatedDate() != null) {
            infoCreatedLabel.setText(currentNote.getCreatedDate().toString());
        }
        if (infoModifiedLabel != null && currentNote.getModifiedDate() != null) {
            infoModifiedLabel.setText(currentNote.getModifiedDate().toString());
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
            String latVal = currentNote.getLatitude() != 0 ? String.valueOf(currentNote.getLatitude()) : "-";
            infoLatitudeLabel.setText(java.text.MessageFormat.format(getString("info.lat"), latVal));
        }
        if (infoLongitudeLabel != null) {
            String lonVal = currentNote.getLongitude() != 0 ? String.valueOf(currentNote.getLongitude()) : "-";
            infoLongitudeLabel.setText(java.text.MessageFormat.format(getString("info.lon"), lonVal));
        }
        if (infoAuthorLabel != null) {
            String authorVal = (currentNote.getAuthor() != null && !currentNote.getAuthor().isEmpty())
                    ? currentNote.getAuthor()
                    : "-";
            infoAuthorLabel.setText(java.text.MessageFormat.format(getString("info.author"), authorVal));
        }
        if (infoSourceUrlLabel != null) {
            String sourceVal = (currentNote.getSourceUrl() != null && !currentNote.getSourceUrl().isEmpty())
                    ? currentNote.getSourceUrl()
                    : "-";
            infoSourceUrlLabel.setText(java.text.MessageFormat.format(getString("info.source"), sourceVal));
        }
    }

    /**
     * Load folders into the tree view.
     */
    private void loadFolders() {
        try {
            TreeItem<Folder> root = folderTreeView.getRoot();
            root.getChildren().clear();

            // Only load root folders (folders with parent_id IS NULL)
            // We need to check parent_id directly from database since getParent() may not
            // be loaded
            List<Folder> allFolders = folderDAO.fetchAllFoldersAsList();
            List<Folder> rootFolders = new ArrayList<>();

            for (Folder folder : allFolders) {
                // Check if folder has a parent by querying the database
                Folder parent = folderDAO.getParentFolder(folder.getId());
                if (parent == null) {
                    // This is a root folder
                    rootFolders.add(folder);
                }
            }

            for (Folder folder : rootFolders) {
                TreeItem<Folder> folderItem = new TreeItem<>(folder);
                root.getChildren().add(folderItem);
                loadSubFolders(folderItem, folder);
            }

            logger.info("Loaded " + rootFolders.size() + " root folders");
        } catch (Exception e) {
            logger.severe("Failed to load folders: " + e.getMessage());
            updateStatus(getString("status.error_loading_folders"));
        }
    }

    /**
     * Load subfolders recursively.
     */
    private void loadSubFolders(TreeItem<Folder> parentItem, Folder parentFolder) {
        try {
            folderDAO.loadSubFolders(parentFolder);
            for (Component child : parentFolder.getChildren()) {
                if (child instanceof Folder) {
                    Folder childFolder = (Folder) child;
                    TreeItem<Folder> childItem = new TreeItem<>(childFolder);
                    parentItem.getChildren().add(childItem);
                    loadSubFolders(childItem, childFolder);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load subfolders for " + parentFolder.getTitle() + ": " + e.getMessage());
        }
    }

    /**
     * Load all notes.
     */
    private void loadAllNotes() {
        try {
            List<Note> notes = noteDAO.fetchAllNotes();
            notesListView.getItems().setAll(notes);
            sortNotes(sortComboBox.getValue());
            noteCountLabel.setText(java.text.MessageFormat.format(getString("info.notes_count"), notes.size()));
            currentFolder = null;
            currentTag = null;
            currentFilterType = "all";

            // Refresh grid view if active
            if (currentNotesViewMode == NotesViewMode.GRID) {
                Platform.runLater(this::refreshGridView);
            }

            updateStatus(getString("status.loaded_all"));
        } catch (Exception e) {
            logger.severe("Failed to load all notes: " + e.getMessage());
            updateStatus(getString("status.error_loading"));
        }
    }

    /**
     * Load notes for selected folder.
     */
    private void handleFolderSelection(Folder folder) {
        try {
            if (folder == null) {
                loadAllNotes();
                return;
            }

            // Reload folder from database to ensure we have the latest data
            Folder loadedFolder = folderDAO.getFolderById(folder.getId());
            if (loadedFolder != null) {
                currentFolder = loadedFolder;

                // DATA ACCESS FIX: Explicitly use NoteDAO to fetch notes for the folder
                // This ensures we get the latest file list directly from disk (via our
                // optimized method)
                List<Note> notes = noteDAO.fetchNotesByFolderId(currentFolder.getId());

                // Add notes to the folder object implicitly for UI consistency if needed,
                // but primarily use the returned list for the ListView.
                currentFolder.getChildren().removeIf(c -> c instanceof Note);
                currentFolder.addAll(new ArrayList<>(notes));

                notesListView.getItems().setAll(notes);
                sortNotes(sortComboBox.getValue());
                noteCountLabel.setText(notes.size() + " notes");
                currentFilterType = "folder";
                currentTag = null;

                if (currentNotesViewMode == NotesViewMode.GRID) {
                    Platform.runLater(this::refreshGridView);
                }

                // Ensure notes panel is visible when folder is selected (especially in Stacked
                // Mode)
                if (notesPanel != null) {
                    if (isStackedLayout) {
                        // In stacked layout, ensure navSplitPane is 50/50 (or reasonable split) to show
                        // notes
                        if (navSplitPane != null) {
                            // Check if notes might be hidden (divider roughly at 1.0 or notesPanel
                            // collapsed)
                            if (notesPanel.getMaxWidth() < 10 || navSplitPane.getDividerPositions().length > 0
                                    && navSplitPane.getDividerPositions()[0] > 0.95) {
                                notesPanel.setMinWidth(180);
                                notesPanel.setMaxWidth(Double.MAX_VALUE);
                                navSplitPane.setDividerPositions(0.5); // Show split
                            }
                        }
                    } else {
                        // In column layout, ensure contentSplitPane shows notes
                        if (contentSplitPane != null) {
                            if (notesPanel.getMaxWidth() < 10) {
                                notesPanel.setMinWidth(180);
                                notesPanel.setMaxWidth(Double.MAX_VALUE);
                                contentSplitPane.setDividerPositions(0.25);
                            }
                        }
                    }
                    if (notesPanelToggleBtn != null)
                        notesPanelToggleBtn.setSelected(true);
                }

                updateStatus(
                        java.text.MessageFormat.format(getString("status.loaded_folder"), currentFolder.getTitle()));
                if (notesPanelTitleLabel != null) {
                    notesPanelTitleLabel.setText(getString("panel.notes.title") + " - " + currentFolder.getTitle());
                }
            }
        } catch (Exception e) {
            logger.severe(
                    "Failed to load folder " + (folder != null ? folder.getTitle() : "null") + ": " + e.getMessage());
            updateStatus(getString("status.error_loading_folder"));
        }
    }

    /**
     * Load note in editor.
     */
    private void loadNoteInEditor(Note note) {
        if (isModified && currentNote != null) {
            // Ask to save changes
            Optional<ButtonType> result = showSaveDialog();
            if (result.isPresent()) {
                if (result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                    return;
                }
                if (result.get().getText().equals("Save")) {
                    handleSave(new ActionEvent());
                }
            }
        }

        // OPTIMIZATION: The note object passed from the list might be a "lightweight"
        // note without content.
        // We MUST reload the full note data from the DAO using the ID to get the
        // content.
        Note fullNote = noteDAO.getNoteById(note.getId());
        if (fullNote != null) {
            currentNote = fullNote;
        } else {
            // Fallback if load fails (shouldn't happen if file exists)
            currentNote = note;
        }

        noteTitleField.setText(currentNote.getTitle() != null ? currentNote.getTitle() : "");
        noteContentArea.setText(currentNote.getContent() != null ? currentNote.getContent() : "");

        // Load tags
        loadNoteTags(note);

        // Update metadata
        updateNoteMetadata(note);

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
        loadFavorites();

        // Publish event for plugins (Outline, etc.)
        if (eventBus != null) {
            eventBus.publish(new NoteEvents.NoteSelectedEvent(note));
        }

        isModified = false;
        updateStatus(java.text.MessageFormat.format(getString("status.note_loaded"), note.getTitle()));
    }

    /**
     * Load tags for a note.
     */
    private void loadNoteTags(Note note) {
        try {
            List<Tag> tags = noteDAO.fetchTags(note.getId());
            tagsFlowPane.getChildren().clear();

            for (Tag tag : tags) {
                // Create tag label with X button for removal (like Evernote/Obsidian)
                HBox tagContainer = new HBox(4);
                tagContainer.getStyleClass().add("tag-container");
                tagContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label tagLabel = new Label(tag.getTitle());
                tagLabel.getStyleClass().add("tag-label");

                // X button to remove tag (more intuitive than double-click)
                Button removeBtn = new Button("×");
                removeBtn.getStyleClass().add("tag-remove-btn");
                removeBtn.setTooltip(new Tooltip("Remove tag from note"));

                // Store tag ID to ensure it's accessible when removing
                final String tagId = tag.getId();
                final String tagTitle = tag.getTitle();
                removeBtn.setOnAction(e -> {
                    Tag tagToRemove = new Tag(tagId, tagTitle, null, null);
                    removeTagFromNote(tagToRemove);
                });

                // Also support double-click on label for removal (backward compatibility)
                tagLabel.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        Tag tagToRemove = new Tag(tagId, tagTitle, null, null);
                        removeTagFromNote(tagToRemove);
                    }
                });
                tagLabel.setTooltip(new Tooltip("Double-click to remove"));

                tagContainer.getChildren().addAll(tagLabel, removeBtn);
                tagsFlowPane.getChildren().add(tagContainer);
            }

            // Add button to add new tag
            Button addTagButton = new Button("+ Add Tag");
            addTagButton.getStyleClass().add("add-tag-button");
            addTagButton.setOnAction(e -> handleAddTagToNote());
            tagsFlowPane.getChildren().add(addTagButton);
        } catch (Exception e) {
            logger.warning("Failed to load tags for note " + note.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Update note metadata display.
     */
    private void updateNoteMetadata(Note note) {
        // Update the modified date in the tags bar (subtle format)
        if (modifiedDateLabel != null) {
            String modifiedText = note.getModifiedDate() != null
                    ? "Modified " + note.getModifiedDate()
                    : "";
            modifiedDateLabel.setText(modifiedText);
        }

        // Update info panel labels
        if (infoCreatedLabel != null) {
            infoCreatedLabel.setText(note.getCreatedDate() != null ? note.getCreatedDate() : "-");
        }
        if (infoModifiedLabel != null) {
            infoModifiedLabel.setText(note.getModifiedDate() != null ? note.getModifiedDate() : "-");
        }

        String content = note.getContent() != null ? note.getContent() : "";
        if (infoWordsLabel != null) {
            infoWordsLabel.setText(String.valueOf(countWords(content)));
        }
        if (infoCharsLabel != null) {
            infoCharsLabel.setText(String.valueOf(content.length()));
        }
        if (infoLatitudeLabel != null) {
            String latVal = note.getLatitude() != 0 ? String.valueOf(note.getLatitude()) : "-";
            infoLatitudeLabel.setText(java.text.MessageFormat.format(getString("info.lat"), latVal));
        }
        if (infoLongitudeLabel != null) {
            String lonVal = note.getLongitude() != 0 ? String.valueOf(note.getLongitude()) : "-";
            infoLongitudeLabel.setText(java.text.MessageFormat.format(getString("info.lon"), lonVal));
        }
        if (infoAuthorLabel != null) {
            String authorVal = (note.getAuthor() != null && !note.getAuthor().isEmpty()) ? note.getAuthor() : "-";
            infoAuthorLabel.setText(java.text.MessageFormat.format(getString("info.author"), authorVal));
        }
        if (infoSourceUrlLabel != null) {
            String sourceVal = (note.getSourceUrl() != null && !note.getSourceUrl().isEmpty()) ? note.getSourceUrl()
                    : "-";
            infoSourceUrlLabel.setText(java.text.MessageFormat.format(getString("info.source"), sourceVal));
        }
    }

    /**
     * Load recent notes.
     */
    private void loadRecentNotes() {
        try {
            cachedAllNotes = noteDAO.fetchAllNotes();
            // Sort by modified date (simplified)
            cachedAllNotes.sort((a, b) -> {
                String dateA = a.getModifiedDate() != null ? a.getModifiedDate()
                        : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String dateB = b.getModifiedDate() != null ? b.getModifiedDate()
                        : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                return dateB.compareTo(dateA);
            });

            List<String> recentTitles = cachedAllNotes.stream()
                    .limit(10)
                    .map(Note::getTitle)
                    .toList();

            recentNotesListView.getItems().setAll(recentTitles);

            // Add listener only once
            if (!recentListenerAdded) {
                recentListenerAdded = true;
                recentNotesListView.getSelectionModel().selectedItemProperty().addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue != null) {
                                // Find and load the recent note from cached list
                                Optional<Note> recentNote = cachedAllNotes.stream()
                                        .filter(n -> n.getTitle() != null && n.getTitle().equals(newValue))
                                        .findFirst();
                                if (recentNote.isPresent()) {
                                    loadNoteInEditor(recentNote.get());
                                }
                            }
                        });
            }
        } catch (Exception e) {
            logger.warning("Failed to load recent notes: " + e.getMessage());
        }
    }

    /**
     * Load favorites.
     */
    private void loadFavorites() {
        try {
            List<Note> allNotes = noteDAO.fetchAllNotes();
            cachedFavoriteNotes = allNotes.stream()
                    .filter(Note::isFavorite)
                    .toList();

            List<String> favoriteTitles = cachedFavoriteNotes.stream()
                    .map(Note::getTitle)
                    .limit(10)
                    .toList();

            Platform.runLater(() -> {
                favoritesListView.getItems().setAll(favoriteTitles);
            });

            // Add listener only once
            if (!favoritesListenerAdded) {
                favoritesListenerAdded = true;
                favoritesListView.getSelectionModel().selectedItemProperty().addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue != null) {
                                // Find and load the favorite note from cached list
                                Optional<Note> favoriteNote = cachedFavoriteNotes.stream()
                                        .filter(n -> n.getTitle() != null && n.getTitle().equals(newValue))
                                        .findFirst();
                                if (favoriteNote.isPresent()) {
                                    // Update context to show favorites
                                    currentFilterType = "favorites";
                                    currentFolder = null;
                                    currentTag = null;

                                    // Use Platform.runLater for the entire UI update to avoid race conditions
                                    // during selection
                                    Platform.runLater(() -> {
                                        notesListView.getSelectionModel().clearSelection();
                                        notesListView.getItems().clear();

                                        List<Note> currentFavorites = new ArrayList<>(cachedFavoriteNotes);
                                        if (!currentFavorites.isEmpty()) {
                                            notesListView.getItems().setAll(currentFavorites);
                                            sortNotes(sortComboBox.getValue());
                                            noteCountLabel.setText(currentFavorites.size() + " favorite notes");

                                            final Note noteToLoad = favoriteNote.get();
                                            try {
                                                int index = notesListView.getItems().indexOf(noteToLoad);
                                                if (index >= 0 && index < notesListView.getItems().size()) {
                                                    notesListView.getSelectionModel().select(index);
                                                }
                                                loadNoteInEditor(noteToLoad);
                                            } catch (Exception e) {
                                                logger.warning(
                                                        "Could not select favorite note in list: " + e.getMessage());
                                                loadNoteInEditor(noteToLoad);
                                            }
                                        }
                                    });
                                }
                            }
                        });
            }
        } catch (Exception e) {
            logger.warning("Failed to load favorites: " + e.getMessage());
        }
    }

    /**
     * Load trash notes.
     */
    private void loadTrashNotes() {
        try {
            cachedTrashNotes = noteDAO.fetchTrashNotes();

            List<String> trashTitles = cachedTrashNotes.stream()
                    .map(Note::getTitle)
                    .toList();

            trashListView.getItems().setAll(trashTitles);

            // Add listener only once
            if (!trashListenerAdded) {
                trashListenerAdded = true;
                trashListView.getSelectionModel().selectedItemProperty().addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue != null) {
                                // Find and load the trash note from cached list
                                Optional<Note> trashNote = cachedTrashNotes.stream()
                                        .filter(n -> n.getTitle() != null && n.getTitle().equals(newValue))
                                        .findFirst();
                                if (trashNote.isPresent()) {
                                    // Update context to show trash
                                    currentFilterType = "trash";
                                    currentFolder = null;
                                    currentTag = null;

                                    // Clear current selection and showing list
                                    // Use Platform.runLater for the entire UI update to avoid race conditions
                                    // during selection
                                    Platform.runLater(() -> {
                                        notesListView.getSelectionModel().clearSelection();
                                        notesListView.getItems().clear();

                                        if (cachedTrashNotes != null && !cachedTrashNotes.isEmpty()) {
                                            // Show all trash notes in the notes list
                                            notesListView.getItems().addAll(cachedTrashNotes);
                                            noteCountLabel.setText(cachedTrashNotes.size() + " notes in trash");

                                            final Note noteToLoad = trashNote.get();
                                            try {
                                                int index = notesListView.getItems().indexOf(noteToLoad);
                                                if (index >= 0 && index < notesListView.getItems().size()) {
                                                    notesListView.getSelectionModel().select(index);
                                                }
                                                loadNoteInEditor(noteToLoad);
                                            } catch (Exception e) {
                                                loadNoteInEditor(noteToLoad);
                                            }
                                        }
                                    });
                                }
                            }
                        });

                // Add context menu for Trash
                ContextMenu trashMenu = new ContextMenu();
                MenuItem restoreItem = new MenuItem("Restore Note");
                restoreItem.setOnAction(e -> {
                    String selected = trashListView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        cachedTrashNotes.stream()
                                .filter(n -> n.getTitle().equals(selected))
                                .findFirst()
                                .ifPresent(n -> {
                                    noteDAO.restoreNote(n.getId());
                                    loadTrashNotes();
                                    refreshNotesList();
                                    updateStatus(getString("status.note_restored"));
                                });
                    }
                });

                MenuItem deleteDefItem = new MenuItem("Delete Permanently");
                deleteDefItem.setOnAction(e -> {
                    String selected = trashListView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                        alert.setTitle("Delete Permanently");
                        alert.setHeaderText("Delete this note permanently?");
                        alert.setContentText("This action cannot be undone.");

                        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                            cachedTrashNotes.stream()
                                    .filter(n -> n.getTitle().equals(selected))
                                    .findFirst()
                                    .ifPresent(n -> {
                                        noteDAO.permanentlyDeleteNote(n.getId());
                                        loadTrashNotes();
                                        updateStatus(getString("status.note_deleted_perm"));
                                    });
                        }
                    }
                });

                MenuItem emptyTrashItem = new MenuItem("Empty Trash");
                emptyTrashItem.setOnAction(e -> {
                    if (cachedTrashNotes.isEmpty()) {
                        return;
                    }

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Empty Trash");
                    alert.setHeaderText("Empty the trash?");
                    alert.setContentText(
                            "All notes in the trash will be permanently deleted. This action cannot be undone.");

                    if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                        for (Note n : cachedTrashNotes) {
                            noteDAO.permanentlyDeleteNote(n.getId());
                        }
                        loadTrashNotes();
                        updateStatus(getString("status.trash_emptied"));
                    }
                });

                trashMenu.getItems().addAll(restoreItem, new SeparatorMenuItem(), deleteDefItem,
                        new SeparatorMenuItem(), emptyTrashItem);
                trashListView.setContextMenu(trashMenu);
            }
        } catch (Exception e) {
            logger.warning("Failed to load trash notes: " + e.getMessage());
        }
    }

    /**
     * Load tags with context menu support.
     */
    private void loadTags() {
        try {
            List<Tag> tags = tagDAO.fetchAllTags();
            tagListView.getItems().clear();

            tagListView.setCellFactory(lv -> {
                ListCell<String> cell = new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setContextMenu(null);
                        } else {
                            setText("# " + item);
                            Tag tag = findTagByTitle(item);
                            if (tag != null) {
                                setContextMenu(createTagContextMenu(tag));
                            }
                        }
                    }
                };
                return cell;
            });

            for (Tag tag : tags) {
                tagListView.getItems().add(tag.getTitle());
            }

            tagListView.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            handleTagSelection(newValue);
                        } else {
                            loadAllNotes();
                        }
                    });
        } catch (Exception e) {
            logger.warning("Failed to load tags: " + e.getMessage());
        }
    }

    /**
     * Handle tag selection to filter notes.
     */
    private void handleTagSelection(String tagName) {
        try {
            // Find tag by name
            List<Tag> allTags = tagDAO.fetchAllTags();
            Optional<Tag> selectedTag = allTags.stream()
                    .filter(t -> t.getTitle().equals(tagName))
                    .findFirst();

            if (selectedTag.isPresent()) {
                Tag tag = selectedTag.get();
                currentTag = tag;
                currentFolder = null; // Clear folder selection when filtering by tag
                currentFilterType = "tag";
                List<Note> notesWithTag = tagDAO.fetchAllNotesWithTag(tag.getId());
                notesListView.getItems().setAll(notesWithTag);
                sortNotes(sortComboBox.getValue());
                noteCountLabel.setText(notesWithTag.size() + " notes with tag: " + tagName);
                updateStatus(java.text.MessageFormat.format(getString("status.filtered_tag"), tagName));
            }
        } catch (Exception e) {
            logger.severe("Failed to filter notes by tag " + tagName + ": " + e.getMessage());
            updateStatus("Error filtering by tag");
        }
    }

    /**
     * Perform search.
     */
    private void performSearch(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            // Restore previous context if search is cleared
            if (currentFolder != null) {
                handleFolderSelection(currentFolder);
            } else if (currentTag != null) {
                handleTagSelection(currentTag.getTitle());
            } else {
                loadAllNotes();
            }
            return;
        }

        try {
            List<Note> allNotes = noteDAO.fetchAllNotes();
            String searchLower = searchText.toLowerCase();
            List<Note> filteredNotes = allNotes.stream()
                    .filter(note -> {
                        String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
                        String content = note.getContent() != null ? note.getContent().toLowerCase() : "";
                        return title.contains(searchLower) || content.contains(searchLower);
                    })
                    .toList();

            notesListView.getItems().setAll(filteredNotes);
            sortNotes(sortComboBox.getValue());
            noteCountLabel.setText(java.text.MessageFormat.format(getString("info.notes_found"), filteredNotes.size()));
            currentFilterType = "search";

            // Refresh grid view if active
            if (currentNotesViewMode == NotesViewMode.GRID) {
                Platform.runLater(this::refreshGridView);
            }

            updateStatus(java.text.MessageFormat.format(getString("status.search_active"), searchText));
        } catch (Exception e) {
            logger.severe("Failed to perform search: " + e.getMessage());
            updateStatus(getString("status.search_failed"));
        }
    }

    /**
     * Sort notes with null-safe comparisons.
     */
    private void sortNotes(String sortOption) {
        if (sortOption == null)
            return;

        List<Note> notes = new ArrayList<>(notesListView.getItems());

        notes.sort((a, b) -> {
            // High priority: Pinned notes always on top
            if (a.isPinned() != b.isPinned()) {
                return a.isPinned() ? -1 : 1;
            }

            // Normal matching based on selection
            if (sortOption.equals(getString("sort.title_az"))) {
                String titleA = a.getTitle() != null ? a.getTitle() : "";
                String titleB = b.getTitle() != null ? b.getTitle() : "";
                return titleA.compareToIgnoreCase(titleB);
            } else if (sortOption.equals(getString("sort.title_za"))) {
                String titleZA = a.getTitle() != null ? a.getTitle() : "";
                String titleZB = b.getTitle() != null ? b.getTitle() : "";
                return titleZB.compareToIgnoreCase(titleZA);
            } else if (sortOption.equals(getString("sort.created_newest"))) {
                String cDateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
                String cDateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
                return cDateB.compareTo(cDateA);
            } else if (sortOption.equals(getString("sort.created_oldest"))) {
                String coDateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
                String coDateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
                return coDateA.compareTo(coDateB);
            } else if (sortOption.equals(getString("sort.modified_newest"))) {
                String mDateA = a.getModifiedDate() != null ? a.getModifiedDate()
                        : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String mDateB = b.getModifiedDate() != null ? b.getModifiedDate()
                        : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                return mDateB.compareTo(mDateA);
            } else if (sortOption.equals(getString("sort.modified_oldest"))) {
                String moDateA = a.getModifiedDate() != null ? a.getModifiedDate()
                        : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String moDateB = b.getModifiedDate() != null ? b.getModifiedDate()
                        : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                return moDateA.compareTo(moDateB);
            } else {
                return 0;
            }
        });

        notesListView.getItems().setAll(notes);

        // Refresh grid view if active
        if (currentNotesViewMode == NotesViewMode.GRID) {
            Platform.runLater(this::refreshGridView);
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
        if (currentNote != null && previewWebView != null) {
            String content = noteContentArea.getText();
            // Determine if dark theme is active (handle system mode)
            String actualTheme = currentTheme;
            if ("system".equals(currentTheme)) {
                actualTheme = detectSystemTheme();
            }
            boolean isDarkTheme = "dark".equals(actualTheme);

            if (content != null && !content.trim().isEmpty()) {
                // Convert markdown to HTML
                String html = com.example.forevernote.util.MarkdownProcessor.markdownToHtml(content);

                // highlight.js theme selection (VS Code style)
                String highlightTheme = isDarkTheme
                        ? "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/vs2015.min.css"
                        : "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/vs.min.css";

                // Collect injections from plugins
                StringBuilder headInjections = new StringBuilder();
                StringBuilder bodyInjections = new StringBuilder();

                for (PreviewEnhancer enhancer : previewEnhancers.values()) {
                    String head = enhancer.getHeadInjections();
                    if (head != null && !head.isEmpty()) {
                        headInjections.append(head).append("\n");
                    }
                    String body = enhancer.getBodyInjections();
                    if (body != null && !body.isEmpty()) {
                        bodyInjections.append(body).append("\n");
                    }
                }

                // Create a complete HTML document with theme-aware styling and syntax
                // highlighting
                String fullHtml = "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
                        "    <link rel=\"stylesheet\" href=\"" + highlightTheme + "\">\n" +
                        "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js\"></script>\n"
                        + headInjections.toString() + "\n" +
                        "    <style>\n" +
                        "        @import url('https://fonts.googleapis.com/css2?family=Noto+Color+Emoji&family=JetBrains+Mono:wght@400;500&display=swap');\n"
                        +
                        "        html { " +
                        (isDarkTheme ? "background-color: #1E1E1E;" : "background-color: #FFFFFF;") +
                        " margin: 0; padding: 0; width: 100%; height: 100%; }\n" +
                        (isDarkTheme ?
                        // Dark theme styles with improved code styling
                                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; padding: 20px; line-height: 1.6; color: #E0E0E0; background-color: #1E1E1E; }\n"
                                        +
                                        "        h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 600; color: #FFFFFF; }\n"
                                        +
                                        "        h1 { font-size: 2em; border-bottom: 2px solid #3a3a3a; padding-bottom: 0.3em; }\n"
                                        +
                                        "        h2 { font-size: 1.5em; border-bottom: 1px solid #3a3a3a; padding-bottom: 0.3em; }\n"
                                        +
                                        "        h3 { font-size: 1.25em; }\n" +
                                        "        /* Inline code */\n" +
                                        "        code:not(pre code) { background-color: #2d2d2d; color: #ce9178; padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 0.9em; }\n"
                                        +
                                        "        /* Code blocks with syntax highlighting */\n" +
                                        "        pre { background-color: #1e1e1e; border: 1px solid #3a3a3a; border-radius: 6px; margin: 1em 0; overflow-x: auto; }\n"
                                        +
                                        "        pre code { font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 13px; line-height: 1.5; padding: 16px !important; display: block; background: transparent !important; color: inherit; }\n"
                                        +
                                        "        /* Language label */\n" +
                                        "        pre[class*='language-']::before { content: attr(data-lang); position: absolute; top: 0; right: 0; padding: 2px 8px; font-size: 10px; color: #888; background: #2d2d2d; border-bottom-left-radius: 4px; }\n"
                                        +
                                        "        pre { position: relative; }\n" +
                                        "        /* Additional highlight.js overrides for dark theme */\n" +
                                        "        .hljs { background: transparent !important; }\n" +
                                        "        blockquote { border-left: 4px solid #818CF8; margin: 0; padding-left: 20px; color: #B3B3B3; background-color: #252525; padding: 10px 20px; border-radius: 4px; }\n"
                                        +
                                        "        ul, ol { margin: 1em 0; padding-left: 2em; color: #E0E0E0; }\n" +
                                        "        li { margin: 0.5em 0; }\n" +
                                        "        /* Task lists */\n" +
                                        "        input[type='checkbox'] { margin-right: 8px; transform: scale(1.2); }\n"
                                        +
                                        "        table { border-collapse: collapse; width: 100%; margin: 1em 0; }\n" +
                                        "        table th, table td { border: 1px solid #3a3a3a; padding: 10px; text-align: left; }\n"
                                        +
                                        "        table th { background-color: #252525; font-weight: 600; color: #FFFFFF; }\n"
                                        +
                                        "        table td { background-color: #1E1E1E; color: #E0E0E0; }\n" +
                                        "        table tr:hover td { background-color: #252525; }\n" +
                                        "        a { color: #818CF8; text-decoration: none; }\n" +
                                        "        a:hover { color: #A5B4FC; text-decoration: underline; }\n" +
                                        "        img { max-width: 100%; height: auto; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.3); }\n"
                                        +
                                        "        hr { border: none; border-top: 1px solid #3a3a3a; margin: 2em 0; }\n" +
                                        "        strong { color: #FFFFFF; font-weight: 600; }\n" +
                                        "        em { font-style: italic; }\n" +
                                        "        mark { background-color: #564a00; color: #ffd700; padding: 1px 3px; border-radius: 2px; }\n"
                                        +
                                        "        /* Emoji support */\n" +
                                        "        * { font-variant-emoji: emoji; }\n"
                                :
                                // Light theme styles with improved code styling
                                "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; padding: 20px; line-height: 1.6; color: #24292e; background-color: #FFFFFF; }\n"
                                        +
                                        "        h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 600; color: #24292e; }\n"
                                        +
                                        "        h1 { font-size: 2em; border-bottom: 2px solid #eaecef; padding-bottom: 0.3em; }\n"
                                        +
                                        "        h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }\n"
                                        +
                                        "        h3 { font-size: 1.25em; }\n" +
                                        "        /* Inline code */\n" +
                                        "        code:not(pre code) { background-color: #f0f0f0; color: #d63384; padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 0.9em; }\n"
                                        +
                                        "        /* Code blocks with syntax highlighting */\n" +
                                        "        pre { background-color: #f8f8f8; border: 1px solid #e1e4e8; border-radius: 6px; margin: 1em 0; overflow-x: auto; }\n"
                                        +
                                        "        pre code { font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 13px; line-height: 1.5; padding: 16px !important; display: block; background: transparent !important; color: inherit; }\n"
                                        +
                                        "        /* Language label */\n" +
                                        "        pre[class*='language-']::before { content: attr(data-lang); position: absolute; top: 0; right: 0; padding: 2px 8px; font-size: 10px; color: #666; background: #e8e8e8; border-bottom-left-radius: 4px; }\n"
                                        +
                                        "        pre { position: relative; }\n" +
                                        "        /* Additional highlight.js overrides for light theme */\n" +
                                        "        .hljs { background: transparent !important; }\n" +
                                        "        blockquote { border-left: 4px solid #6366F1; margin: 0; padding-left: 20px; color: #57606a; background-color: #f6f8fa; padding: 10px 20px; border-radius: 4px; }\n"
                                        +
                                        "        ul, ol { margin: 1em 0; padding-left: 2em; color: #24292e; }\n" +
                                        "        li { margin: 0.5em 0; }\n" +
                                        "        /* Task lists */\n" +
                                        "        input[type='checkbox'] { margin-right: 8px; transform: scale(1.2); }\n"
                                        +
                                        "        table { border-collapse: collapse; width: 100%; margin: 1em 0; }\n" +
                                        "        table th, table td { border: 1px solid #e1e4e8; padding: 10px; text-align: left; }\n"
                                        +
                                        "        table th { background-color: #f6f8fa; font-weight: 600; color: #24292e; }\n"
                                        +
                                        "        table td { background-color: #FFFFFF; color: #24292e; }\n" +
                                        "        table tr:hover td { background-color: #f6f8fa; }\n" +
                                        "        a { color: #0969da; text-decoration: none; }\n" +
                                        "        a:hover { color: #0550ae; text-decoration: underline; }\n" +
                                        "        img { max-width: 100%; height: auto; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n"
                                        +
                                        "        hr { border: none; border-top: 1px solid #e1e4e8; margin: 2em 0; }\n" +
                                        "        strong { color: #24292e; font-weight: 600; }\n" +
                                        "        em { font-style: italic; }\n" +
                                        "        mark { background-color: #fff8c5; padding: 1px 3px; border-radius: 2px; }\n"
                                        +
                                        "        /* Emoji support */\n" +
                                        "        * { font-variant-emoji: emoji; }\n")
                        +
                        "    </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        html +
                        "\n<script>\n" +
                        "    // Initialize syntax highlighting\n" +
                        "    document.addEventListener('DOMContentLoaded', function() {\n" +
                        "        document.querySelectorAll('pre code').forEach(function(block) {\n" +
                        "            hljs.highlightElement(block);\n" +
                        "        });\n" +
                        "    });\n" +
                        "    // Run immediately in case DOM is already loaded\n" +
                        "    document.querySelectorAll('pre code').forEach(function(block) {\n" +
                        "        hljs.highlightElement(block);\n" +
                        "    });\n" +
                        "</script>\n" +
                        bodyInjections.toString() + "\n" +
                        "</body>\n" +
                        "</html>";

                previewWebView.getEngine().loadContent(fullHtml, "text/html");
            } else {
                // Empty preview with theme-aware background
                String emptyHtml = "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
                        "    <style>\n" +
                        "        html, body { " +
                        (isDarkTheme ? "color: #B3B3B3; background-color: #1E1E1E;"
                                : "color: #71717A; background-color: #FFFFFF;")
                        +
                        " margin: 0; padding: 0; width: 100%; height: 100%; }\n" +
                        "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; }\n"
                        +
                        "    </style>\n" +
                        "</head>\n" +
                        "<body></body>\n" +
                        "</html>";
                previewWebView.getEngine().loadContent(emptyHtml, "text/html");
            }
        }
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
        alert.setTitle("Save Changes");
        alert.setHeaderText("Do you want to save changes to the current note?");
        alert.setContentText("Your changes will be lost if you don't save them.");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType dontSaveButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

        return alert.showAndWait();
    }

    /**
     * Add tag to current note.
     */
    @FXML
    private void handleAddTagToNote() {
        if (currentNote == null) {
            updateStatus(getString("status.no_note"));
            return;
        }

        try {
            // Get existing tags
            List<Tag> existingTags = tagDAO.fetchAllTags();
            List<Tag> noteTags = noteDAO.fetchTags(currentNote.getId());

            // Filter out tags already assigned to the note
            List<String> availableTagNames = existingTags.stream()
                    .filter(tag -> !noteTags.stream().anyMatch(nt -> nt.getId().equals(tag.getId())))
                    .map(Tag::getTitle)
                    .sorted()
                    .toList();

            // Create dialog for adding tag
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Add Tag");
            dialog.setHeaderText(availableTagNames.isEmpty()
                    ? "No existing tags available. Enter a new tag name:"
                    : "Select an existing tag or enter a new tag name:");

            // Set buttons
            ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

            // Create layout
            VBox content = new VBox(10);
            ComboBox<String> tagComboBox = new ComboBox<>();
            tagComboBox.setEditable(true);
            tagComboBox.getItems().addAll(availableTagNames);
            tagComboBox.setPromptText("Select or type a tag name...");
            tagComboBox.setPrefWidth(300);
            content.getChildren().add(new Label("Tag:"));
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
                    String tagId = tagDAO.createTag(tag);
                    tag.setId(tagId);
                }

                // Check if tag is already assigned to note (double check)
                List<Tag> currentNoteTags = noteDAO.fetchTags(currentNote.getId());
                boolean alreadyHasTag = currentNoteTags.stream()
                        .anyMatch(t -> t.getId().equals(tag.getId()));

                if (alreadyHasTag) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Tag Already Assigned");
                    alert.setHeaderText("This note already has the tag: " + tagName);
                    alert.showAndWait();
                } else {
                    noteDAO.addTag(currentNote, tag);
                    loadNoteTags(currentNote);
                    // Update tags list in sidebar
                    loadTags();
                    updateStatus(java.text.MessageFormat.format(getString("status.tag_added"), tagName));
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to add tag: " + e.getMessage());
            updateStatus(getString("status.tag_add_error"));
        }
    }

    private ContextMenu createFolderContextMenu(Folder folder) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem newNoteItem = new MenuItem(getString("action.new_note"));
        newNoteItem.setOnAction(e -> {
            currentFolder = folder;
            handleNewNote(e);
        });
        MenuItem newFolderItem = new MenuItem(getString("action.new_subfolder"));
        newFolderItem.setOnAction(e -> {
            currentFolder = folder;
            handleNewSubfolder(e);
        });
        MenuItem renameItem = new MenuItem(getString("action.rename"));
        renameItem.setOnAction(e -> handleRenameFolder(folder));
        MenuItem deleteItem = new MenuItem(getString("action.delete"));
        deleteItem.setOnAction(e -> handleDeleteFolder(folder));
        contextMenu.getItems().addAll(newNoteItem, newFolderItem, renameItem, deleteItem);
        return contextMenu;
    }

    private ContextMenu createNoteContextMenu(Note note) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem(getString("action.open"));
        openItem.setOnAction(e -> loadNoteInEditor(note));
        MenuItem favoriteItem = new MenuItem(
                note.isFavorite() ? getString("action.remove_favorite") : getString("action.add_favorite"));
        favoriteItem.setOnAction(e -> toggleFavorite(note));
        MenuItem pinItem = new MenuItem(
                note.isPinned() ? getString("action.unpin_note") : getString("action.pin_note"));
        pinItem.setOnAction(e -> togglePin(note));
        MenuItem deleteItem = new MenuItem(getString("action.move_to_trash"));
        deleteItem.setOnAction(e -> deleteNote(note));
        contextMenu.getItems().addAll(openItem, favoriteItem, pinItem, new SeparatorMenuItem(), deleteItem);
        return contextMenu;
    }

    private ContextMenu createTagContextMenu(Tag tag) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem renameItem = new MenuItem(getString("action.rename_tag"));
        renameItem.setOnAction(e -> handleRenameTag(tag));
        MenuItem deleteItem = new MenuItem(getString("action.delete_tag"));
        deleteItem.setOnAction(e -> handleDeleteTag(tag));
        contextMenu.getItems().addAll(renameItem, deleteItem);
        return contextMenu;
    }

    /**
     * Remove tag from note.
     */
    private void removeTagFromNote(Tag tag) {
        if (currentNote == null) {
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
                noteDAO.removeTag(currentNote, tag);
                loadNoteTags(currentNote);
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

            // Fix: If a folder is selected, prepare the ID with the folder path
            if (currentFolder != null && currentFolder.getId() != null &&
                    !"ROOT".equals(currentFolder.getId()) &&
                    !currentFolder.getTitle().equals("All Notes")) {

                String pathSeparator = File.separator;
                String folderPath = currentFolder.getId();
                String safeTitle = newNote.getTitle().replaceAll("[^a-zA-Z0-9\\.\\-_ ]", "_");

                // We set an ID like "Folder/New Note" so NoteDAOFileSystem detects the parent
                // folder
                newNote.setId(folderPath + pathSeparator + safeTitle);
            }

            String noteId = noteDAO.createNote(newNote);
            newNote.setId(noteId);

            // With FS DAO and our fix, the file is already in the right place.
            // We don't need to manually add it to the Folder object's children list
            // because refreshNotesList() will re-fetch from disk correctly.

            notesListView.getItems().add(0, newNote);
            notesListView.getSelectionModel().select(newNote);
            loadNoteInEditor(newNote);

            // Refresh recent notes to include new note
            loadRecentNotes();

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
                currentFolder.getTitle().equals(getString("app.all_notes")) ||
                currentFolder.getTitle().equals("All Notes") ||
                currentFolder.getTitle().equals("📚 All Notes") ||
                currentFolder.getTitle().endsWith("All Notes")); // Robust check

        String headerText = createInRoot
                ? getString("dialog.new_folder.header_root")
                : java.text.MessageFormat.format(getString("dialog.new_folder.header_sub"), currentFolder.getTitle());

        dialog.setHeaderText(headerText);
        dialog.setContentText(getString("dialog.new_folder.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                Folder newFolder = new Folder(result.get().trim());
                String folderId = folderDAO.createFolder(newFolder);
                newFolder.setId(folderId);

                // Only add as subfolder if currentFolder is set and not "All Notes"
                if (!createInRoot && currentFolder != null) {
                    folderDAO.addSubFolder(currentFolder, newFolder);
                }
                // Otherwise, it's created in root (parent_id will be NULL)

                loadFolders();
                // Select "All Notes" root to make it clear where new folders are created
                if (folderTreeView.getRoot() != null) {
                    folderTreeView.getSelectionModel().select(folderTreeView.getRoot());
                }
                currentFolder = null;
                updateStatus(java.text.MessageFormat.format(getString("status.folder_created"), newFolder.getTitle()));
            } catch (Exception e) {
                logger.severe("Failed to create folder: " + e.getMessage());
                updateStatus(getString("status.error") + ": " + e.getMessage());
            }
        }
    }

    /**
     * Handle creating a new subfolder in the currently selected folder.
     */
    private void handleNewSubfolder(ActionEvent event) {
        if (currentFolder == null ||
                currentFolder.getTitle().equals("All Notes") ||
                currentFolder.getTitle().equals("📚 All Notes")) {
            handleNewFolder(event);
            return;
        }

        TextInputDialog dialog = new TextInputDialog("New Subfolder");
        dialog.setTitle("New Subfolder");
        dialog.setHeaderText("Create a new subfolder in: " + currentFolder.getTitle());
        dialog.setContentText("Subfolder name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                Folder newSubfolder = new Folder(result.get().trim());
                String folderId = folderDAO.createFolder(newSubfolder);
                newSubfolder.setId(folderId);

                folderDAO.addSubFolder(currentFolder, newSubfolder);

                loadFolders();
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
        fileChooser.setTitle("Import Notes");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Files", "*.md", "*.txt", "*.markdown"),
                new FileChooser.ExtensionFilter("Markdown Files", "*.md", "*.markdown"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

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
                    String noteId = noteDAO.createNote(newNote);
                    newNote.setId(noteId);

                    // Add to current folder if selected
                    if (currentFolder != null && currentFolder.getId() != null) {
                        folderDAO.addNote(currentFolder, newNote);
                    }

                    imported++;
                } catch (Exception e) {
                    logger.warning("Failed to import file " + file.getName() + ": " + e.getMessage());
                    failed++;
                }
            }

            // Refresh lists
            refreshNotesList();
            loadRecentNotes();

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
        if (currentNote != null && isModified) {
            try {
                // Update note content and title from UI
                currentNote.setTitle(noteTitleField.getText());
                currentNote.setContent(noteContentArea.getText());
                noteDAO.updateNote(currentNote);
                isModified = false;

                // Refresh the notes list to show updated title
                refreshNotesList();

                // Refresh favorites list in case favorite status changed
                loadFavorites();

                // Publish NoteSavedEvent for plugins (Outline, etc.)
                if (eventBus != null) {
                    eventBus.publish(new NoteEvents.NoteSavedEvent(currentNote));
                }

                updateStatus(java.text.MessageFormat.format(getString("status.saved_note"), currentNote.getTitle()));
            } catch (Exception e) {
                logger.severe("Failed to save note: " + e.getMessage());
                updateStatus(getString("status.error_saving"));
            }
        }
    }

    /**
     * Refresh the notes list to reflect current state.
     */
    private void refreshNotesList() {
        if (currentFolder == null) {
            loadAllNotes();
        } else {
            handleFolderSelection(currentFolder);
        }

        // Also refresh grid view if active
        if (currentNotesViewMode == NotesViewMode.GRID) {
            refreshGridView();
        }
    }

    @FXML
    private void handleSaveAll(ActionEvent event) {
        // Save all modified notes (for now, just save current if modified)
        if (currentNote != null && isModified) {
            handleSave(event);
        }
        updateStatus(getString("status.saved_all"));
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        // Obtenemos qué pestaña de la barra lateral está activa
        int activeTabIndex = navigationTabPane.getSelectionModel().getSelectedIndex();

        // Prioridad 1: Si estamos en la pestaña de Carpetas (index 0)
        if (activeTabIndex == 0) {
            TreeItem<Folder> selectedFolderItem = folderTreeView.getSelectionModel().getSelectedItem();
            if (selectedFolderItem != null && selectedFolderItem.getValue() != null) {
                Folder folder = selectedFolderItem.getValue();
                String rootTitle = getString("app.all_notes");
                if (!folder.getTitle().equals(rootTitle) && !"All Notes".equals(folder.getTitle())) {
                    handleDeleteFolder(folder);
                    return;
                }
            }
        }

        // Prioridad 2: Si estamos en la pestaña de Etiquetas (index 1)
        if (activeTabIndex == 1) {
            String selectedTagName = tagListView.getSelectionModel().getSelectedItem();
            if (selectedTagName != null) {
                Tag tag = findTagByTitle(selectedTagName);
                if (tag != null) {
                    handleDeleteTag(tag);
                    return;
                }
            }
        }

        // Prioridad 3: Borrar la nota seleccionada en la lista principal
        Note selectedNote = notesListView.getSelectionModel().getSelectedItem();
        if (selectedNote != null) {
            deleteNote(selectedNote);
            return;
        }

        // Prioridad 4: Borrar la nota que esté abierta actualmente en el editor (si no
        // hay selección en lista)
        if (currentNote != null) {
            deleteNote(currentNote);
            return;
        }

        updateStatus(getString("status.nothing_to_delete"));
    }

    /**
     * Delete a note (move to trash).
     */
    private void deleteNote(Note note) {
        if (note == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(getString("dialog.delete_note.title"));
        alert.setHeaderText(getString("dialog.delete_note.header"));
        alert.setContentText(getString("dialog.delete_note.content"));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                noteDAO.deleteNote(note.getId());

                if (currentNote != null && currentNote.getId().equals(note.getId())) {
                    // Clear editor
                    currentNote = null;
                    noteTitleField.clear();
                    noteContentArea.clear();
                    tagsFlowPane.getChildren().clear();
                    previewWebView.getEngine().loadContent("", "text/html");
                }

                // Refresh ALL lists
                refreshNotesList();
                loadRecentNotes();
                loadFavorites();
                loadTrashNotes();

                updateStatus(getString("status.note_moved_trash"));
            } catch (Exception e) {
                logger.severe("Failed to delete note: " + e.getMessage());
                updateStatus(getString("status.note_delete_error"));
            }
        }
    }

    @FXML
    private void handleDeleteNote(ActionEvent event) {
        handleDelete(event);
    }

    @FXML
    private void handleExit(ActionEvent event) {
        if (isModified && currentNote != null) {
            Optional<ButtonType> result = showSaveDialog();
            if (result.isPresent()) {
                if (result.get().getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                    return;
                }
                if (result.get().getText().equals("Save")) {
                    handleSave(event);
                }
            }
        }

        try {
            if (connection != null && !connection.isClosed()) {
                SQLiteDB db = SQLiteDB.getInstance();
                db.closeConnection(connection);
            }
        } catch (Exception e) {
            logger.warning("Error closing database connection: " + e.getMessage());
        }

        System.exit(0);
    }

    @FXML
    private void handleExport(ActionEvent event) {
        if (currentNote == null) {
            showAlert(Alert.AlertType.WARNING, getString("dialog.export.title"),
                    getString("dialog.export.no_note_header"), getString("dialog.export.no_note_content"));
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(getString("dialog.export.save_title"));
        fileChooser.setInitialFileName(sanitizeFileName(currentNote.getTitle()));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(getString("file_filter.markdown"), "*.md"),
                new FileChooser.ExtensionFilter(getString("file_filter.text"), "*.txt"),
                new FileChooser.ExtensionFilter(getString("file_filter.all"), "*.*"));

        File file = fileChooser.showSaveDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Add title as header for Markdown
                if (file.getName().endsWith(".md")) {
                    writer.write("# " + currentNote.getTitle() + "\n\n");
                }
                writer.write(currentNote.getContent() != null ? currentNote.getContent() : "");
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
            dialog.setTitle("Find");
            dialog.setHeaderText("Find text in note");
            dialog.setContentText("Search for:");

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
        dialog.setTitle("Find and Replace");
        dialog.setHeaderText("Replace text in note");

        ButtonType replaceButton = new ButtonType("Replace", ButtonBar.ButtonData.OK_DONE);
        ButtonType replaceAllButton = new ButtonType("Replace All", ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(replaceButton, replaceAllButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        TextField findField = new TextField();
        findField.setPromptText("Find...");
        TextField replaceField = new TextField();
        replaceField.setPromptText("Replace with...");

        content.getChildren().addAll(
                new Label("Find:"), findField,
                new Label("Replace with:"), replaceField);

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
                isModified = true;
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
                        if (sidebarToggleBtn != null)
                            sidebarToggleBtn.setSelected(true);
                    } else {
                        // Collapse: hide navSplitPane completely
                        navSplitPane.setMinWidth(0);
                        navSplitPane.setMaxWidth(0);
                        navSplitPane.setPrefWidth(0);
                        updateStatus(getString("status.nav_hidden"));
                        if (sidebarToggleBtn != null)
                            sidebarToggleBtn.setSelected(false);
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
                    if (sidebarToggleBtn != null)
                        sidebarToggleBtn.setSelected(true);
                } else {
                    // Collapse
                    sidebarPane.setMinWidth(0);
                    sidebarPane.setMaxWidth(0);
                    sidebarPane.setPrefWidth(0);
                    updateStatus(getString("status.sidebar_hidden"));
                    if (sidebarToggleBtn != null)
                        sidebarToggleBtn.setSelected(false);
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
                if (notesPanelToggleBtn != null)
                    notesPanelToggleBtn.setSelected(true);
            } else {
                // Collapse
                notesPanel.setMinWidth(0);
                notesPanel.setMaxWidth(0);
                notesPanel.setPrefWidth(0);
                updateStatus(getString("status.notes_panel_hidden"));
                if (notesPanelToggleBtn != null)
                    notesPanelToggleBtn.setSelected(false);
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
        if (toolbarHBox != null && toolbarHBox.getScene() != null) {
            toolbarHBox.getScene().getRoot().setStyle("-fx-font-size: " + uiFontSize + "px;");
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
        if (sidebarToggleBtn != null)
            sidebarToggleBtn.setSelected(true);
        if (notesPanelToggleBtn != null)
            notesPanelToggleBtn.setSelected(true);
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
            updateStatus(show ? "Tags bar shown" : "Tags bar hidden");
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

        // Add the appropriate theme stylesheet
        java.net.URL themeResource;
        if ("dark".equals(currentTheme)) {
            themeResource = getClass().getResource("/com/example/forevernote/ui/css/dark-theme.css");
        } else {
            themeResource = getClass().getResource("/com/example/forevernote/ui/css/modern-theme.css");
        }

        if (themeResource != null) {
            scene.getStylesheets().add(themeResource.toExternalForm());
            logger.info("Theme changed to: " + currentTheme);

            // Determine actual theme (system mode uses detected theme)
            String actualTheme = currentTheme;
            if ("system".equals(currentTheme)) {
                // Re-detect system theme
                actualTheme = detectSystemTheme();
            }

            // Ensure WebView has correct background color
            if (previewWebView != null) {
                if (!previewWebView.getStyleClass().contains("webview-theme")) {
                    previewWebView.getStyleClass().add("webview-theme");
                }

                // Still set background via JavaScript to ensure it's applied to the body
                String bgColor = "dark".equals(actualTheme) ? "#1E1E1E" : "#FFFFFF";
                previewWebView.getEngine().executeScript(
                        "document.body.style.backgroundColor = '" + bgColor + "';");
            }

            // Update preview to reflect theme change
            if (currentNote != null) {
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

    @FXML
    private void handleSearch(ActionEvent event) {
        if (searchField != null) {
            searchField.requestFocus();
            searchField.selectAll();
            updateStatus(getString("status.search_focused"));
        }
    }

    @FXML
    private void handleTagsManager(ActionEvent event) {
        try {
            List<Tag> allTags = tagDAO.fetchAllTags();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Tags Manager");
            dialog.setHeaderText("Manage your tags");

            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
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
                        Label dateLabel = new Label(tag.getCreatedDate() != null ? tag.getCreatedDate() : "N/A");
                        dateLabel.setStyle("-fx-text-fill: gray;");

                        Button deleteButton = new Button("Delete");
                        deleteButton.setOnAction(e -> {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle("Delete Tag");
                            confirm.setHeaderText("Are you sure you want to delete this tag?");
                            confirm.setContentText("This will remove the tag from all notes.");
                            Optional<ButtonType> result = confirm.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                try {
                                    tagDAO.deleteTag(tag.getId());
                                    tagListView.getItems().remove(tag);
                                    loadTags(); // Refresh sidebar
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

            content.getChildren().add(new Label("All Tags (" + allTags.size() + "):"));
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
        dialog.setTitle("Preferences");
        dialog.setHeaderText("Application Settings");

        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        // Database location
        Label dbLabel = new Label("Database Location:");
        Label dbPathLabel = new Label("Forevernote/data/database.db");
        dbPathLabel.setStyle("-fx-text-fill: gray;");

        // Auto-save option (placeholder)
        Label autoSaveLabel = new Label("Auto-save: Coming soon");
        autoSaveLabel.setStyle("-fx-text-fill: gray;");

        content.getChildren().addAll(
                new Label("General Settings"),
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
        alert.setTitle("Documentation");
        alert.setHeaderText("Forevernote - User Guide");
        alert.setContentText(
                "Forevernote is a free, offline note-taking application.\n\n" +
                        "Key Features:\n" +
                        "• Create and organize notes in folders\n" +
                        "• Tag your notes for easy categorization\n" +
                        "• Markdown support with live preview\n" +
                        "• Search across all notes\n" +
                        "• Keyboard shortcuts for quick access\n\n" +
                        "Keyboard Shortcuts:\n" +
                        "• Ctrl+N: New Note\n" +
                        "• Ctrl+S: Save\n" +
                        "• Ctrl+F: Find in note\n" +
                        "• F9: Toggle Sidebar\n" +
                        "• F1: Show all shortcuts\n\n" +
                        "For more information, visit the project repository.");
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500, 400);
        alert.showAndWait();
    }

    @FXML
    private void handleKeyboardShortcuts(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Available Keyboard Shortcuts");
        alert.setContentText(
                "File Operations:\n" +
                        "  Ctrl+N          New Note\n" +
                        "  Ctrl+Shift+N    New Folder\n" +
                        "  Ctrl+S          Save\n" +
                        "  Ctrl+Shift+S    Save All\n" +
                        "  Ctrl+E          Export\n" +
                        "  Ctrl+Q          Exit\n\n" +
                        "Edit Operations:\n" +
                        "  Ctrl+Z          Undo\n" +
                        "  Ctrl+Y          Redo\n" +
                        "  Ctrl+X          Cut\n" +
                        "  Ctrl+C          Copy\n" +
                        "  Ctrl+V          Paste\n" +
                        "  Ctrl+F          Find\n" +
                        "  Ctrl+H          Replace\n\n" +
                        "View Operations:\n" +
                        "  F9              Toggle Sidebar\n" +
                        "  Ctrl++          Zoom In\n" +
                        "  Ctrl+-          Zoom Out\n" +
                        "  Ctrl+0          Reset Zoom\n\n" +
                        "Tools:\n" +
                        "  Ctrl+Shift+F    Search\n" +
                        "  F1              Show this help");
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(450, 500);
        alert.showAndWait();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("About Forevernote");

        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox content = new VBox(16);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setAlignment(javafx.geometry.Pos.CENTER);

        // App icon and name
        Label titleLabel = new Label("Forevernote");
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
        Label techLabel = new Label("Built with Java 17, JavaFX 21, SQLite & CommonMark");
        techLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        // Copyright
        Label copyrightLabel = new Label(com.example.forevernote.AppConfig.getAppCopyright());
        copyrightLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        // Developer credit
        Label developerLabel = new Label("Developed by Edu Díaz (RGiskard7)");
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
                        loadAllNotes();
                    }
                    break;
                case "tag":
                    if (currentTag != null) {
                        handleTagSelection(currentTag.getTitle());
                    } else {
                        loadAllNotes();
                    }
                    break;
                case "favorites":
                    // Load favorites
                    List<Note> allNotes = noteDAO.fetchAllNotes();
                    List<Note> favoriteNotes = allNotes.stream()
                            .filter(Note::isFavorite)
                            .toList();
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
                    String searchText = searchField.getText();
                    if (searchText != null && !searchText.trim().isEmpty()) {
                        performSearch(searchText);
                    } else {
                        loadAllNotes();
                    }
                    break;
                default:
                    loadAllNotes();
                    break;
            }
        } catch (Exception e) {
            logger.severe("Failed to refresh: " + e.getMessage());
            updateStatus(getString("status.refresh_error"));
        }
    }

    @FXML
    private void handleToggleFavorite(ActionEvent event) {
        if (currentNote == null) {
            updateStatus(getString("status.no_note"));
            return;
        }
        toggleFavorite(currentNote);
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
            noteDAO.updateNote(note);

            // Update UI if this is the current note
            if (currentNote != null && currentNote.getId().equals(note.getId())) {
                updateFavoriteButtonIcon();
            }

            // Refresh lists
            refreshNotesList();
            loadFavorites();

            updateStatus(newFavoriteStatus ? "Note marked as favorite" : "Note unmarked as favorite");
        } catch (Exception e) {
            logger.severe("Failed to toggle favorite: " + e.getMessage());
            updateStatus(getString("status.fav_error"));
        }
    }

    @FXML
    private void handleTogglePin(ActionEvent event) {
        if (currentNote == null) {
            updateStatus(getString("status.no_note"));
            return;
        }
        togglePin(currentNote);
    }

    private void togglePin(Note note) {
        if (note == null)
            return;

        try {
            boolean newPinStatus = !note.isPinned();
            note.setPinned(newPinStatus);

            noteDAO.updateNote(note);

            if (currentNote != null && currentNote.getId().equals(note.getId())) {
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

        if (currentNote != null && currentNote.isPinned()) {
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
        if (favoriteButton != null && currentNote != null) {
            boolean isFav = currentNote.isFavorite();
            favoriteButton.setSelected(isFav);
            if (favoriteButton.getTooltip() != null) {
                favoriteButton.getTooltip().setText(isFav ? "Remove from favorites" : "Add to favorites");
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
                if (tagDAO.existsByTitle(tagName)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(getString("dialog.tag_exists.title"));
                    alert.setHeaderText(getString("dialog.tag_exists.header"));
                    alert.setContentText(getString("dialog.tag_exists.content"));
                    alert.showAndWait();
                } else {
                    Tag newTag = new Tag(tagName);
                    tagDAO.createTag(newTag);
                    loadTags(); // Refresh tag list
                    updateStatus(java.text.MessageFormat.format(getString("status.tag_created"), tagName));
                }
            } catch (Exception e) {
                logger.severe("Failed to create tag: " + e.getMessage());
                updateStatus(getString("status.error") + ": " + e.getMessage());
            }
        }
    }

    /**
     * Insert Markdown formatting at cursor position or around selected text.
     */
    private void insertMarkdownFormat(String prefix, String suffix) {
        if (noteContentArea == null)
            return;

        String selectedText = noteContentArea.getSelectedText();

        if (selectedText != null && !selectedText.isEmpty()) {
            // Replace selected text with formatted version
            String formatted = prefix + selectedText + suffix;
            noteContentArea.replaceSelection(formatted);
        } else {
            // Insert at cursor position
            int caretPos = noteContentArea.getCaretPosition();
            String text = noteContentArea.getText() != null ? noteContentArea.getText() : "";
            String newText = text.substring(0, caretPos) + prefix + suffix + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + prefix.length());
        }
        noteContentArea.requestFocus();
        isModified = true;
    }

    @FXML
    private void handleBold(ActionEvent event) {
        insertMarkdownFormat("**", "**");
        updateStatus(getString("status.bold"));
    }

    @FXML
    private void handleItalic(ActionEvent event) {
        insertMarkdownFormat("*", "*");
        updateStatus(getString("status.italic"));
    }

    @FXML
    private void handleUnderline(ActionEvent event) {
        // Markdown doesn't have underline, but we can use HTML in preview
        insertMarkdownFormat("<u>", "</u>");
        updateStatus(getString("status.underline"));
    }

    @FXML
    private void handleLink(ActionEvent event) {
        if (noteContentArea == null)
            return;

        TextInputDialog dialog = new TextInputDialog("https://");
        dialog.setTitle("Insert Link");
        dialog.setHeaderText("Enter URL:");
        dialog.setContentText("URL:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String url = result.get().trim();
            String selectedText = noteContentArea.getSelectedText();
            String linkText = (selectedText != null && !selectedText.isEmpty()) ? selectedText : "link text";
            String markdownLink = "[" + linkText + "](" + url + ")";

            if (selectedText != null && !selectedText.isEmpty()) {
                noteContentArea.replaceSelection(markdownLink);
            } else {
                int caretPos = noteContentArea.getCaretPosition();
                String text = noteContentArea.getText();
                String newText = text.substring(0, caretPos) + markdownLink + text.substring(caretPos);
                noteContentArea.setText(newText);
                noteContentArea.positionCaret(caretPos + markdownLink.length());
            }
            noteContentArea.requestFocus();
            isModified = true;
            updateStatus(getString("status.link"));
        }
    }

    @FXML
    private void handleImage(ActionEvent event) {
        if (noteContentArea == null)
            return;

        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Insert Image");
        dialog.setHeaderText("Enter image URL or path:");
        dialog.setContentText("Image:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String imagePath = result.get().trim();
            String selectedText = noteContentArea.getSelectedText();
            String altText = (selectedText != null && !selectedText.isEmpty()) ? selectedText : "image";
            String markdownImage = "![" + altText + "](" + imagePath + ")";

            if (selectedText != null && !selectedText.isEmpty()) {
                noteContentArea.replaceSelection(markdownImage);
            } else {
                int caretPos = noteContentArea.getCaretPosition();
                String text = noteContentArea.getText();
                String newText = text.substring(0, caretPos) + markdownImage + text.substring(caretPos);
                noteContentArea.setText(newText);
                noteContentArea.positionCaret(caretPos + markdownImage.length());
            }
            noteContentArea.requestFocus();
            isModified = true;
            updateStatus(getString("status.image"));
        }
    }

    @FXML
    private void handleAttachment(ActionEvent event) {
        // Attachments would require file storage - placeholder for now
        updateStatus(getString("status.attachments_not_supported"));
    }

    @FXML
    private void handleTodoList(ActionEvent event) {
        if (noteContentArea == null)
            return;

        int caretPos = noteContentArea.getCaretPosition();
        String text = noteContentArea.getText();
        String newLine = "- [ ] ";

        // Check if we're at the start of a line
        int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
        String lineText = text.substring(lineStart, caretPos);

        if (lineText.trim().isEmpty()) {
            // Insert at current position
            String newText = text.substring(0, caretPos) + newLine + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + newLine.length());
        } else {
            // Insert on new line
            String newText = text.substring(0, caretPos) + "\n" + newLine + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + newLine.length() + 1);
        }
        noteContentArea.requestFocus();
        isModified = true;
        updateStatus(getString("status.todo"));
    }

    @FXML
    private void handleNumberedList(ActionEvent event) {
        if (noteContentArea == null)
            return;

        int caretPos = noteContentArea.getCaretPosition();
        String text = noteContentArea.getText();
        String newLine = "1. ";

        // Check if we're at the start of a line
        int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
        String lineText = text.substring(lineStart, caretPos);

        if (lineText.trim().isEmpty()) {
            // Insert at current position
            String newText = text.substring(0, caretPos) + newLine + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + newLine.length());
        } else {
            // Insert on new line
            String newText = text.substring(0, caretPos) + "\n" + newLine + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + newLine.length() + 1);
        }
        noteContentArea.requestFocus();
        isModified = true;
        updateStatus(getString("status.number"));
    }

    @FXML
    private void handleHeading1(ActionEvent event) {
        if (noteContentArea == null)
            return;
        insertLinePrefix("# ");
        updateStatus(getString("status.h1"));
    }

    @FXML
    private void handleHeading2(ActionEvent event) {
        if (noteContentArea == null)
            return;
        insertLinePrefix("## ");
        updateStatus(getString("status.h2"));
    }

    @FXML
    private void handleBulletList(ActionEvent event) {
        if (noteContentArea == null)
            return;
        insertLinePrefix("- ");
        updateStatus(getString("status.bullet"));
    }

    @FXML
    private void handleCode(ActionEvent event) {
        if (noteContentArea == null)
            return;

        String selectedText = noteContentArea.getSelectedText();
        if (selectedText != null && selectedText.contains("\n")) {
            // Multi-line: wrap in code block
            insertMarkdownFormat("```\n", "\n```");
        } else {
            // Single line: inline code
            insertMarkdownFormat("`", "`");
        }
        updateStatus(getString("status.code"));
    }

    @FXML
    private void handleQuote(ActionEvent event) {
        if (noteContentArea == null)
            return;
        insertLinePrefix("> ");
        updateStatus(getString("status.quote"));
    }

    @FXML
    private void handleHeading3(ActionEvent event) {
        if (noteContentArea == null)
            return;
        insertLinePrefix("### ");
        updateStatus(getString("status.h3"));
    }

    @FXML
    private void handleRealUnderline(ActionEvent event) {
        insertMarkdownFormat("<u>", "</u>");
        updateStatus(getString("status.underline"));
    }

    @FXML
    private void handleHighlight(ActionEvent event) {
        insertMarkdownFormat("==", "==");
        updateStatus(getString("status.highlight"));
    }

    /**
     * Insert a prefix at the start of the current line.
     */
    private void insertLinePrefix(String prefix) {
        int caretPos = noteContentArea.getCaretPosition();
        String text = noteContentArea.getText() != null ? noteContentArea.getText() : "";

        // Find line start
        int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
        String lineText = text.substring(lineStart, caretPos);

        if (lineText.trim().isEmpty() && lineStart == caretPos) {
            // At the beginning of an empty line
            String newText = text.substring(0, caretPos) + prefix + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + prefix.length());
        } else {
            // Insert on new line
            String newText = text.substring(0, caretPos) + "\n" + prefix + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + prefix.length() + 1);
        }
        noteContentArea.requestFocus();
        isModified = true;
    }

    private void handleRenameFolder(Folder folder) {
        try {
            if (folder == null) {
                return;
            }

            // Reload folder from database to ensure we have the latest data
            Folder folderToRename = folderDAO.getFolderById(folder.getId());
            if (folderToRename != null) {
                TextInputDialog dialog = new TextInputDialog(folderToRename.getTitle());
                dialog.setTitle(getString("dialog.rename_folder.title"));
                dialog.setHeaderText(getString("dialog.rename_folder.header"));
                dialog.setContentText(getString("dialog.rename_folder.content"));

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent() && !result.get().trim().isEmpty()
                        && !result.get().equals(folderToRename.getTitle())) {
                    folderToRename.setTitle(result.get().trim());
                    folderDAO.updateFolder(folderToRename);
                    loadFolders();
                    updateStatus(java.text.MessageFormat.format(getString("status.renamed_folder"), result.get()));
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to rename folder: " + e.getMessage());
            updateStatus(getString("status.error_renaming_folder"));
        }
    }

    private void handleDeleteFolder(Folder folder) {
        try {
            if (folder == null) {
                return;
            }

            // Reload folder from database to ensure we have the latest data
            Folder folderToDelete = folderDAO.getFolderById(folder.getId());
            if (folderToDelete != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle(getString("dialog.delete_folder.title"));
                alert.setHeaderText(getString("dialog.delete_folder.header"));
                alert.setContentText(getString("dialog.delete_folder.content"));

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    folderDAO.deleteFolder(folderToDelete.getId());
                    loadFolders();
                    if (currentFolder != null && currentFolder.getId().equals(folderToDelete.getId())) {
                        currentFolder = null;
                        loadAllNotes();
                    }
                    updateStatus(java.text.MessageFormat.format(getString("status.deleted_folder"),
                            folderToDelete.getTitle()));
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to delete folder: " + e.getMessage());
            updateStatus(getString("status.error_deleting_folder"));
        }
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
                tagDAO.deleteTag(tag.getId());
                loadTags();
                if (currentNote != null)
                    loadNoteTags(currentNote);
                updateStatus(java.text.MessageFormat.format(getString("status.deleted_tag"), tag.getTitle()));
            } catch (Exception e) {
                logger.severe("Failed to delete tag: " + e.getMessage());
                updateStatus(getString("status.error_deleting_tag"));
            }
        }
    }

    /**
     * Rename a tag.
     */
    private void handleRenameTag(Tag tag) {
        if (tag == null)
            return;

        TextInputDialog dialog = new TextInputDialog(tag.getTitle());
        dialog.setTitle(getString("dialog.rename_tag.title"));
        dialog.setHeaderText(getString("dialog.rename_tag.header"));
        dialog.setContentText(getString("dialog.rename_tag.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty() && !result.get().equals(tag.getTitle())) {
            try {
                tag.setTitle(result.get().trim());
                tagDAO.updateTag(tag);
                loadTags();
                if (currentNote != null)
                    loadNoteTags(currentNote);
                updateStatus(java.text.MessageFormat.format(getString("status.renamed_tag"), result.get()));
            } catch (Exception e) {
                logger.severe("Failed to rename tag: " + e.getMessage());
                updateStatus(getString("status.error_renaming_tag"));
            }
        }
    }

    /**
     * Find a tag by title.
     */
    private Tag findTagByTitle(String title) {
        if (title == null)
            return null;
        try {
            return tagDAO.fetchAllTags().stream()
                    .filter(t -> t.getTitle().equals(title))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}