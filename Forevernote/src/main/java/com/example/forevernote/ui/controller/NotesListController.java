package com.example.forevernote.ui.controller;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

public class NotesListController {
    private static final Logger logger = Logger.getLogger(NotesListController.class.getName());

    private EventBus eventBus;
    private NoteService noteService;
    private TagService tagService;
    private FolderService folderService;
    private ResourceBundle bundle;

    private String currentFilterType = "all";
    private Folder currentFolder;
    private Tag currentTag;

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
    }

    private ListCell<Note> createNoteListCell() {
        return new ListCell<>() {
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
    }

    private ContextMenu createNoteContextMenu(Note note) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem(getString("action.open"));
        openItem.setOnAction(e -> {
            if (eventBus != null) {
                eventBus.publish(new NoteEvents.NoteSelectedEvent(note));
            }
        });
        MenuItem favoriteItem = new MenuItem(
                note.isFavorite() ? getString("action.remove_favorite") : getString("action.add_favorite"));
        favoriteItem.setOnAction(e -> toggleFavorite(note));

        MenuItem deleteItem = new MenuItem(getString("action.move_to_trash"));
        deleteItem.setOnAction(e -> deleteNote(note));

        contextMenu.getItems().addAll(openItem, favoriteItem, new SeparatorMenuItem(), deleteItem);
        return contextMenu;
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
            logger.severe("Failed to toggle favorite: " + e.getMessage());
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
        try {
            List<Note> notes = noteService.getAllNotes();
            notesListView.getSelectionModel().clearSelection();
            notesListView.getItems().setAll(notes);
            sortNotes(sortComboBox.getValue());

            currentFolder = null;
            currentTag = null;
            currentFilterType = "all";

            String msg = bundle != null
                    ? java.text.MessageFormat.format(bundle.getString("info.notes_count"), notes.size())
                    : notes.size() + " notes";
            if (notesPanelTitleLabel != null)
                notesPanelTitleLabel.setText(msg);
            publishNotesLoadedEvent(notes, getString("status.loaded_all"));
        } catch (Exception e) {
            logger.severe("Failed to load all notes: " + e.getMessage());
        }
    }

    public void loadNotesForFolder(Folder folder) {
        if (folder == null)
            return;
        try {
            List<Note> notes = noteService.getNotesByFolder(folder);
            notesListView.getSelectionModel().clearSelection();
            notesListView.getItems().setAll(notes);
            sortNotes(sortComboBox.getValue());

            currentFolder = folder;
            currentTag = null;
            currentFilterType = "folder";

            String msg = notes.size() + " notes";
            if (notesPanelTitleLabel != null)
                notesPanelTitleLabel.setText(
                        (bundle != null ? bundle.getString("panel.notes.title") : "Notes") + " - " + folder.getTitle());
            publishNotesLoadedEvent(notes,
                    bundle != null
                            ? java.text.MessageFormat.format(bundle.getString("status.loaded_folder"),
                                    folder.getTitle())
                            : "Loaded folder");
        } catch (Exception e) {
            logger.severe("Failed to load notes for folder: " + e.getMessage());
        }
    }

    public void loadNotesForTag(String tagName) {
        try {
            if (tagName != null && !tagName.isEmpty()) {
                Optional<Tag> tagOpt = tagService.getTagByTitle(tagName);
                if (tagOpt.isPresent()) {
                    Tag tag = tagOpt.get();
                    currentFolder = null;
                    currentFilterType = "tag";
                    currentTag = tag;
                    List<Note> notesWithTag = tagService.getNotesWithTag(tag);
                    notesListView.getSelectionModel().clearSelection();
                    notesListView.getItems().setAll(notesWithTag);
                    sortNotes(sortComboBox.getValue());

                    String msg = notesWithTag.size() + " notes with tag: " + tagName;
                    if (notesPanelTitleLabel != null)
                        notesPanelTitleLabel.setText(msg);
                    publishNotesLoadedEvent(notesWithTag,
                            bundle != null
                                    ? java.text.MessageFormat.format(bundle.getString("status.filtered_tag"), tagName)
                                    : msg);
                }
            }
        } catch (Exception e) {
            logger.severe("Failed to filter notes by tag " + tagName + ": " + e.getMessage());
        }
    }

    public void performSearch(String searchText) {
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

        try {
            List<Note> allNotes = noteService.getAllNotes();
            String searchLower = searchText.toLowerCase();
            List<Note> filteredNotes = allNotes.stream()
                    .filter(note -> {
                        String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
                        String content = note.getContent() != null ? note.getContent().toLowerCase() : "";
                        return title.contains(searchLower) || content.contains(searchLower);
                    })
                    .toList();

            notesListView.getSelectionModel().clearSelection();
            notesListView.getItems().setAll(filteredNotes);
            sortNotes(sortComboBox.getValue());
            currentFilterType = "search";

            String msg = bundle != null
                    ? java.text.MessageFormat.format(bundle.getString("info.notes_found"), filteredNotes.size())
                    : filteredNotes.size() + " notes found";
            if (notesPanelTitleLabel != null)
                notesPanelTitleLabel.setText(msg);
            publishNotesLoadedEvent(filteredNotes,
                    bundle != null
                            ? java.text.MessageFormat.format(bundle.getString("status.search_active"), searchText)
                            : "Search active");
        } catch (Exception e) {
            logger.severe("Failed to perform search: " + e.getMessage());
        }
    }

    public void sortNotes(String sortOption) {
        if (sortOption == null)
            return;
        List<Note> notes = new ArrayList<>(notesListView.getItems());

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

        notesListView.getSelectionModel().clearSelection();
        notesListView.getItems().setAll(notes);
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
        try {
            Note newNote = new Note(getString("action.new_note"), "");

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
                folderService.addNoteToFolder(currentFolder, newNote);
            }

            notesListView.getItems().add(0, newNote);
            notesListView.getSelectionModel().select(newNote);

            // Fire event for plugins and other controllers
            if (eventBus != null) {
                eventBus.publish(new NoteEvents.NoteCreatedEvent(newNote));
            }

            publishStatusUpdate(getString("status.note_created"));
        } catch (Exception e) {
            logger.severe("Failed to create new note: " + e.getMessage());
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
                logger.severe("Failed to delete note: " + e.getMessage());
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
