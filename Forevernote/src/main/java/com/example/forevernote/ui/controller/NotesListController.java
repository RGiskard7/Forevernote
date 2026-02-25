package com.example.forevernote.ui.controller;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.SystemActionEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class NotesListController {

    private EventBus eventBus;

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

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
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
