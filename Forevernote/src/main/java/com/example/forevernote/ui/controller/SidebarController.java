package com.example.forevernote.ui.controller;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.SystemActionEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SidebarController {

    private EventBus eventBus;

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
