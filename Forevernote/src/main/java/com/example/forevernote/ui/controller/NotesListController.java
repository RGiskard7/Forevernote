package com.example.forevernote.ui.controller;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.event.events.SystemActionEvent;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class NotesListController {
    private static final Logger logger = Logger.getLogger(NotesListController.class.getName());

    private EventBus eventBus;
    private NoteService noteService;
    private TagService tagService;
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
    }

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setServices(NoteService noteService, TagService tagService) {
        this.noteService = noteService;
        this.tagService = tagService;
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

    private String getString(String key) {
        return bundle != null ? bundle.getString(key) : key;
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

    private void publishEvent(SystemActionEvent.ActionType actionType) {
        if (eventBus != null) {
            eventBus.publish(new SystemActionEvent(actionType));
        }
    }
}
