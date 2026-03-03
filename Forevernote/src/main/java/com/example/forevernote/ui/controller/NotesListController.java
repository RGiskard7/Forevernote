package com.example.forevernote.ui.controller;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.event.events.SystemActionEvent;
import com.example.forevernote.event.events.UIEvents;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;
import java.util.prefs.Preferences;
import java.io.File;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.application.Platform;

public class NotesListController {
    private static final Logger logger = LoggerConfig.getLogger(NotesListController.class);

    private EventBus eventBus;
    private NoteService noteService;
    private TagService tagService;
    private FolderService folderService;
    private ResourceBundle bundle;

    private String currentFilterType = "all";
    private Folder currentFolder;
    private Tag currentTag;
    private final ExecutorService notesLoadExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "forevernote-notes-loader");
        t.setDaemon(true);
        return t;
    });
    private final AtomicLong notesLoadVersion = new AtomicLong(0);
    private volatile List<Note> allNotesSearchCache = List.of();
    private volatile boolean allNotesSearchCacheDirty = true;

    @FXML
    private VBox notesPanel;
    @FXML
    private HBox stackedModeHeader;
    @FXML
    private Label notesPanelTitleLabel;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Button refreshBtn;
    @FXML
    private ListView<Note> notesListView;

    @FXML
    public void initialize() {
        // Publish event when a note is selected
        notesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && eventBus != null) {
                eventBus.publish(new NoteEvents.NoteSelectedEvent(newVal));
            }
        });

        // Trigger sort on combo box change
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> sortNotes(newVal));

        // Use custom cell factory
        notesListView.setCellFactory(lv -> createNoteListCell());
        // Slimmer row density for a cleaner, more compact notes list.
        notesListView.setFixedCellSize(62);
    }

    private ListCell<Note> createNoteListCell() {
        return new ListCell<>() {
            private final VBox container = new VBox(2);
            private final HBox titleRow = new HBox(5);
            private final FontIcon pinIcon = new FontIcon("fth-map-pin");
            private final FontIcon favIcon = new FontIcon("fth-star");
            private final Label titleLabel = new Label();
            private final Label previewLabel = new Label();
            private final Label dateLabel = new Label();
            private final ContextMenu contextMenu = new ContextMenu();
            private final MenuItem openItem = new MenuItem(getString("action.open"));
            private final MenuItem favoriteItem = new MenuItem();
            private final MenuItem deleteItem = new MenuItem(getString("action.move_to_trash"));

            {
                container.getStyleClass().add("note-cell-container");
                container.setPadding(new javafx.geometry.Insets(2, 8, 2, 8));

                titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                pinIcon.getStyleClass().add("feather-pin-active");
                pinIcon.setIconSize(12);
                favIcon.setIconColor(javafx.scene.paint.Color.GOLD);
                favIcon.setIconSize(12);

                titleLabel.getStyleClass().add("note-cell-title");
                previewLabel.getStyleClass().add("note-cell-preview");
                dateLabel.getStyleClass().add("note-cell-date");

                container.getChildren().addAll(titleRow, previewLabel, dateLabel);

                openItem.setOnAction(e -> {
                    Note note = getItem();
                    if (note != null && eventBus != null) {
                        eventBus.publish(new NoteEvents.NoteSelectedEvent(note));
                    }
                });
                favoriteItem.setOnAction(e -> {
                    Note note = getItem();
                    if (note != null) {
                        toggleFavorite(note);
                    }
                });
                deleteItem.setOnAction(e -> {
                    Note note = getItem();
                    if (note != null) {
                        deleteNote(note);
                    }
                });
                contextMenu.getItems().addAll(openItem, favoriteItem, new SeparatorMenuItem(), deleteItem);

                setOnDragDetected(event -> {
                    Note note = getItem();
                    if (note == null || note.getId() == null) {
                        return;
                    }
                    Dragboard db = startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString("note:" + note.getId());
                    db.setContent(content);
                    event.consume();
                });
            }

            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    titleRow.getChildren().clear();
                    if (note.isPinned()) {
                        titleRow.getChildren().add(pinIcon);
                    }
                    if (note.isFavorite()) {
                        titleRow.getChildren().add(favIcon);
                    }
                    titleLabel.setText(note.getTitle() != null ? note.getTitle() : "");
                    titleRow.getChildren().add(titleLabel);

                    previewLabel.setText(buildPreviewText(note.getContent()));
                    dateLabel.setText(formatDateText(note));
                    favoriteItem.setText(note.isFavorite() ? getString("action.remove_favorite")
                            : getString("action.add_favorite"));

                    setGraphic(container);
                    setText(null);
                    setContextMenu(contextMenu);
                }
            }
        };
    }

    private String buildPreviewText(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String text = content;
        int idx = 0;
        while (idx < text.length() && text.charAt(idx) == '#') {
            idx++;
        }
        if (idx > 0 && idx < text.length() && Character.isWhitespace(text.charAt(idx))) {
            text = text.substring(idx).trim();
        }
        text = text.replace('\n', ' ').trim();
        return text.length() > 60 ? text.substring(0, 57) + "..." : text;
    }

    private String formatDateText(Note note) {
        if (note == null) {
            return "";
        }
        String dateText = note.getModifiedDate() != null ? note.getModifiedDate() : note.getCreatedDate();
        if (dateText != null && dateText.length() > 10) {
            return dateText.substring(0, 10);
        }
        return dateText != null ? dateText : "";
    }

    private void toggleFavorite(Note note) {
        try {
            note.setFavorite(!note.isFavorite());
            noteService.updateNote(note);
            notesListView.refresh();
            if (eventBus != null) {
                eventBus.publish(new NoteEvents.NoteModifiedEvent(note));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to toggle favorite", e);
        }
    }

    private String getString(String key) {
        return bundle != null && bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    private boolean isAllNotesVirtualFolder(Folder folder) {
        return folder != null && "ALL_NOTES_VIRTUAL".equals(folder.getId());
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        if (eventBus == null)
            return;

        eventBus.subscribe(SystemActionEvent.class, event -> {
            javafx.application.Platform.runLater(() -> {
                if (event.getActionType() == SystemActionEvent.ActionType.NEW_NOTE) {
                    handleNewNote(null);
                } else if (event.getActionType() == SystemActionEvent.ActionType.DELETE) {
                    handleDelete(null);
                }
            });
        });
        eventBus.subscribe(NoteEvents.NoteCreatedEvent.class, event -> markAllNotesSearchCacheDirty());
        eventBus.subscribe(NoteEvents.NoteSavedEvent.class, event -> markAllNotesSearchCacheDirty());
        eventBus.subscribe(NoteEvents.NoteDeletedEvent.class, event -> markAllNotesSearchCacheDirty());
        eventBus.subscribe(NoteEvents.TrashItemDeletedEvent.class, event -> markAllNotesSearchCacheDirty());
    }

    public void setServices(NoteService noteService, TagService tagService, FolderService folderService) {
        this.noteService = noteService;
        this.tagService = tagService;
        this.folderService = folderService;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public Folder getCurrentFolder() {
        return currentFolder;
    }

    public String getCurrentFilterType() {
        return currentFilterType;
    }

    public Tag getCurrentTag() {
        return currentTag;
    }

    public VBox getNotesPanel() {
        return notesPanel;
    }

    public HBox getStackedModeHeader() {
        return stackedModeHeader;
    }

    public Label getNotesPanelTitleLabel() {
        return notesPanelTitleLabel;
    }

    public ComboBox<String> getSortComboBox() {
        return sortComboBox;
    }

    public Button getRefreshBtn() {
        return refreshBtn;
    }

    public ListView<Note> getNotesListView() {
        return notesListView;
    }

    // Phase 3: Migrated Loading Logic

    public void loadAllNotes() {
        if (noteService == null) {
            logger.warning("Cannot load notes: noteService is null");
            return;
        }
        currentFolder = null;
        currentTag = null;
        currentFilterType = "all";
        String sortOption = sortComboBox != null ? sortComboBox.getValue() : null;
        executeNotesLoad(
                this::loadAllNotesFromServiceAndRefreshCache,
                notes -> {
                    List<Note> sorted = sortNotesData(notes, sortOption);
                    notesListView.getSelectionModel().clearSelection();
                    notesListView.getItems().setAll(sorted);
                    String msg = bundle != null
                            ? java.text.MessageFormat.format(bundle.getString("info.notes_count"), sorted.size())
                            : sorted.size() + " notes";
                    if (notesPanelTitleLabel != null) {
                        notesPanelTitleLabel.setText(msg);
                    }
                    publishNotesLoadedEvent(sorted, getString("status.loaded_all"));
                },
                "Failed to load all notes");
    }

    public void loadNotesForFolder(Folder folder) {
        if (folder == null)
            return;
        if (noteService == null) {
            logger.warning("Cannot load folder notes: noteService is null");
            return;
        }
        currentFolder = folder;
        currentTag = null;
        currentFilterType = "folder";
        String sortOption = sortComboBox != null ? sortComboBox.getValue() : null;
        executeNotesLoad(
                () -> noteService.getNotesByFolder(folder),
                notes -> {
                    List<Note> sorted = sortNotesData(notes, sortOption);
                    notesListView.getSelectionModel().clearSelection();
                    notesListView.getItems().setAll(sorted);
                    if (notesPanelTitleLabel != null) {
                        notesPanelTitleLabel.setText(getString("panel.notes.title") + " - " + folder.getTitle());
                    }
                    publishNotesLoadedEvent(sorted,
                            bundle != null
                                    ? java.text.MessageFormat.format(bundle.getString("status.loaded_folder"),
                                            folder.getTitle())
                                    : "Loaded folder");
                },
                "Failed to load notes for folder");
    }

    public void loadNotesForTag(String tagName) {
        if (tagService == null) {
            logger.warning("Cannot filter by tag: tagService is null");
            return;
        }
        if (tagName == null || tagName.isEmpty()) {
            return;
        }
        try {
            Optional<Tag> tagOpt = tagService.getTagByTitle(tagName);
            if (tagOpt.isPresent()) {
                Tag tag = tagOpt.get();
                currentFolder = null;
                currentFilterType = "tag";
                currentTag = tag;
                String sortOption = sortComboBox != null ? sortComboBox.getValue() : null;
                executeNotesLoad(
                        () -> tagService.getNotesWithTag(tag),
                        notesWithTag -> {
                            List<Note> sorted = sortNotesData(notesWithTag, sortOption);
                            notesListView.getSelectionModel().clearSelection();
                            notesListView.getItems().setAll(sorted);
                            String msg = java.text.MessageFormat.format(getString("info.notes_count"), sorted.size());
                            if (notesPanelTitleLabel != null) {
                                notesPanelTitleLabel.setText(msg);
                            }
                            publishNotesLoadedEvent(sorted,
                                    bundle != null
                                            ? java.text.MessageFormat.format(bundle.getString("status.filtered_tag"),
                                                    tagName)
                                            : msg);
                        },
                        "Failed to filter notes by tag " + tagName);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to filter notes by tag " + tagName, e);
        }
    }

    public void performSearch(String searchText) {
        if (noteService == null) {
            logger.warning("Cannot perform search: noteService is null");
            return;
        }
        if (searchText == null || searchText.trim().isEmpty()) {
            if (currentFolder != null) {
                loadNotesForFolder(currentFolder);
            } else if (currentTag != null) {
                loadNotesForTag(currentTag.getTitle());
            } else {
                loadAllNotes();
            }
            return;
        }
        currentFilterType = "search";
        String sortOption = sortComboBox != null ? sortComboBox.getValue() : null;
        executeNotesLoad(
                () -> {
                    List<Note> allNotes = getSearchSourceNotes();
                    String searchLower = searchText.toLowerCase(Locale.ROOT);
                    List<Note> filteredNotes = allNotes.stream()
                            .filter(note -> {
                                String title = note.getTitle() != null ? note.getTitle().toLowerCase(Locale.ROOT) : "";
                                String content = note.getContent() != null ? note.getContent().toLowerCase(Locale.ROOT)
                                        : "";
                                return title.contains(searchLower) || content.contains(searchLower);
                            })
                            .toList();
                    return sortNotesData(filteredNotes, sortOption);
                },
                filteredNotes -> {
                    notesListView.getSelectionModel().clearSelection();
                    notesListView.getItems().setAll(filteredNotes);
                    String msg = bundle != null
                            ? java.text.MessageFormat.format(bundle.getString("info.notes_found"), filteredNotes.size())
                            : filteredNotes.size() + " notes found";
                    if (notesPanelTitleLabel != null) {
                        notesPanelTitleLabel.setText(msg);
                    }
                    publishNotesLoadedEvent(filteredNotes,
                            bundle != null
                                    ? java.text.MessageFormat.format(bundle.getString("status.search_active"),
                                            searchText)
                                    : "Search active");
                },
                "Failed to perform search");
    }

    private void markAllNotesSearchCacheDirty() {
        allNotesSearchCacheDirty = true;
    }

    private List<Note> loadAllNotesFromServiceAndRefreshCache() {
        List<Note> allNotes = noteService.getAllNotes();
        allNotesSearchCache = List.copyOf(allNotes);
        allNotesSearchCacheDirty = false;
        return allNotes;
    }

    private List<Note> getSearchSourceNotes() {
        if (allNotesSearchCacheDirty || allNotesSearchCache.isEmpty()) {
            return loadAllNotesFromServiceAndRefreshCache();
        }
        return allNotesSearchCache;
    }

    public void sortNotes(String sortOption) {
        if (sortOption == null || notesListView == null)
            return;
        List<Note> notes = sortNotesData(new ArrayList<>(notesListView.getItems()), sortOption);
        notesListView.getSelectionModel().clearSelection();
        notesListView.getItems().setAll(notes);
    }

    private List<Note> sortNotesData(List<Note> notes, String sortOption) {
        if (notes == null || sortOption == null) {
            return notes != null ? notes : new ArrayList<>();
        }
        notes.sort((a, b) -> {
            if (a.isPinned() != b.isPinned()) {
                return a.isPinned() ? -1 : 1;
            }

            if (bundle != null && sortOption.equals(bundle.getString("sort.title_az"))) {
                String titleA = a.getTitle() != null ? a.getTitle() : "";
                String titleB = b.getTitle() != null ? b.getTitle() : "";
                return titleA.compareToIgnoreCase(titleB);
            } else if (bundle != null && sortOption.equals(bundle.getString("sort.title_za"))) {
                String titleZA = a.getTitle() != null ? a.getTitle() : "";
                String titleZB = b.getTitle() != null ? b.getTitle() : "";
                return titleZB.compareToIgnoreCase(titleZA);
            } else if (bundle != null && sortOption.equals(bundle.getString("sort.created_newest"))) {
                String cDateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
                String cDateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
                return cDateB.compareTo(cDateA);
            } else if (bundle != null && sortOption.equals(bundle.getString("sort.created_oldest"))) {
                String coDateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
                String coDateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
                return coDateA.compareTo(coDateB);
            } else if (bundle != null && sortOption.equals(bundle.getString("sort.modified_newest"))) {
                String mDateA = a.getModifiedDate() != null ? a.getModifiedDate()
                        : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String mDateB = b.getModifiedDate() != null ? b.getModifiedDate()
                        : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                return mDateB.compareTo(mDateA);
            } else if (bundle != null && sortOption.equals(bundle.getString("sort.modified_oldest"))) {
                String moDateA = a.getModifiedDate() != null ? a.getModifiedDate()
                        : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String moDateB = b.getModifiedDate() != null ? b.getModifiedDate()
                        : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                return moDateA.compareTo(moDateB);
            } else {
                return 0;
            }
        });
        return notes;
    }

    private void executeNotesLoad(Supplier<List<Note>> loader, Consumer<List<Note>> uiConsumer, String errorLog) {
        long requestVersion = notesLoadVersion.incrementAndGet();
        notesLoadExecutor.submit(() -> {
            try {
                List<Note> result = loader.get();
                if (requestVersion != notesLoadVersion.get()) {
                    return;
                }
                Platform.runLater(() -> {
                    if (requestVersion != notesLoadVersion.get()) {
                        return;
                    }
                    uiConsumer.accept(result != null ? result : List.of());
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, errorLog, e);
            }
        });
    }

    private void publishNotesLoadedEvent(List<Note> notes, String message) {
        if (eventBus != null) {
            eventBus.publish(new NoteEvents.NotesLoadedEvent(notes, message));
        }
    }

    @FXML
    private void handleToggleNotesPanel(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.TOGGLE_NOTES_LIST);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.REFRESH_NOTES);
    }

    @FXML
    private void handleNewNote(ActionEvent event) {
        if (noteService == null) {
            logger.warning("Cannot create note: noteService is null");
            publishStatusUpdate(getString("status.error_creating_note"));
            return;
        }
        try {
            Note newNote = new Note(getString("app.untitled"), "");

            // Set parent folder
            if (currentFolder != null && currentFolder.getId() != null &&
                    !"ROOT".equals(currentFolder.getId()) &&
                    !isAllNotesVirtualFolder(currentFolder)) {
                newNote.setParent(currentFolder);
            }

            // Fix: If a folder is selected, prepare the ID with the folder path for
            // FileSystem storage
            Preferences prefs = Preferences.userNodeForPackage(NotesListController.class);
            boolean isFileSystem = !"sqlite".equals(prefs.get("storage_type", "sqlite"));

            if (isFileSystem && currentFolder != null && currentFolder.getId() != null &&
                    !"ROOT".equals(currentFolder.getId()) &&
                    !isAllNotesVirtualFolder(currentFolder)) {

                String pathSeparator = File.separator;
                String folderPath = currentFolder.getId();
                String safeTitle = newNote.getTitle().replaceAll("[^a-zA-Z0-9\\.\\-_ ]", "_");

                newNote.setId(folderPath + pathSeparator + safeTitle);
            }

            Note createdNote = noteService.createNote(newNote);
            String noteId = createdNote.getId();
            if (noteId == null) {
                publishStatusUpdate(getString("status.error_creating_note"));
                return;
            }
            newNote.setId(noteId);

            if (currentFolder != null && currentFolder.getId() != null &&
                    !"ROOT".equals(currentFolder.getId()) &&
                    !isAllNotesVirtualFolder(currentFolder)) {
                if (folderService != null) {
                    folderService.addNoteToFolder(currentFolder, newNote);
                } else {
                    logger.warning("Skipped addNoteToFolder: folderService is null");
                }
            }

            notesListView.getItems().add(0, newNote);
            notesListView.getSelectionModel().select(newNote);

            // Fire event for plugins and other controllers
            if (eventBus != null) {
                eventBus.publish(new NoteEvents.NoteCreatedEvent(newNote));
            }

            publishStatusUpdate(getString("status.note_created"));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create new note", e);
            publishStatusUpdate(getString("status.error_creating_note"));
        }
    }

    private void publishStatusUpdate(String message) {
        if (eventBus != null) {
            eventBus.publish(new UIEvents.StatusUpdateEvent(message));
        }
    }

    @FXML
    private void handleDelete(ActionEvent event) {
        Note selectedNote = notesListView.getSelectionModel().getSelectedItem();
        if (selectedNote != null) {
            deleteNote(selectedNote);
        } else {
            // Note: SidebarController also listens to DELETE and handles its managed items.
            // If we are here and no note is selected, we could just do nothing or
            // publish a status if we want to be explicit.
            // However, MainController previously favored Sidebar over Note selection.
            // Here they both listen, so if Sidebar found something, it acts.
            // If NotesList finds something, it acts.
        }
    }

    private void deleteNote(Note note) {
        if (note == null)
            return;
        if (noteService == null) {
            logger.warning("Cannot delete note: noteService is null");
            publishStatusUpdate(getString("status.note_delete_error"));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(getString("dialog.delete_note.title"));
        alert.setHeaderText(getString("dialog.delete_note.header"));
        alert.setContentText(getString("dialog.delete_note.content"));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                noteService.moveToTrash(note.getId());

                // Selection clearing is important
                notesListView.getSelectionModel().clearSelection();

                // Publish event so Sidebar can refresh counts, Main can refresh etc.
                if (eventBus != null) {
                    eventBus.publish(new NoteEvents.NoteDeletedEvent(note.getId(), note.getTitle()));
                }

                publishStatusUpdate(getString("status.note_moved_trash"));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to delete note", e);
                publishStatusUpdate(getString("status.note_delete_error"));
            }
        }
    }

    private void publishEvent(SystemActionEvent.ActionType actionType) {
        if (eventBus != null) {
            eventBus.publish(new SystemActionEvent(actionType));
        }
    }
}
