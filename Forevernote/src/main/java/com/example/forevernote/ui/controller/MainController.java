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
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;

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
import com.example.forevernote.ui.components.CommandPalette;
import com.example.forevernote.ui.components.QuickSwitcher;

import javafx.stage.Stage;

import java.util.logging.Logger;
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
 */
public class MainController {
    
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
    private boolean recentListenerAdded = false;
    private boolean favoritesListenerAdded = false;
    
    // FXML UI Components
    @FXML private MenuBar menuBar;
    @FXML private SplitPane mainSplitPane;
    @FXML private SplitPane contentSplitPane;
    @FXML private SplitPane editorPreviewSplitPane;
    @FXML private TabPane navigationTabPane;
    
    // Navigation components
    @FXML private TreeView<Folder> folderTreeView;
    @FXML private ListView<String> tagListView;
    @FXML private ListView<String> recentNotesListView;
    @FXML private ListView<String> favoritesListView;
    
    // Search and toolbar
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private ToggleButton listViewButton;
    @FXML private ToggleButton gridViewButton;
    
    // Notes list
    @FXML private ListView<Note> notesListView;
    
    // Editor components
    @FXML private TextField noteTitleField;
    @FXML private TextArea noteContentArea;
    @FXML private FlowPane tagsFlowPane;
    @FXML private Label modifiedDateLabel;
    @FXML private Label wordCountLabel;
    
    // Editor/Preview panes (Obsidian-style)
    @FXML private VBox editorPane;
    @FXML private VBox previewPane;
    @FXML private ToggleButton editorOnlyButton;
    @FXML private ToggleButton splitViewButton;
    @FXML private ToggleButton previewOnlyButton;
    @FXML private Button favoriteButton;
    @FXML private Button infoButton;
    @FXML private Button deleteNoteBtn;
    
    // Toolbar buttons
    @FXML private Button newNoteBtn;
    @FXML private Button newFolderBtn;
    @FXML private Button saveBtn;
    @FXML private Button deleteBtn;
    @FXML private Button refreshBtn;
    
    // Sidebar buttons
    @FXML private Button sidebarNewNoteBtn;
    @FXML private Button sidebarNewFolderBtn;
    @FXML private Button sidebarNewTagBtn;
    
    // Format toolbar buttons
    @FXML private Button heading1Btn;
    @FXML private Button heading2Btn;
    @FXML private Button heading3Btn;
    @FXML private Button boldBtn;
    @FXML private Button italicBtn;
    @FXML private Button strikeBtn;
    @FXML private Button underlineBtn;
    @FXML private Button highlightBtn;
    @FXML private Button linkBtn;
    @FXML private Button imageBtn;
    @FXML private Button todoBtn;
    @FXML private Button bulletBtn;
    @FXML private Button numberBtn;
    @FXML private Button quoteBtn;
    @FXML private Button codeBtn;
    @FXML private Button closeInfoBtn;
    
    // Info panel (slide-out)
    @FXML private VBox infoPanel;
    
    // Preview and info
    @FXML private javafx.scene.web.WebView previewWebView;
    @FXML private Label infoCreatedLabel;
    @FXML private Label infoModifiedLabel;
    @FXML private Label infoWordsLabel;
    @FXML private Label infoCharsLabel;
    @FXML private Label infoLatitudeLabel;
    @FXML private Label infoLongitudeLabel;
    @FXML private Label infoAuthorLabel;
    @FXML private Label infoSourceUrlLabel;
    
    // Status bar
    @FXML private Label statusLabel;
    @FXML private Label noteCountLabel;
    @FXML private Label syncStatusLabel;
    
    // Theme menu items
    @FXML private RadioMenuItem lightThemeMenuItem;
    @FXML private RadioMenuItem darkThemeMenuItem;
    @FXML private RadioMenuItem systemThemeMenuItem;
    private ToggleGroup themeToggleGroup;
    
    // View mode state
    private enum ViewMode { EDITOR_ONLY, SPLIT, PREVIEW_ONLY }
    private ViewMode currentViewMode = ViewMode.SPLIT;
    
    // UI Components for quick access
    private CommandPalette commandPalette;
    private QuickSwitcher quickSwitcher;
    
    /**
     * Initialize the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        try {
            // Initialize database connections
            initializeDatabase();
            
            // Initialize theme toggle group
            initializeThemeMenu();
            
            // Initialize UI components
            initializeFolderTree();
            initializeNotesList();
            initializeEditor();
            initializeSearch();
            initializeSortOptions();
            initializeViewModeButtons();
            initializeIcons();
            
            // Load initial data
            loadFolders();
            loadRecentNotes();
            loadTags();
            loadFavorites();
            
            // Initialize keyboard shortcuts after scene is ready
            Platform.runLater(this::initializeKeyboardShortcuts);
            
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
    private void initializeDatabase() {
        try {
            SQLiteDB db = SQLiteDB.getInstance();
            connection = db.openConnection();
            factoryDAO = FactoryDAO.getFactory(FactoryDAO.SQLITE_FACTORY, connection);
            folderDAO = factoryDAO.getFolderDAO();
            noteDAO = factoryDAO.getNoteDAO();
            tagDAO = factoryDAO.getLabelDAO();
            
            logger.info("Database connections initialized");
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Initialize the folder tree view.
     */
    private void initializeFolderTree() {
        // Create a visible root folder for "All Notes" (like Evernote/Joplin/Obsidian)
        Folder rootFolder = new Folder("All Notes", null, null);
        TreeItem<Folder> rootItem = new TreeItem<>(rootFolder);
        rootItem.setExpanded(true);
        folderTreeView.setRoot(rootItem);
        folderTreeView.setShowRoot(true);
        
        // Handle folder selection
        folderTreeView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null && newValue.getValue() != null) {
                    Folder selectedFolder = newValue.getValue();
                    if (selectedFolder.getTitle().equals("All Notes")) {
                        currentFolder = null;
                        loadAllNotes();
                    } else {
                        handleFolderSelection(selectedFolder);
                    }
                } else {
                    currentFolder = null;
                    loadAllNotes();
                }
            }
        );
        
        // Enable context menu with note count display (Obsidian-style)
        folderTreeView.setCellFactory(tv -> {
            TreeCell<Folder> cell = new TreeCell<Folder>() {
                private final HBox container = new HBox(6);
                private final Label iconLabel = new Label();
                private final Label nameLabel = new Label();
                private final Label countLabel = new Label();
                private final Region spacer = new Region();
                
                {
                    container.setAlignment(Pos.CENTER_LEFT);
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    iconLabel.getStyleClass().add("folder-icon");
                    nameLabel.getStyleClass().add("folder-name");
                    countLabel.getStyleClass().add("folder-count");
                    container.getChildren().addAll(iconLabel, nameLabel, spacer, countLabel);
                }
                
                @Override
                protected void updateItem(Folder folder, boolean empty) {
                    super.updateItem(folder, empty);
                    if (empty || folder == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        // Simple text icons that always work
                        TreeItem<Folder> treeItem = getTreeItem();
                        boolean hasChildren = treeItem != null && !treeItem.getChildren().isEmpty();
                        boolean isExpanded = treeItem != null && treeItem.isExpanded();
                        
                        if (folder.getTitle().equals("All Notes")) {
                            iconLabel.setText("=");
                        } else if (hasChildren && isExpanded) {
                            iconLabel.setText("v");
                        } else if (hasChildren) {
                            iconLabel.setText(">");
                        } else {
                            iconLabel.setText("-");
                        }
                        
                        nameLabel.setText(folder.getTitle());
                        int noteCount = getNoteCountForFolder(folder);
                        countLabel.setText(noteCount > 0 ? String.valueOf(noteCount) : "");
                        setText(null);
                        setGraphic(container);
                    }
                }
            };
            
            cell.setOnContextMenuRequested(event -> {
                if (!cell.isEmpty() && cell.getItem() != null) {
                    Folder folder = cell.getItem();
                    String title = folder.getTitle();
                    if (!title.equals("All Notes")) {
                        showFolderContextMenu(folder, cell);
                    }
                }
            });
            
            return cell;
        });
    }
    
    /**
     * Get the count of notes in a folder (including subfolders recursively).
     */
    private int getNoteCountForFolder(Folder folder) {
        try {
            if (folder == null) return 0;
            
            // "All Notes" shows total count
            if (folder.getTitle().equals("All Notes")) {
                List<Note> allNotes = noteDAO.fetchAllNotes();
                return allNotes != null ? allNotes.size() : 0;
            }
            
            // Get notes directly in this folder
            folderDAO.loadNotes(folder);
            int count = 0;
            for (Component child : folder.getChildren()) {
                if (child instanceof Note) {
                    count++;
                }
            }
            
            // Add notes from subfolders recursively
            folderDAO.loadSubFolders(folder);
            for (Component child : folder.getChildren()) {
                if (child instanceof Folder) {
                    count += getNoteCountForFolder((Folder) child);
                }
            }
            
            return count;
        } catch (Exception e) {
            logger.warning("Error counting notes for folder " + folder.getTitle() + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Initialize the notes list view.
     */
    private void initializeNotesList() {
        notesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                } else {
                    setText(note.getTitle());
                }
            }
        });
        
        // Handle note selection
        notesListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    loadNoteInEditor(newValue);
                }
            }
        );
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
                            "if (document.documentElement) document.documentElement.style.backgroundColor = '#1E1E1E';"
                        );
                    } else {
                        previewWebView.getEngine().executeScript(
                            "document.body.style.backgroundColor = '#FFFFFF'; " +
                            "if (document.documentElement) document.documentElement.style.backgroundColor = '#FFFFFF';"
                        );
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
    
    /**
     * Update theme menu selection based on current theme.
     */
    private void updateThemeMenuSelection() {
        if (themeToggleGroup == null) return;
        
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
                handleShowNoteInfo(null);
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
                logger.warning("Unknown command: " + commandName);
                updateStatus("Unknown command: " + commandName);
        }
    }
    
    /**
     * Initialize sort options.
     */
    private void initializeSortOptions() {
        sortComboBox.getItems().addAll(
            "Title (A-Z)",
            "Title (Z-A)", 
            "Created (Newest)",
            "Created (Oldest)",
            "Modified (Newest)",
            "Modified (Oldest)"
        );
        sortComboBox.getSelectionModel().selectFirst();
        
        sortComboBox.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                sortNotes(newValue);
            }
        );
    }
    
    /**
     * Initialize view mode toggle buttons (Obsidian-style).
     */
    private void initializeViewModeButtons() {
        // Create toggle group for view modes
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
        
        // Apply initial view mode
        applyViewMode();
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
        updateStatus("Editor mode");
    }
    
    /**
     * Handle split view mode button click.
     */
    @FXML
    private void handleSplitViewMode(ActionEvent event) {
        currentViewMode = ViewMode.SPLIT;
        applyViewMode();
        updateStatus("Split view mode");
    }
    
    /**
     * Handle preview-only mode button click.
     */
    @FXML
    private void handlePreviewOnlyMode(ActionEvent event) {
        currentViewMode = ViewMode.PREVIEW_ONLY;
        applyViewMode();
        updateStatus("Preview mode");
    }
    
    /**
     * Initialize all icons using Ikonli (Feather icons - similar to Obsidian).
     * Uses simple text labels that work reliably across all platforms.
     */
    private void initializeIcons() {
        // Toolbar buttons
        setButtonText(newNoteBtn, "+", "New Note (Ctrl+N)");
        setButtonText(newFolderBtn, "📁", "New Folder");
        setButtonText(saveBtn, "💾", "Save (Ctrl+S)");
        setButtonText(deleteBtn, "🗑", "Delete");
        setButtonText(refreshBtn, "↻", "Refresh");
        
        // Sidebar buttons
        setButtonText(sidebarNewNoteBtn, "+", "New Note");
        setButtonText(sidebarNewFolderBtn, "📁", "New Folder");
        setButtonText(sidebarNewTagBtn, "#", "New Tag");
        
        // View mode buttons
        setToggleText(editorOnlyButton, "✎", "Editor only (Alt+1)");
        setToggleText(splitViewButton, "▦", "Split view (Alt+2)");
        setToggleText(previewOnlyButton, "👁", "Preview only (Alt+3)");
        
        // Action buttons  
        setButtonText(favoriteButton, "☆", "Add to favorites");
        setButtonText(infoButton, "ℹ", "Note information");
        setButtonText(deleteNoteBtn, "✕", "Delete note");
        setButtonText(closeInfoBtn, "✕", "Close panel");
        
        // Format toolbar
        setButtonText(heading1Btn, "H1", "Heading 1");
        setButtonText(heading2Btn, "H2", "Heading 2");
        setButtonText(heading3Btn, "H3", "Heading 3");
        setButtonText(boldBtn, "B", "Bold (Ctrl+B)");
        setButtonText(italicBtn, "I", "Italic (Ctrl+I)");
        setButtonText(strikeBtn, "S", "Strikethrough");
        setButtonText(underlineBtn, "U", "Underline");
        setButtonText(highlightBtn, "~", "Highlight");
        setButtonText(linkBtn, "Lk", "Insert link (Ctrl+K)");
        setButtonText(imageBtn, "Img", "Insert image");
        setButtonText(todoBtn, "[]", "Checkbox");
        setButtonText(bulletBtn, "•", "Bullet list");
        setButtonText(numberBtn, "1.", "Numbered list");
        setButtonText(quoteBtn, ">", "Blockquote");
        setButtonText(codeBtn, "</>", "Code block");
        
        logger.info("Button labels initialized");
    }
    
    private void setButtonText(Button btn, String text, String tooltip) {
        if (btn != null) {
            btn.setText(text);
            btn.setTooltip(new Tooltip(tooltip));
        }
    }
    
    private void setToggleText(ToggleButton btn, String text, String tooltip) {
        if (btn != null) {
            btn.setText(text);
            btn.setTooltip(new Tooltip(tooltip));
        }
    }
    
    /**
     * Handle show note info panel.
     */
    @FXML
    private void handleShowNoteInfo(ActionEvent event) {
        if (infoPanel != null) {
            boolean isVisible = infoPanel.isVisible();
            infoPanel.setVisible(!isVisible);
            infoPanel.setManaged(!isVisible);
            
            if (!isVisible && currentNote != null) {
                updateNoteInfoPanel();
            }
        }
    }
    
    /**
     * Handle close info panel button.
     */
    @FXML
    private void handleCloseInfoPanel(ActionEvent event) {
        if (infoPanel != null) {
            infoPanel.setVisible(false);
            infoPanel.setManaged(false);
        }
    }
    
    /**
     * Update the note info panel with current note data.
     */
    private void updateNoteInfoPanel() {
        if (currentNote == null) return;
        
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
            infoLatitudeLabel.setText("Lat: " + (currentNote.getLatitude() != 0 ? currentNote.getLatitude() : "-"));
        }
        if (infoLongitudeLabel != null) {
            infoLongitudeLabel.setText("Lon: " + (currentNote.getLongitude() != 0 ? currentNote.getLongitude() : "-"));
        }
        if (infoAuthorLabel != null) {
            infoAuthorLabel.setText("Author: " + (currentNote.getAuthor() != null && !currentNote.getAuthor().isEmpty() ? currentNote.getAuthor() : "-"));
        }
        if (infoSourceUrlLabel != null) {
            infoSourceUrlLabel.setText("URL: " + (currentNote.getSourceUrl() != null && !currentNote.getSourceUrl().isEmpty() ? currentNote.getSourceUrl() : "-"));
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
            // We need to check parent_id directly from database since getParent() may not be loaded
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
            updateStatus("Error loading folders");
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
            noteCountLabel.setText(notes.size() + " notes");
            currentFolder = null;
            currentTag = null;
            currentFilterType = "all";
            updateStatus("Loaded all notes");
        } catch (Exception e) {
            logger.severe("Failed to load all notes: " + e.getMessage());
            updateStatus("Error loading notes");
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
                folderDAO.loadNotes(currentFolder);
                
                List<Note> notes = currentFolder.getChildren().stream()
                    .filter(c -> c instanceof Note)
                    .map(c -> (Note) c)
                    .toList();
                
                notesListView.getItems().setAll(notes);
                noteCountLabel.setText(notes.size() + " notes");
                currentFilterType = "folder";
                currentTag = null;
                updateStatus("Loaded folder: " + currentFolder.getTitle());
            }
        } catch (Exception e) {
            logger.severe("Failed to load folder " + (folder != null ? folder.getTitle() : "null") + ": " + e.getMessage());
            updateStatus("Error loading folder");
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
        
        currentNote = note;
        noteTitleField.setText(note.getTitle() != null ? note.getTitle() : "");
        noteContentArea.setText(note.getContent() != null ? note.getContent() : "");
        
        // Load tags
        loadNoteTags(note);
        
        // Update metadata
        updateNoteMetadata(note);
        
        // Ensure WebView has correct background color based on theme
        if ("dark".equals(currentTheme) && previewWebView != null) {
            previewWebView.setStyle("-fx-background-color: #1E1E1E;");
        } else if (previewWebView != null) {
            previewWebView.setStyle("-fx-background-color: #FFFFFF;");
        }
        
        // Update preview
        updatePreview();
        
        // Update favorite button icon
        updateFavoriteButtonIcon();
        
        // Refresh favorites list to show current favorite status
        loadFavorites();
        
        isModified = false;
        updateStatus("Loaded note: " + note.getTitle());
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
                final int tagId = tag.getId();
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
            infoLatitudeLabel.setText("Lat: " + (note.getLatitude() != 0 ? note.getLatitude() : "-"));
        }
        if (infoLongitudeLabel != null) {
            infoLongitudeLabel.setText("Lon: " + (note.getLongitude() != 0 ? note.getLongitude() : "-"));
        }
        if (infoAuthorLabel != null) {
            infoAuthorLabel.setText("Author: " + (note.getAuthor() != null && !note.getAuthor().isEmpty() ? note.getAuthor() : "-"));
        }
        if (infoSourceUrlLabel != null) {
            infoSourceUrlLabel.setText("URL: " + (note.getSourceUrl() != null && !note.getSourceUrl().isEmpty() ? note.getSourceUrl() : "-"));
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
                String dateA = a.getModifiedDate() != null ? a.getModifiedDate() : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String dateB = b.getModifiedDate() != null ? b.getModifiedDate() : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
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
                    }
                );
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
            
            favoritesListView.getItems().setAll(favoriteTitles);
            
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
                                
                                // Clear selection and items to avoid IndexOutOfBoundsException
                                notesListView.getSelectionModel().clearSelection();
                                notesListView.getItems().clear();
                                
                                // Refresh favorites list and show in notes list
                                List<Note> currentFavorites = new ArrayList<>(cachedFavoriteNotes);
                                notesListView.getItems().addAll(currentFavorites);
                                noteCountLabel.setText(currentFavorites.size() + " favorite notes");
                                
                                // Use Platform.runLater to ensure list update completes before loading note
                                final Note noteToLoad = favoriteNote.get();
                                Platform.runLater(() -> {
                                    try {
                                        int index = notesListView.getItems().indexOf(noteToLoad);
                                        if (index >= 0) {
                                            notesListView.getSelectionModel().select(index);
                                        }
                                        loadNoteInEditor(noteToLoad);
                                    } catch (Exception e) {
                                        logger.warning("Could not select favorite note in list: " + e.getMessage());
                                        loadNoteInEditor(noteToLoad);
                                    }
                                });
                            }
                        }
                    }
                );
            }
        } catch (Exception e) {
            logger.warning("Failed to load favorites: " + e.getMessage());
        }
    }
    
    /**
     * Load tags.
     */
    private void loadTags() {
        try {
            List<Tag> tags = tagDAO.fetchAllTags();
            tagListView.getItems().clear();
            
            // Store tags in a map for quick lookup
            for (Tag tag : tags) {
                tagListView.getItems().add(tag.getTitle());
            }
            
            // Add listener for tag selection
            tagListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        handleTagSelection(newValue);
                    } else {
                        // Deselected - show all notes
                        loadAllNotes();
                    }
                }
            );
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
                noteCountLabel.setText(notesWithTag.size() + " notes with tag: " + tagName);
                updateStatus("Filtered by tag: " + tagName);
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
            noteCountLabel.setText(filteredNotes.size() + " notes found");
            currentFilterType = "search";
            updateStatus("Search: " + searchText);
        } catch (Exception e) {
            logger.severe("Failed to perform search: " + e.getMessage());
            updateStatus("Search failed");
        }
    }
    
    /**
     * Sort notes with null-safe comparisons.
     */
    private void sortNotes(String sortOption) {
        if (sortOption == null) return;
        
        List<Note> notes = new ArrayList<>(notesListView.getItems());
        
        switch (sortOption) {
            case "Title (A-Z)":
                notes.sort((a, b) -> {
                    String titleA = a.getTitle() != null ? a.getTitle() : "";
                    String titleB = b.getTitle() != null ? b.getTitle() : "";
                    return titleA.compareToIgnoreCase(titleB);
                });
                break;
            case "Title (Z-A)":
                notes.sort((a, b) -> {
                    String titleA = a.getTitle() != null ? a.getTitle() : "";
                    String titleB = b.getTitle() != null ? b.getTitle() : "";
                    return titleB.compareToIgnoreCase(titleA);
                });
                break;
            case "Created (Newest)":
                notes.sort((a, b) -> {
                    String dateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
                    String dateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
                    return dateB.compareTo(dateA);
                });
                break;
            case "Created (Oldest)":
                notes.sort((a, b) -> {
                    String dateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
                    String dateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
                    return dateA.compareTo(dateB);
                });
                break;
            case "Modified (Newest)":
                notes.sort((a, b) -> {
                    String dateA = a.getModifiedDate() != null ? a.getModifiedDate() : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                    String dateB = b.getModifiedDate() != null ? b.getModifiedDate() : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                    return dateB.compareTo(dateA);
                });
                break;
            case "Modified (Oldest)":
                notes.sort((a, b) -> {
                    String dateA = a.getModifiedDate() != null ? a.getModifiedDate() : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                    String dateB = b.getModifiedDate() != null ? b.getModifiedDate() : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                    return dateA.compareTo(dateB);
                });
                break;
        }
        
        notesListView.getItems().setAll(notes);
    }
    
    /**
     * Update word count.
     */
    private void updateWordCount() {
        String content = noteContentArea != null ? noteContentArea.getText() : "";
        if (content == null) content = "";
        
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
     * Update preview.
     */
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
                
                // Create a complete HTML document with theme-aware styling
                String fullHtml = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
                    "    <style>\n" +
                    "        @import url('https://fonts.googleapis.com/css2?family=Noto+Color+Emoji&display=swap');\n" +
                    "        html { " +
                    (isDarkTheme ? "background-color: #1E1E1E;" : "background-color: #FFFFFF;") +
                    " margin: 0; padding: 0; width: 100%; height: 100%; }\n" +
                    (isDarkTheme ? 
                    // Dark theme styles (standard dark theme colors)
                    "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; padding: 20px; line-height: 1.6; color: #FFFFFF; background-color: #1E1E1E; }\n" +
                    "        h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 600; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; color: #FFFFFF; }\n" +
                    "        h1 { font-size: 2em; border-bottom: 2px solid #404040; padding-bottom: 0.3em; }\n" +
                    "        h2 { font-size: 1.5em; border-bottom: 1px solid #404040; padding-bottom: 0.3em; }\n" +
                    "        h3 { font-size: 1.25em; }\n" +
                    "        code { background-color: #2D2D2D; color: #A5B4FC; padding: 2px 4px; border-radius: 3px; font-family: 'Courier New', monospace; }\n" +
                    "        pre { background-color: #2D2D2D; color: #FFFFFF; padding: 10px; border-radius: 5px; overflow-x: auto; border: 1px solid #404040; }\n" +
                    "        pre code { background-color: transparent; padding: 0; color: #A5B4FC; }\n" +
                    "        blockquote { border-left: 4px solid #818CF8; margin: 0; padding-left: 20px; color: #B3B3B3; background-color: #2D2D2D; padding: 10px 20px; border-radius: 4px; }\n" +
                    "        ul, ol { margin: 1em 0; padding-left: 2em; color: #FFFFFF; }\n" +
                    "        li { margin: 0.5em 0; }\n" +
                    "        table { border-collapse: collapse; width: 100%; margin: 1em 0; }\n" +
                    "        table th, table td { border: 1px solid #404040; padding: 8px; text-align: left; }\n" +
                    "        table th { background-color: #2D2D2D; font-weight: 600; color: #FFFFFF; }\n" +
                    "        table td { background-color: #1E1E1E; color: #FFFFFF; }\n" +
                    "        a { color: #818CF8; text-decoration: none; }\n" +
                    "        a:hover { color: #A5B4FC; text-decoration: underline; }\n" +
                    "        img { max-width: 100%; height: auto; border-radius: 4px; }\n" +
                    "        hr { border: none; border-top: 1px solid #404040; margin: 2em 0; }\n" +
                    "        strong { color: #FFFFFF; font-weight: 600; }\n" +
                    "        em { color: #FFFFFF; font-style: italic; }\n" +
                    "        /* Emoji support */\n" +
                    "        * { font-variant-emoji: emoji; }\n" :
                    // Light theme styles
                    "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; padding: 20px; line-height: 1.6; color: #18181B; background-color: #FFFFFF; }\n" +
                    "        h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 600; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; color: #18181B; }\n" +
                    "        h1 { font-size: 2em; border-bottom: 2px solid #E4E4E7; padding-bottom: 0.3em; }\n" +
                    "        h2 { font-size: 1.5em; border-bottom: 1px solid #E4E4E7; padding-bottom: 0.3em; }\n" +
                    "        h3 { font-size: 1.25em; }\n" +
                    "        code { background-color: #F5F5F5; color: #6366F1; padding: 2px 4px; border-radius: 3px; font-family: 'Courier New', monospace; }\n" +
                    "        pre { background-color: #F5F5F5; color: #18181B; padding: 10px; border-radius: 5px; overflow-x: auto; border: 1px solid #E4E4E7; }\n" +
                    "        pre code { background-color: transparent; padding: 0; color: #6366F1; }\n" +
                    "        blockquote { border-left: 4px solid #6366F1; margin: 0; padding-left: 20px; color: #71717A; background-color: #FAFAFA; padding: 10px 20px; border-radius: 4px; }\n" +
                    "        ul, ol { margin: 1em 0; padding-left: 2em; color: #18181B; }\n" +
                    "        li { margin: 0.5em 0; }\n" +
                    "        table { border-collapse: collapse; width: 100%; margin: 1em 0; }\n" +
                    "        table th, table td { border: 1px solid #E4E4E7; padding: 8px; text-align: left; }\n" +
                    "        table th { background-color: #F5F5F5; font-weight: 600; color: #18181B; }\n" +
                    "        table td { background-color: #FFFFFF; color: #18181B; }\n" +
                    "        a { color: #6366F1; text-decoration: none; }\n" +
                    "        a:hover { color: #4F46E5; text-decoration: underline; }\n" +
                    "        img { max-width: 100%; height: auto; border-radius: 4px; }\n" +
                    "        hr { border: none; border-top: 1px solid #E4E4E7; margin: 2em 0; }\n" +
                    "        strong { color: #18181B; font-weight: 600; }\n" +
                    "        em { color: #18181B; font-style: italic; }\n" +
                    "        /* Emoji support */\n" +
                    "        * { font-variant-emoji: emoji; }\n") +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    html +
                    "\n</body>\n" +
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
                    (isDarkTheme ? "color: #B3B3B3; background-color: #1E1E1E;" : "color: #71717A; background-color: #FFFFFF;") +
                    " margin: 0; padding: 0; width: 100%; height: 100%; }\n" +
                    "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; }\n" +
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
            updateStatus("No note selected");
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
                    int tagId = tagDAO.createTag(tag);
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
                    updateStatus("Added tag: " + tagName);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to add tag: " + e.getMessage());
            updateStatus("Error adding tag");
        }
    }
    
    /**
     * Show folder context menu.
     */
    private void showFolderContextMenu(Folder folder, TreeCell<Folder> cell) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem newNoteItem = new MenuItem("New Note");
        newNoteItem.setOnAction(e -> {
            currentFolder = folder;
            handleNewNote(e);
        });

        MenuItem newFolderItem = new MenuItem("New Subfolder");
        newFolderItem.setOnAction(e -> {
            currentFolder = folder;
            handleNewSubfolder(e);
        });
        
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> handleRenameFolder(folder));
        
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDeleteFolder(folder));
        
        contextMenu.getItems().addAll(newNoteItem, newFolderItem, renameItem, deleteItem);
        contextMenu.show(cell, cell.getLayoutX(), cell.getLayoutY());
    }
    
    /**
     * Remove tag from note.
     */
    private void removeTagFromNote(Tag tag) {
        if (currentNote == null) {
            updateStatus("No note selected");
            return;
        }
        if (tag == null || tag.getId() == null) {
            updateStatus("Invalid tag");
            return;
        }
        
        // Confirm removal
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Tag");
        confirm.setHeaderText("Remove tag '" + tag.getTitle() + "' from this note?");
        confirm.setContentText("This will only remove the tag from this note, not delete the tag itself.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                noteDAO.removeTag(currentNote, tag);
                loadNoteTags(currentNote);
                updateStatus("Removed tag: " + tag.getTitle());
            } catch (Exception e) {
                logger.severe("Failed to remove tag: " + e.getMessage());
                updateStatus("Error removing tag: " + e.getMessage());
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
            Note newNote = new Note("New Note", "");
            int noteId = noteDAO.createNote(newNote);
            newNote.setId(noteId);
            
            if (currentFolder != null) {
                folderDAO.addNote(currentFolder, newNote);
            }
            
            notesListView.getItems().add(0, newNote);
            notesListView.getSelectionModel().select(newNote);
            loadNoteInEditor(newNote);
            
            // Refresh recent notes to include new note
            loadRecentNotes();
            
            updateStatus("Created new note");
        } catch (Exception e) {
            logger.severe("Failed to create new note: " + e.getMessage());
            updateStatus("Error creating note");
        }
    }
    
    @FXML
    private void handleNewFolder(ActionEvent event) {
        // Create dialog with option to create in root or as subfolder
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("New Folder");
        
        // Determine if we should create in root or as subfolder
        boolean createInRoot = (currentFolder == null || 
            currentFolder.getTitle().equals("All Notes") || 
            currentFolder.getTitle().equals("📚 All Notes"));
        String headerText = createInRoot 
            ? "Create a new folder in root (All Notes)" 
            : "Create a new folder in: " + currentFolder.getTitle() + "\n(Click '📚 All Notes' in tree to create in root)";
        dialog.setHeaderText(headerText);
        dialog.setContentText("Folder name:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                Folder newFolder = new Folder(result.get().trim());
                int folderId = folderDAO.createFolder(newFolder);
                newFolder.setId(folderId);
                
                // Only add as subfolder if currentFolder is set and not "All Notes"
                if (!createInRoot && currentFolder != null && 
                    !currentFolder.getTitle().equals("All Notes") && 
                    !currentFolder.getTitle().equals("📚 All Notes")) {
                    folderDAO.addSubFolder(currentFolder, newFolder);
                }
                // Otherwise, it's created in root (parent_id will be NULL)
                
                loadFolders();
                // Select "All Notes" root to make it clear where new folders are created
                folderTreeView.getSelectionModel().select(folderTreeView.getRoot());
                currentFolder = null;
                updateStatus("Created folder: " + newFolder.getTitle());
            } catch (Exception e) {
                logger.severe("Failed to create folder: " + e.getMessage());
                updateStatus("Error creating folder: " + e.getMessage());
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
                int folderId = folderDAO.createFolder(newSubfolder);
                newSubfolder.setId(folderId);
                
                folderDAO.addSubFolder(currentFolder, newSubfolder);
                
                loadFolders();
                updateStatus("Created subfolder: " + newSubfolder.getTitle());
            } catch (Exception e) {
                logger.severe("Failed to create subfolder: " + e.getMessage());
                updateStatus("Error creating subfolder");
            }
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
                
                updateStatus("Saved: " + currentNote.getTitle());
            } catch (Exception e) {
                logger.severe("Failed to save note: " + e.getMessage());
                updateStatus("Error saving note");
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
    }
    
    @FXML
    private void handleSaveAll(ActionEvent event) {
        // Save all modified notes (for now, just save current if modified)
        if (currentNote != null && isModified) {
            handleSave(event);
        }
        updateStatus("All notes saved");
    }
    
    @FXML
    private void handleDelete(ActionEvent event) {
        if (currentNote != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Note");
            alert.setHeaderText("Are you sure you want to delete this note?");
            alert.setContentText("This action cannot be undone.");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    Note noteToDelete = currentNote;
                    noteDAO.deleteNote(noteToDelete.getId());
                    
                    // Clear editor
                    currentNote = null;
                    noteTitleField.clear();
                    noteContentArea.clear();
                    tagsFlowPane.getChildren().clear();
                    previewWebView.getEngine().loadContent("", "text/html");
                    
                    // Refresh ALL lists - notes, recent, favorites
                    refreshNotesList();
                    loadRecentNotes();  // Update recent notes to remove deleted note
                    loadFavorites();    // Update favorites to remove deleted note
                    
                    updateStatus("Note deleted");
                } catch (Exception e) {
                    logger.severe("Failed to delete note: " + e.getMessage());
                    updateStatus("Error deleting note");
                }
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
    private void handleImport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Notes");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Supported Files", "*.md", "*.txt", "*.markdown"),
            new FileChooser.ExtensionFilter("Markdown Files", "*.md", "*.markdown"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
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
                    int noteId = noteDAO.createNote(newNote);
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
            String message = "Imported " + imported + " note(s)";
            if (failed > 0) {
                message += "\nFailed: " + failed + " file(s)";
            }
            updateStatus(message);
            showAlert(Alert.AlertType.INFORMATION, "Import Complete", 
                "Import finished", message);
        }
    }
    
    @FXML 
    private void handleExport(ActionEvent event) {
        if (currentNote == null) {
            showAlert(Alert.AlertType.WARNING, "Export", "No note selected", "Please select a note to export.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Note");
        fileChooser.setInitialFileName(sanitizeFileName(currentNote.getTitle()));
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
            new FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showSaveDialog(mainSplitPane.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                // Add title as header for Markdown
                if (file.getName().endsWith(".md")) {
                    writer.write("# " + currentNote.getTitle() + "\n\n");
                }
                writer.write(currentNote.getContent() != null ? currentNote.getContent() : "");
                updateStatus("Exported: " + file.getName());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                    "Note exported successfully", "Saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                logger.severe("Failed to export note: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Export Failed", 
                    "Could not export note", e.getMessage());
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
    @FXML private void handleUndo(ActionEvent event) { 
        if (noteContentArea != null) {
            noteContentArea.undo();
        }
    }
    
    @FXML private void handleRedo(ActionEvent event) { 
        // JavaFX TextArea doesn't have redo by default
        updateStatus("Redo not available in TextArea");
    }
    
    @FXML private void handleCut(ActionEvent event) { 
        if (noteContentArea != null && noteContentArea.getSelectedText() != null) {
            noteContentArea.cut();
        } else if (noteTitleField != null && noteTitleField.getSelectedText() != null) {
            noteTitleField.cut();
        }
    }
    
    @FXML private void handleCopy(ActionEvent event) { 
        if (noteContentArea != null && noteContentArea.getSelectedText() != null) {
            noteContentArea.copy();
        } else if (noteTitleField != null && noteTitleField.getSelectedText() != null) {
            noteTitleField.copy();
        }
    }
    
    @FXML private void handlePaste(ActionEvent event) { 
        if (noteContentArea != null && noteContentArea.isFocused()) {
            noteContentArea.paste();
        } else if (noteTitleField != null && noteTitleField.isFocused()) {
            noteTitleField.paste();
        }
    }
    
    @FXML private void handleFind(ActionEvent event) { 
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
                    updateStatus("Found: " + searchText);
                } else {
                    updateStatus("Text not found: " + searchText);
                }
            }
        }
    }
    
    @FXML 
    private void handleReplace(ActionEvent event) {
        if (noteContentArea == null) {
            updateStatus("No note open");
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
            new Label("Replace with:"), replaceField
        );
        
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
                    updateStatus("Replaced all occurrences");
                } else {
                    int index = noteContent.indexOf(find);
                    if (index >= 0) {
                        String newContent = noteContent.substring(0, index) + replace + 
                                          noteContent.substring(index + find.length());
                        noteContentArea.setText(newContent);
                        noteContentArea.selectRange(index, index + replace.length());
                        updateStatus("Replaced first occurrence");
                    } else {
                        updateStatus("Text not found");
                    }
                }
                isModified = true;
            }
        }
    }
    
    @FXML private void handleToggleSidebar(ActionEvent event) { 
        if (mainSplitPane != null) {
            double[] positions = mainSplitPane.getDividerPositions();
            if (positions[0] < 0.1) {
                mainSplitPane.setDividerPositions(0.25);
                updateStatus("Sidebar shown");
            } else {
                mainSplitPane.setDividerPositions(0.0);
                updateStatus("Sidebar hidden");
            }
        }
    }
    // Zoom settings
    private double currentZoom = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 3.0;
    
    // Preferences for persistence
    private static final Preferences prefs = Preferences.userNodeForPackage(MainController.class);
    
    @FXML 
    private void handleZoomIn(ActionEvent event) {
        if (currentZoom < MAX_ZOOM) {
            currentZoom = Math.min(currentZoom + ZOOM_STEP, MAX_ZOOM);
            applyZoom();
            updateStatus("Zoom: " + (int)(currentZoom * 100) + "%");
        }
    }
    
    @FXML 
    private void handleZoomOut(ActionEvent event) {
        if (currentZoom > MIN_ZOOM) {
            currentZoom = Math.max(currentZoom - ZOOM_STEP, MIN_ZOOM);
            applyZoom();
            updateStatus("Zoom: " + (int)(currentZoom * 100) + "%");
        }
    }
    
    @FXML 
    private void handleResetZoom(ActionEvent event) {
        currentZoom = 1.0;
        applyZoom();
        updateStatus("Zoom reset to 100%");
    }
    
    private void applyZoom() {
        if (noteContentArea != null) {
            noteContentArea.setStyle("-fx-font-size: " + (14 * currentZoom) + "px;");
        }
        if (noteTitleField != null) {
            noteTitleField.setStyle("-fx-font-size: " + (16 * currentZoom) + "px;");
        }
    }
    private String currentTheme = prefs.get("theme", "light"); // Load from preferences
    
    @FXML 
    private void handleLightTheme(ActionEvent event) {
        currentTheme = "light";
        prefs.put("theme", currentTheme); // Save preference
        updateThemeMenuSelection();
        applyTheme();
        updateStatus("Light theme applied");
    }
    
    @FXML 
    private void handleDarkTheme(ActionEvent event) {
        currentTheme = "dark";
        prefs.put("theme", currentTheme); // Save preference
        updateThemeMenuSelection();
        applyTheme();
        updateStatus("Dark theme applied");
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
        
        updateStatus("System theme applied (" + actualTheme + ")");
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
        scene.getStylesheets().removeIf(stylesheet -> 
            stylesheet.contains("modern-theme.css") || 
            stylesheet.contains("dark-theme.css")
        );
        
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
            if ("dark".equals(actualTheme) && previewWebView != null) {
                previewWebView.setStyle("-fx-background-color: #1E1E1E;");
                // Also set background via JavaScript to ensure it's applied
                previewWebView.getEngine().executeScript(
                    "document.body.style.backgroundColor = '#1E1E1E';"
                );
            } else if (previewWebView != null) {
                previewWebView.setStyle("-fx-background-color: #FFFFFF;");
                previewWebView.getEngine().executeScript(
                    "document.body.style.backgroundColor = '#FFFFFF';"
                );
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
            updateStatus("Search field focused - Type to search");
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
                                    updateStatus("Tag deleted: " + tag.getTitle());
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
            updateStatus("Error opening tags manager");
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
            autoSaveLabel
        );
        
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
            "For more information, visit the project repository."
        );
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
            "  F1              Show this help"
        );
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
            techLabel, copyrightLabel, developerLabel
        );
        
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
                    noteCountLabel.setText(favoriteNotes.size() + " favorite notes");
                    currentFilterType = "favorites";
                    currentFolder = null;
                    currentTag = null;
                    updateStatus("Refreshed favorites");
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
            updateStatus("Error refreshing");
        }
    }
    @FXML 
    private void handleToggleFavorite(ActionEvent event) {
        if (currentNote == null) {
            updateStatus("No note selected");
            return;
        }
        
        try {
            // Toggle favorite status
            boolean newFavoriteStatus = !currentNote.isFavorite();
            currentNote.setFavorite(newFavoriteStatus);
            
            // Save to database
            noteDAO.updateNote(currentNote);
            isModified = false; // Already saved
            
            // Update favorite button icon
            updateFavoriteButtonIcon();
            
            // Refresh favorites list if visible
            loadFavorites();
            
            updateStatus(newFavoriteStatus ? "Note marked as favorite" : "Note unmarked as favorite");
        } catch (Exception e) {
            logger.severe("Failed to toggle favorite: " + e.getMessage());
            updateStatus("Error toggling favorite");
        }
    }
    
    /**
     * Update the favorite button icon based on current note's favorite status.
     */
    private void updateFavoriteButtonIcon() {
        if (favoriteButton != null && currentNote != null) {
            if (currentNote.isFavorite()) {
                favoriteButton.setText("★");
                if (favoriteButton.getTooltip() != null) {
                    favoriteButton.getTooltip().setText("Remove from favorites");
                }
            } else {
                favoriteButton.setText("☆");
                if (favoriteButton.getTooltip() != null) {
                    favoriteButton.getTooltip().setText("Add to favorites");
                }
            }
        }
    }
    @FXML 
    private void handleNewTag(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Tag");
        dialog.setHeaderText("Create a new tag");
        dialog.setContentText("Tag name:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                String tagName = result.get().trim();
                if (tagDAO.existsByTitle(tagName)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Tag Already Exists");
                    alert.setHeaderText("A tag with this name already exists");
                    alert.setContentText("Please choose a different name.");
                    alert.showAndWait();
                } else {
                    Tag newTag = new Tag(tagName);
                    tagDAO.createTag(newTag);
                    loadTags(); // Refresh tag list
                    updateStatus("Created tag: " + tagName);
                }
            } catch (Exception e) {
                logger.severe("Failed to create tag: " + e.getMessage());
                updateStatus("Error creating tag");
            }
        }
    }
    /**
     * Insert Markdown formatting at cursor position or around selected text.
     */
    private void insertMarkdownFormat(String prefix, String suffix) {
        if (noteContentArea == null) return;
        
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
        updateStatus("Bold formatting applied");
    }
    
    @FXML 
    private void handleItalic(ActionEvent event) {
        insertMarkdownFormat("*", "*");
        updateStatus("Italic formatting applied");
    }
    
    @FXML 
    private void handleUnderline(ActionEvent event) {
        // Markdown doesn't have underline, but we can use HTML in preview
        insertMarkdownFormat("<u>", "</u>");
        updateStatus("Underline formatting applied");
    }
    
    @FXML 
    private void handleLink(ActionEvent event) {
        if (noteContentArea == null) return;
        
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
            updateStatus("Link inserted");
        }
    }
    
    @FXML 
    private void handleImage(ActionEvent event) {
        if (noteContentArea == null) return;
        
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
            updateStatus("Image inserted");
        }
    }
    
    @FXML 
    private void handleAttachment(ActionEvent event) {
        // Attachments would require file storage - placeholder for now
        updateStatus("File attachments require file storage system");
    }
    
    @FXML 
    private void handleTodoList(ActionEvent event) {
        if (noteContentArea == null) return;
        
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
        updateStatus("Todo item inserted");
    }
    
    @FXML 
    private void handleNumberedList(ActionEvent event) {
        if (noteContentArea == null) return;
        
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
        updateStatus("Numbered list item inserted");
    }
    
    @FXML 
    private void handleHeading1(ActionEvent event) {
        if (noteContentArea == null) return;
        insertLinePrefix("# ");
        updateStatus("Heading 1 inserted");
    }
    
    @FXML 
    private void handleHeading2(ActionEvent event) {
        if (noteContentArea == null) return;
        insertLinePrefix("## ");
        updateStatus("Heading 2 inserted");
    }
    
    @FXML 
    private void handleBulletList(ActionEvent event) {
        if (noteContentArea == null) return;
        insertLinePrefix("- ");
        updateStatus("Bullet list item inserted");
    }
    
    @FXML 
    private void handleCode(ActionEvent event) {
        if (noteContentArea == null) return;
        
        String selectedText = noteContentArea.getSelectedText();
        if (selectedText != null && selectedText.contains("\n")) {
            // Multi-line: wrap in code block
            insertMarkdownFormat("```\n", "\n```");
        } else {
            // Single line: inline code
            insertMarkdownFormat("`", "`");
        }
        updateStatus("Code formatting applied");
    }
    
    @FXML 
    private void handleQuote(ActionEvent event) {
        if (noteContentArea == null) return;
        insertLinePrefix("> ");
        updateStatus("Quote inserted");
    }
    
    @FXML 
    private void handleHeading3(ActionEvent event) {
        if (noteContentArea == null) return;
        insertLinePrefix("### ");
        updateStatus("Heading 3 inserted");
    }
    
    @FXML 
    private void handleRealUnderline(ActionEvent event) {
        insertMarkdownFormat("<u>", "</u>");
        updateStatus("Underline formatting applied");
    }
    
    @FXML 
    private void handleHighlight(ActionEvent event) {
        insertMarkdownFormat("==", "==");
        updateStatus("Highlight formatting applied");
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
                dialog.setTitle("Rename Folder");
                dialog.setHeaderText("Rename folder");
                dialog.setContentText("New name:");
                
                Optional<String> result = dialog.showAndWait();
                if (result.isPresent() && !result.get().trim().isEmpty() && !result.get().equals(folderToRename.getTitle())) {
                    folderToRename.setTitle(result.get().trim());
                    folderDAO.updateFolder(folderToRename);
                    loadFolders();
                    updateStatus("Renamed folder to: " + result.get());
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to rename folder: " + e.getMessage());
            updateStatus("Error renaming folder");
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
                alert.setTitle("Delete Folder");
                alert.setHeaderText("Are you sure you want to delete this folder?");
                alert.setContentText("All notes in this folder will be moved to the root.");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    folderDAO.deleteFolder(folderToDelete.getId());
                    loadFolders();
                    if (currentFolder != null && currentFolder.getId().equals(folderToDelete.getId())) {
                        currentFolder = null;
                        loadAllNotes();
                    }
                    updateStatus("Deleted folder: " + folderToDelete.getTitle());
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to delete folder: " + e.getMessage());
            updateStatus("Error deleting folder");
        }
    }
}