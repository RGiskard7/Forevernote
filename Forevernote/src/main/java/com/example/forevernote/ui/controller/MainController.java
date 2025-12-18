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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
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

import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.sql.Connection;

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
    
    // FXML UI Components
    @FXML private MenuBar menuBar;
    @FXML private SplitPane mainSplitPane;
    @FXML private SplitPane contentSplitPane;
    @FXML private SplitPane editorSplitPane;
    @FXML private TabPane navigationTabPane;
    @FXML private TabPane previewTabPane;
    
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
    @FXML private Label createdDateLabel;
    @FXML private Label modifiedDateLabel;
    @FXML private Label wordCountLabel;
    
    // Preview and info
    @FXML private javafx.scene.web.WebView previewWebView;
    @FXML private ListView<String> attachmentsListView;
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
    
    /**
     * Initialize the controller after FXML loading.
     */
    @FXML
    public void initialize() {
        try {
            // Initialize database connections
            initializeDatabase();
            
            // Initialize UI components
            initializeFolderTree();
            initializeNotesList();
            initializeEditor();
            initializeSearch();
            initializeSortOptions();
            
            // Load initial data
            loadFolders();
            loadRecentNotes();
            loadTags();
            
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
        Folder rootFolder = new Folder("📚 All Notes", null, null);
        TreeItem<Folder> rootItem = new TreeItem<>(rootFolder);
        rootItem.setExpanded(true);
        folderTreeView.setRoot(rootItem);
        folderTreeView.setShowRoot(true); // Make root visible
        
        // Handle folder selection
        folderTreeView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null && newValue.getValue() != null) {
                    Folder selectedFolder = newValue.getValue();
                    if (selectedFolder.getTitle().equals("📚 All Notes") || selectedFolder.getTitle().equals("All Notes")) {
                        currentFolder = null;
                        loadAllNotes();
                    } else {
                        handleFolderSelection(selectedFolder);
                    }
                } else {
                    // Selection cleared - show all notes
                    currentFolder = null;
                    loadAllNotes();
                }
            }
        );
        
        // Allow clicking on "All Notes" to deselect and show all notes
        // This is handled in the selection listener above
        
        // Enable context menu
        folderTreeView.setCellFactory(tv -> {
            TreeCell<Folder> cell = new TreeCell<Folder>() {
                @Override
                protected void updateItem(Folder folder, boolean empty) {
                    super.updateItem(folder, empty);
                    if (empty || folder == null) {
                        setText(null);
                    } else {
                        setText(folder.getTitle());
                    }
                }
            };
            
            cell.setOnContextMenuRequested(event -> {
                if (!cell.isEmpty() && cell.getItem() != null) {
                    Folder folder = cell.getItem();
                    String title = folder.getTitle();
                    if (!title.equals("📚 All Notes") && !title.equals("All Notes")) {
                        showFolderContextMenu(folder, cell);
                    }
                }
            });
            
            return cell;
        });
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
                Label tagLabel = new Label(tag.getTitle());
                tagLabel.getStyleClass().add("tag-label");
                tagLabel.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        removeTagFromNote(tag);
                    }
                });
                // Add tooltip
                tagLabel.setTooltip(new Tooltip("Double-click to remove"));
                tagsFlowPane.getChildren().add(tagLabel);
            }
            
            // Add button to add new tag
            Button addTagButton = new Button("+ Add Tag");
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
        createdDateLabel.setText("Created: " + (note.getCreatedDate() != null ? note.getCreatedDate() : "N/A"));
        modifiedDateLabel.setText("Modified: " + (note.getModifiedDate() != null ? note.getModifiedDate() : "N/A"));
        infoCreatedLabel.setText(note.getCreatedDate() != null ? note.getCreatedDate() : "N/A");
        infoModifiedLabel.setText(note.getModifiedDate() != null ? note.getModifiedDate() : "N/A");
        String content = note.getContent() != null ? note.getContent() : "";
        infoWordsLabel.setText(String.valueOf(countWords(content)));
        infoCharsLabel.setText(String.valueOf(content.length()));
        infoLatitudeLabel.setText(note.getLatitude() != null ? note.getLatitude().toString() : "0.0");
        infoLongitudeLabel.setText(note.getLongitude() != null ? note.getLongitude().toString() : "0.0");
        infoAuthorLabel.setText(note.getAuthor() != null ? note.getAuthor() : "N/A");
        infoSourceUrlLabel.setText(note.getSourceUrl() != null ? note.getSourceUrl() : "N/A");
    }
    
    /**
     * Load recent notes.
     */
    private void loadRecentNotes() {
        try {
            List<Note> allNotes = noteDAO.fetchAllNotes();
            // Sort by modified date (simplified)
            allNotes.sort((a, b) -> {
                String dateA = a.getModifiedDate() != null ? a.getModifiedDate() : a.getCreatedDate();
                String dateB = b.getModifiedDate() != null ? b.getModifiedDate() : b.getCreatedDate();
                return dateB.compareTo(dateA);
            });
            
            List<String> recentTitles = allNotes.stream()
                .limit(10)
                .map(Note::getTitle)
                .toList();
            
            recentNotesListView.getItems().setAll(recentTitles);
        } catch (Exception e) {
            logger.warning("Failed to load recent notes: " + e.getMessage());
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
                List<Note> notesWithTag = tagDAO.fetchAllNotesWithTag(tag.getId());
                notesListView.getItems().setAll(notesWithTag);
                noteCountLabel.setText(notesWithTag.size() + " notes with tag: " + tagName);
                currentFolder = null; // Clear folder selection when filtering by tag
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
            loadAllNotes();
            return;
        }
        
        try {
            List<Note> allNotes = noteDAO.fetchAllNotes();
            List<Note> filteredNotes = allNotes.stream()
                .filter(note -> 
                    note.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                    note.getContent().toLowerCase().contains(searchText.toLowerCase())
                )
                .toList();
            
            notesListView.getItems().setAll(filteredNotes);
            noteCountLabel.setText(filteredNotes.size() + " notes found");
            updateStatus("Search: " + searchText);
        } catch (Exception e) {
            logger.severe("Failed to perform search: " + e.getMessage());
            updateStatus("Search failed");
        }
    }
    
    /**
     * Sort notes.
     */
    private void sortNotes(String sortOption) {
        List<Note> notes = new ArrayList<>(notesListView.getItems());
        
        switch (sortOption) {
            case "Title (A-Z)":
                notes.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
                break;
            case "Title (Z-A)":
                notes.sort((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
                break;
            case "Created (Newest)":
                notes.sort((a, b) -> b.getCreatedDate().compareTo(a.getCreatedDate()));
                break;
            case "Created (Oldest)":
                notes.sort((a, b) -> a.getCreatedDate().compareTo(b.getCreatedDate()));
                break;
            case "Modified (Newest)":
                notes.sort((a, b) -> {
                    String dateA = a.getModifiedDate() != null ? a.getModifiedDate() : a.getCreatedDate();
                    String dateB = b.getModifiedDate() != null ? b.getModifiedDate() : b.getCreatedDate();
                    return dateB.compareTo(dateA);
                });
                break;
            case "Modified (Oldest)":
                notes.sort((a, b) -> {
                    String dateA = a.getModifiedDate() != null ? a.getModifiedDate() : a.getCreatedDate();
                    String dateB = b.getModifiedDate() != null ? b.getModifiedDate() : b.getCreatedDate();
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
        String content = noteContentArea.getText();
        int wordCount = countWords(content);
        wordCountLabel.setText(wordCount + " words");
        infoWordsLabel.setText(String.valueOf(wordCount));
        infoCharsLabel.setText(String.valueOf(content.length()));
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
            if (content != null && !content.trim().isEmpty()) {
                // Convert markdown to HTML
                String html = com.example.forevernote.util.MarkdownProcessor.markdownToHtml(content);
                
                // Create a complete HTML document with basic styling and emoji support
                String fullHtml = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
                    "    <style>\n" +
                    "        @import url('https://fonts.googleapis.com/css2?family=Noto+Color+Emoji&display=swap');\n" +
                    "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; padding: 20px; line-height: 1.6; color: #333; }\n" +
                    "        h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 600; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Color Emoji', sans-serif; }\n" +
                    "        h1 { font-size: 2em; border-bottom: 2px solid #eee; padding-bottom: 0.3em; }\n" +
                    "        h2 { font-size: 1.5em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }\n" +
                    "        h3 { font-size: 1.25em; }\n" +
                    "        code { background-color: #f4f4f4; padding: 2px 4px; border-radius: 3px; font-family: 'Courier New', monospace; }\n" +
                    "        pre { background-color: #f4f4f4; padding: 10px; border-radius: 5px; overflow-x: auto; }\n" +
                    "        pre code { background-color: transparent; padding: 0; }\n" +
                    "        blockquote { border-left: 4px solid #ddd; margin: 0; padding-left: 20px; color: #666; }\n" +
                    "        ul, ol { margin: 1em 0; padding-left: 2em; }\n" +
                    "        li { margin: 0.5em 0; }\n" +
                    "        table { border-collapse: collapse; width: 100%; margin: 1em 0; }\n" +
                    "        table th, table td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n" +
                    "        table th { background-color: #f4f4f4; font-weight: 600; }\n" +
                    "        a { color: #0366d6; text-decoration: none; }\n" +
                    "        a:hover { text-decoration: underline; }\n" +
                    "        img { max-width: 100%; height: auto; }\n" +
                    "        hr { border: none; border-top: 1px solid #eee; margin: 2em 0; }\n" +
                    "        /* Emoji support */\n" +
                    "        * { font-variant-emoji: emoji; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    html +
                    "\n</body>\n" +
                    "</html>";
                
                previewWebView.getEngine().loadContent(fullHtml, "text/html");
            } else {
                previewWebView.getEngine().loadContent("", "text/html");
            }
        }
    }
    
    /**
     * Convert plain text to simple HTML.
     */
    private String convertToHtml(String text) {
        if (text == null) return "";
        
        return "<html><body><pre style='font-family: Arial, sans-serif; white-space: pre-wrap;'>" 
               + text.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;")
                     .replace("\n", "<br>")
               + "</pre></body></html>";
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
        try {
            noteDAO.removeTag(currentNote, tag);
            loadNoteTags(currentNote);
            updateStatus("Removed tag: " + tag.getTitle());
        } catch (Exception e) {
            logger.severe("Failed to remove tag: " + e.getMessage());
            updateStatus("Error removing tag");
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
                    
                    // Refresh the notes list automatically
                    refreshNotesList();
                    
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
    
    // Placeholder implementations for other menu items
    @FXML 
    private void handleImport(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Import Notes");
        alert.setHeaderText("Import Feature");
        alert.setContentText("Import functionality will allow you to import notes from:\n" +
            "• Markdown files (.md)\n" +
            "• Text files (.txt)\n" +
            "• Evernote export files\n\n" +
            "This feature is coming soon.");
        alert.showAndWait();
    }
    
    @FXML 
    private void handleExport(ActionEvent event) {
        if (currentNote == null) {
            updateStatus("No note selected to export");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Note");
        alert.setHeaderText("Export Feature");
        alert.setContentText("Export functionality will allow you to export notes to:\n" +
            "• Markdown files (.md)\n" +
            "• Text files (.txt)\n" +
            "• PDF files\n\n" +
            "This feature is coming soon.\n\n" +
            "For now, you can copy the note content manually.");
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
    private double currentZoom = 1.0;
    private static final double ZOOM_STEP = 0.1;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 3.0;
    
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
    private String currentTheme = "light";
    
    @FXML 
    private void handleLightTheme(ActionEvent event) {
        currentTheme = "light";
        applyTheme();
        updateStatus("Light theme applied");
    }
    
    @FXML 
    private void handleDarkTheme(ActionEvent event) {
        currentTheme = "dark";
        applyTheme();
        updateStatus("Dark theme applied");
    }
    
    @FXML 
    private void handleSystemTheme(ActionEvent event) {
        // Detect system theme (simplified - always use light for now)
        currentTheme = "light";
        applyTheme();
        updateStatus("System theme applied (light)");
    }
    
    private void applyTheme() {
        // Theme switching would require CSS files
        // For now, just log the change
        // TODO: Implement CSS theme switching
        logger.info("Theme changed to: " + currentTheme);
    }
    @FXML 
    private void handleSearch(ActionEvent event) {
        if (searchField != null) {
            searchField.requestFocus();
            searchField.selectAll();
            updateStatus("Search field focused");
        }
    }
    
    /**
     * Perform search across all notes.
     */
    private void performGlobalSearch(String searchText) {
        try {
            List<Note> allNotes = noteDAO.fetchAllNotes();
            List<Note> matchingNotes = new ArrayList<>();
            
            String lowerSearch = searchText.toLowerCase();
            for (Note note : allNotes) {
                boolean matches = false;
                if (note.getTitle() != null && note.getTitle().toLowerCase().contains(lowerSearch)) {
                    matches = true;
                }
                if (note.getContent() != null && note.getContent().toLowerCase().contains(lowerSearch)) {
                    matches = true;
                }
                if (matches) {
                    matchingNotes.add(note);
                }
            }
            
            notesListView.getItems().setAll(matchingNotes);
            noteCountLabel.setText(matchingNotes.size() + " notes found");
            currentFolder = null;
            updateStatus("Found " + matchingNotes.size() + " notes matching: " + searchText);
        } catch (Exception e) {
            logger.severe("Failed to search notes: " + e.getMessage());
            updateStatus("Error searching notes");
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
    @FXML private void handleAbout(ActionEvent event) { updateStatus("About not implemented yet"); }
    @FXML private void handleRefresh(ActionEvent event) { loadAllNotes(); }
    @FXML 
    private void handleToggleFavorite(ActionEvent event) {
        if (currentNote == null) {
            updateStatus("No note selected");
            return;
        }
        // Note: Favorites feature requires a database field that doesn't exist yet
        // For now, we'll use a simple in-memory tracking
        // TODO: Add is_favorite field to notes table
        updateStatus("Favorite feature requires database schema update");
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
     * Insert Markdown formatting at cursor position.
     */
    private void insertMarkdownFormat(String prefix, String suffix) {
        if (noteContentArea == null) return;
        
        String selectedText = noteContentArea.getSelectedText();
        int start = noteContentArea.getSelection().getStart();
        int end = noteContentArea.getSelection().getEnd();
        
        if (selectedText != null && !selectedText.isEmpty()) {
            // Replace selected text with formatted version
            String formatted = prefix + selectedText + suffix;
            noteContentArea.replaceSelection(formatted);
        } else {
            // Insert at cursor position
            int caretPos = noteContentArea.getCaretPosition();
            String text = noteContentArea.getText();
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