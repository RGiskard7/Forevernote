package com.example.forevernote.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.SQLiteDB;
import com.example.forevernote.data.dao.abstractLayers.FactoryDAO;
import com.example.forevernote.data.dao.interfaces.*;
import com.example.forevernote.data.models.*;
import com.example.forevernote.data.models.interfaces.Component;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
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
    @FXML private TreeView<String> folderTreeView;
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
    @FXML private TextArea previewTextArea;
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
        TreeItem<String> rootItem = new TreeItem<>("All Notes");
        rootItem.setExpanded(true);
        folderTreeView.setRoot(rootItem);
        
        // Handle folder selection
        folderTreeView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.getValue().equals("All Notes")) {
                    handleFolderSelection(newValue.getValue());
                } else {
                    loadAllNotes();
                }
            }
        );
        
        // Enable context menu
        folderTreeView.setCellFactory(tv -> {
            TreeCell<String> cell = new TreeCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                }
            };
            
            cell.setOnContextMenuRequested(event -> {
                if (!cell.isEmpty()) {
                    showFolderContextMenu(cell.getItem(), cell);
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
            TreeItem<String> root = folderTreeView.getRoot();
            root.getChildren().clear();
            
            List<Folder> folders = folderDAO.fetchAllFoldersAsList();
            for (Folder folder : folders) {
                TreeItem<String> folderItem = new TreeItem<>(folder.getTitle());
                root.getChildren().add(folderItem);
                loadSubFolders(folderItem, folder);
            }
            
            logger.info("Loaded " + folders.size() + " folders");
        } catch (Exception e) {
            logger.severe("Failed to load folders: " + e.getMessage());
            updateStatus("Error loading folders");
        }
    }
    
    /**
     * Load subfolders recursively.
     */
    private void loadSubFolders(TreeItem<String> parentItem, Folder parentFolder) {
        try {
            folderDAO.loadSubFolders(parentFolder);
            for (Component child : parentFolder.getChildren()) {
                if (child instanceof Folder) {
                    TreeItem<String> childItem = new TreeItem<>(child.getTitle());
                    parentItem.getChildren().add(childItem);
                    loadSubFolders(childItem, (Folder) child);
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
    private void handleFolderSelection(String folderName) {
        try {
            // Find folder by name (simplified - in real app would use folder ID)
            List<Folder> folders = folderDAO.fetchAllFoldersAsList();
            Optional<Folder> selectedFolder = folders.stream()
                .filter(f -> f.getTitle().equals(folderName))
                .findFirst();
            
            if (selectedFolder.isPresent()) {
                currentFolder = selectedFolder.get();
                folderDAO.loadNotes(currentFolder);
                
                List<Note> notes = currentFolder.getChildren().stream()
                    .filter(c -> c instanceof Note)
                    .map(c -> (Note) c)
                    .toList();
                
                notesListView.getItems().setAll(notes);
                noteCountLabel.setText(notes.size() + " notes");
                updateStatus("Loaded folder: " + folderName);
            }
        } catch (Exception e) {
            logger.severe("Failed to load folder " + folderName + ": " + e.getMessage());
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
            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                return;
            }
        }
        
        currentNote = note;
        noteTitleField.setText(note.getTitle());
        noteContentArea.setText(note.getContent());
        
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
                tagLabel.setOnMouseClicked(event -> removeTagFromNote(tag));
                tagsFlowPane.getChildren().add(tagLabel);
            }
        } catch (Exception e) {
            logger.warning("Failed to load tags for note " + note.getId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Update note metadata display.
     */
    private void updateNoteMetadata(Note note) {
        createdDateLabel.setText("Created: " + note.getCreatedDate());
        modifiedDateLabel.setText("Modified: " + note.getModifiedDate());
        infoCreatedLabel.setText(note.getCreatedDate());
        infoModifiedLabel.setText(note.getModifiedDate());
        infoWordsLabel.setText(String.valueOf(countWords(note.getContent())));
        infoCharsLabel.setText(String.valueOf(note.getContent().length()));
        infoLatitudeLabel.setText(note.getLatitude().toString());
        infoLongitudeLabel.setText(note.getLongitude().toString());
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
            List<String> tagNames = tags.stream()
                .map(Tag::getTitle)
                .toList();
            
            tagListView.getItems().setAll(tagNames);
        } catch (Exception e) {
            logger.warning("Failed to load tags: " + e.getMessage());
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
        if (currentNote != null) {
            previewTextArea.setText(noteContentArea.getText());
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
     * Show folder context menu.
     */
    private void showFolderContextMenu(String folderName, TreeCell<String> cell) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem newNoteItem = new MenuItem("New Note");
        newNoteItem.setOnAction(e -> handleNewNote(new ActionEvent()));
        
        MenuItem newFolderItem = new MenuItem("New Subfolder");
        newFolderItem.setOnAction(e -> handleNewFolder(new ActionEvent()));
        
        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> handleRenameFolder(folderName));
        
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> handleDeleteFolder(folderName));
        
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
        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new folder");
        dialog.setContentText("Folder name:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                Folder newFolder = new Folder(result.get().trim());
                int folderId = folderDAO.createFolder(newFolder);
                newFolder.setId(folderId);
                
                loadFolders();
                updateStatus("Created folder: " + newFolder.getTitle());
            } catch (Exception e) {
                logger.severe("Failed to create folder: " + e.getMessage());
                updateStatus("Error creating folder");
            }
        }
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        if (currentNote != null && isModified) {
            try {
                noteDAO.updateNote(currentNote);
                isModified = false;
                updateStatus("Saved: " + currentNote.getTitle());
            } catch (Exception e) {
                logger.severe("Failed to save note: " + e.getMessage());
                updateStatus("Error saving note");
            }
        }
    }
    
    @FXML
    private void handleSaveAll(ActionEvent event) {
        // Implementation for save all
        updateStatus("Save all not implemented yet");
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
                    noteDAO.deleteNote(currentNote.getId());
                    notesListView.getItems().remove(currentNote);
                    currentNote = null;
                    noteTitleField.clear();
                    noteContentArea.clear();
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
            if (result.isPresent() && result.get() == ButtonType.CANCEL) {
                return;
            }
        }
        
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            logger.warning("Error closing database connection: " + e.getMessage());
        }
        
        System.exit(0);
    }
    
    // Placeholder implementations for other menu items
    @FXML private void handleImport(ActionEvent event) { updateStatus("Import not implemented yet"); }
    @FXML private void handleExport(ActionEvent event) { updateStatus("Export not implemented yet"); }
    @FXML private void handleUndo(ActionEvent event) { updateStatus("Undo not implemented yet"); }
    @FXML private void handleRedo(ActionEvent event) { updateStatus("Redo not implemented yet"); }
    @FXML private void handleCut(ActionEvent event) { updateStatus("Cut not implemented yet"); }
    @FXML private void handleCopy(ActionEvent event) { updateStatus("Copy not implemented yet"); }
    @FXML private void handlePaste(ActionEvent event) { updateStatus("Paste not implemented yet"); }
    @FXML private void handleFind(ActionEvent event) { updateStatus("Find not implemented yet"); }
    @FXML private void handleReplace(ActionEvent event) { updateStatus("Replace not implemented yet"); }
    @FXML private void handleToggleSidebar(ActionEvent event) { updateStatus("Toggle sidebar not implemented yet"); }
    @FXML private void handleZoomIn(ActionEvent event) { updateStatus("Zoom in not implemented yet"); }
    @FXML private void handleZoomOut(ActionEvent event) { updateStatus("Zoom out not implemented yet"); }
    @FXML private void handleResetZoom(ActionEvent event) { updateStatus("Reset zoom not implemented yet"); }
    @FXML private void handleLightTheme(ActionEvent event) { updateStatus("Light theme not implemented yet"); }
    @FXML private void handleDarkTheme(ActionEvent event) { updateStatus("Dark theme not implemented yet"); }
    @FXML private void handleSystemTheme(ActionEvent event) { updateStatus("System theme not implemented yet"); }
    @FXML private void handleSearch(ActionEvent event) { updateStatus("Search not implemented yet"); }
    @FXML private void handleTagsManager(ActionEvent event) { updateStatus("Tags manager not implemented yet"); }
    @FXML private void handlePreferences(ActionEvent event) { updateStatus("Preferences not implemented yet"); }
    @FXML private void handleDocumentation(ActionEvent event) { updateStatus("Documentation not implemented yet"); }
    @FXML private void handleKeyboardShortcuts(ActionEvent event) { updateStatus("Keyboard shortcuts not implemented yet"); }
    @FXML private void handleAbout(ActionEvent event) { updateStatus("About not implemented yet"); }
    @FXML private void handleRefresh(ActionEvent event) { loadAllNotes(); }
    @FXML private void handleToggleFavorite(ActionEvent event) { updateStatus("Toggle favorite not implemented yet"); }
    @FXML private void handleNewTag(ActionEvent event) { updateStatus("New tag not implemented yet"); }
    @FXML private void handleBold(ActionEvent event) { updateStatus("Bold formatting not implemented yet"); }
    @FXML private void handleItalic(ActionEvent event) { updateStatus("Italic formatting not implemented yet"); }
    @FXML private void handleUnderline(ActionEvent event) { updateStatus("Underline formatting not implemented yet"); }
    @FXML private void handleLink(ActionEvent event) { updateStatus("Link not implemented yet"); }
    @FXML private void handleImage(ActionEvent event) { updateStatus("Image not implemented yet"); }
    @FXML private void handleAttachment(ActionEvent event) { updateStatus("Attachment not implemented yet"); }
    @FXML private void handleTodoList(ActionEvent event) { updateStatus("Todo list not implemented yet"); }
    @FXML private void handleNumberedList(ActionEvent event) { updateStatus("Numbered list not implemented yet"); }
    
    private void handleRenameFolder(String folderName) {
        TextInputDialog dialog = new TextInputDialog(folderName);
        dialog.setTitle("Rename Folder");
        dialog.setHeaderText("Rename folder");
        dialog.setContentText("New name:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty() && !result.get().equals(folderName)) {
            updateStatus("Rename folder not implemented yet");
        }
    }
    
    private void handleDeleteFolder(String folderName) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Folder");
        alert.setHeaderText("Are you sure you want to delete this folder?");
        alert.setContentText("All notes in this folder will be moved to the root.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            updateStatus("Delete folder not implemented yet");
        }
    }
}