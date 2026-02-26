package com.example.forevernote.ui.controller;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.SystemActionEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.example.forevernote.event.events.FolderEvents;
import com.example.forevernote.event.events.TagEvents;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.Note;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;

public class SidebarController {

    private EventBus eventBus;
    private NoteService noteService;
    private TagService tagService;

    // List Update Flags (to prevent infinite selection loops)
    private boolean isUpdatingRecentList = false;
    private boolean isUpdatingFavoritesList = false;
    private boolean isUpdatingTagsList = false;

    // Master lists for filtering
    private javafx.collections.ObservableList<String> masterTagsList = javafx.collections.FXCollections
            .observableArrayList();
    private javafx.collections.ObservableList<String> masterRecentList = javafx.collections.FXCollections
            .observableArrayList();
    private javafx.collections.ObservableList<String> masterFavoritesList = javafx.collections.FXCollections
            .observableArrayList();

    private boolean folderSortAscending = true;
    private boolean tagSortAscending = true;
    private boolean recentSortAscending = true;
    private boolean favoritesSortAscending = true;
    private boolean trashSortAscending = true;

    @FXML
    private VBox sidebarPane;
    @FXML
    private TabPane navigationTabPane;

    // Folders Tab
    @FXML
    private TextField filterFoldersField;
    @FXML
    private TreeView<Folder> folderTreeView;

    // Tags Tab
    @FXML
    private TextField filterTagsField;
    @FXML
    private ListView<String> tagListView;

    // Recent Tab
    @FXML
    private TextField filterRecentField;
    @FXML
    private ListView<String> recentNotesListView;

    // Favorites Tab
    @FXML
    private TextField filterFavoritesField;
    @FXML
    private ListView<String> favoritesListView;

    // Trash Tab
    @FXML
    private TextField filterTrashField;
    @FXML
    private TreeView<Component> trashTreeView;

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void setNoteService(NoteService noteService) {
        this.noteService = noteService;
    }

    public void setTagService(TagService tagService) {
        this.tagService = tagService;
    }

    @FXML
    public void initialize() {
        // Folder Tree Selection
        folderTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && eventBus != null) {
                eventBus.publish(new FolderEvents.FolderSelectedEvent(newVal.getValue()));
            }
        });

        // Tag List Selection
        tagListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingTagsList && newVal != null && eventBus != null) {
                // Determine whether this is a full Tag object or just title (currently String
                // in FXML)
                // Assuming TagService loads titles or we just send a mock tag with the title
                // for now
                Tag tag = new Tag(newVal);
                eventBus.publish(new TagEvents.TagSelectedEvent(tag));
            }
        });

        // Recent Notes List Selection
        recentNotesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingRecentList && newVal != null && eventBus != null) {
                // Determine ID or Title. Assuming FXML displays title.
                // In Phase 2, we will convert these to ListView<Note>. For now, emit a generic
                // Open request.
                Note note = new Note(newVal, "");
                eventBus.publish(new NoteEvents.NoteOpenRequestEvent(note));
            }
        });

        // Favorites List Selection
        favoritesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFavoritesList && newVal != null && eventBus != null) {
                Note note = new Note(newVal, "");
                eventBus.publish(new NoteEvents.NoteOpenRequestEvent(note));
            }
        });

        // Trash Tree Selection
        trashTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && eventBus != null) {
                eventBus.publish(new NoteEvents.TrashItemSelectedEvent(newVal.getValue()));
            }
        });
    }

    public VBox getSidebarPane() {
        return sidebarPane;
    }

    public TabPane getNavigationTabPane() {
        return navigationTabPane;
    }

    public TextField getFilterFoldersField() {
        return filterFoldersField;
    }

    public TreeView<Folder> getFolderTreeView() {
        return folderTreeView;
    }

    public TextField getFilterTagsField() {
        return filterTagsField;
    }

    public void loadTags() {
        if (tagService == null)
            return;
        try {
            isUpdatingTagsList = true;
            List<Tag> tags = tagService.getAllTags();

            tagListView.setCellFactory(lv -> {
                ListCell<String> cell = new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText("# " + item);
                        }
                    }
                };
                return cell;
            });

            masterTagsList.clear();
            for (Tag tag : tags) {
                masterTagsList.add(tag.getTitle());
            }

            Collections.sort(masterTagsList,
                    (s1, s2) -> tagSortAscending ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isUpdatingTagsList = false;
        }
    }

    public void loadRecentNotes() {
        if (noteService == null)
            return;
        try {
            isUpdatingRecentList = true;
            List<Note> allNotes = noteService.getAllNotes();

            // Sort by modified date
            allNotes.sort((a, b) -> {
                String dateA = a.getModifiedDate() != null ? a.getModifiedDate()
                        : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String dateB = b.getModifiedDate() != null ? b.getModifiedDate()
                        : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                return dateB.compareTo(dateA);
            });

            List<String> recentTitles = allNotes.stream()
                    .limit(10)
                    .map(Note::getTitle)
                    .toList();

            masterRecentList.clear();
            masterRecentList.addAll(recentTitles);

            // Sort list
            sortStringList(masterRecentList, recentSortAscending);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isUpdatingRecentList = false;
        }
    }

    public void loadFavorites() {
        if (noteService == null)
            return;
        try {
            isUpdatingFavoritesList = true;
            List<Note> allNotes = noteService.getAllNotes();

            List<String> favTitles = allNotes.stream()
                    .filter(Note::isFavorite)
                    .map(Note::getTitle)
                    .toList();

            masterFavoritesList.clear();
            masterFavoritesList.addAll(favTitles);

            // Sort
            sortStringList(masterFavoritesList, favoritesSortAscending);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isUpdatingFavoritesList = false;
        }
    }

    private void sortStringList(javafx.collections.ObservableList<String> list, boolean ascending) {
        if (list == null)
            return;
        Collections.sort(list, (s1, s2) -> ascending ? s1.compareToIgnoreCase(s2) : s2.compareToIgnoreCase(s1));
    }

    public ListView<String> getTagListView() {
        return tagListView;
    }

    public TextField getFilterRecentField() {
        return filterRecentField;
    }

    public ListView<String> getRecentNotesListView() {
        return recentNotesListView;
    }

    public TextField getFilterFavoritesField() {
        return filterFavoritesField;
    }

    public ListView<String> getFavoritesListView() {
        return favoritesListView;
    }

    public TextField getFilterTrashField() {
        return filterTrashField;
    }

    public TreeView<Component> getTrashTreeView() {
        return trashTreeView;
    }

    @FXML
    private void handleSortFolders(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.SORT_FOLDERS);
    }

    @FXML
    private void handleExpandAllFolders(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.EXPAND_ALL_FOLDERS);
    }

    @FXML
    private void handleCollapseAllFolders(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.COLLAPSE_ALL_FOLDERS);
    }

    @FXML
    private void handleSortTags(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.SORT_TAGS);
    }

    @FXML
    private void handleSortRecent(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.SORT_RECENT);
    }

    @FXML
    private void handleSortFavorites(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.SORT_FAVORITES);
    }

    @FXML
    private void handleSortTrash(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.SORT_TRASH);
    }

    @FXML
    private void handleEmptyTrash(ActionEvent event) {
        publishEvent(SystemActionEvent.ActionType.EMPTY_TRASH);
    }

    private void publishEvent(SystemActionEvent.ActionType actionType) {
        if (eventBus != null) {
            eventBus.publish(new SystemActionEvent(actionType));
        }
    }
}
