package com.example.forevernote.ui.controller;

import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.SystemActionEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class EditorController {

    private EventBus eventBus;

    @FXML
    private VBox editorContainer;

    // Editor Header
    @FXML
    private TextField noteTitleField;
    @FXML
    private ToggleButton toggleTagsBtn;
    @FXML
    private ToggleButton editorOnlyButton;
    @FXML
    private ToggleButton splitViewButton;
    @FXML
    private ToggleButton previewOnlyButton;
    @FXML
    private ToggleButton pinButton;
    @FXML
    private ToggleButton favoriteButton;
    @FXML
    private ToggleButton infoButton;

    // Tags Bar
    @FXML
    private VBox tagsContainer;
    @FXML
    private FlowPane tagsFlowPane;
    @FXML
    private Label modifiedDateLabel;

    // Editor/Preview Area
    @FXML
    private SplitPane editorPreviewSplitPane;
    @FXML
    private VBox editorPane;
    @FXML
    private TextArea noteContentArea;

    // Toolbar Buttons
    @FXML
    private Button heading1Btn, heading2Btn, heading3Btn;
    @FXML
    private Button boldBtn, italicBtn, strikeBtn, underlineBtn;
    @FXML
    private Button highlightBtn, linkBtn, imageBtn;
    @FXML
    private Button todoBtn, bulletBtn, numberBtn;
    @FXML
    private Button quoteBtn, codeBtn;
    @FXML
    private Label wordCountLabel;

    // Preview
    @FXML
    private VBox previewPane;
    @FXML
    private WebView previewWebView;

    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    // --- GETTERS ---
    public VBox getEditorContainer() {
        return editorContainer;
    }

    public TextField getNoteTitleField() {
        return noteTitleField;
    }

    public ToggleButton getToggleTagsBtn() {
        return toggleTagsBtn;
    }

    public ToggleButton getEditorOnlyButton() {
        return editorOnlyButton;
    }

    public ToggleButton getSplitViewButton() {
        return splitViewButton;
    }

    public ToggleButton getPreviewOnlyButton() {
        return previewOnlyButton;
    }

    public ToggleButton getPinButton() {
        return pinButton;
    }

    public ToggleButton getFavoriteButton() {
        return favoriteButton;
    }

    public ToggleButton getInfoButton() {
        return infoButton;
    }

    public VBox getTagsContainer() {
        return tagsContainer;
    }

    public FlowPane getTagsFlowPane() {
        return tagsFlowPane;
    }

    public Label getModifiedDateLabel() {
        return modifiedDateLabel;
    }

    public SplitPane getEditorPreviewSplitPane() {
        return editorPreviewSplitPane;
    }

    public VBox getEditorPane() {
        return editorPane;
    }

    public TextArea getNoteContentArea() {
        return noteContentArea;
    }

    public Button getHeading1Btn() {
        return heading1Btn;
    }

    public Button getHeading2Btn() {
        return heading2Btn;
    }

    public Button getHeading3Btn() {
        return heading3Btn;
    }

    public Button getBoldBtn() {
        return boldBtn;
    }

    public Button getItalicBtn() {
        return italicBtn;
    }

    public Button getStrikeBtn() {
        return strikeBtn;
    }

    public Button getUnderlineBtn() {
        return underlineBtn;
    }

    public Button getHighlightBtn() {
        return highlightBtn;
    }

    public Button getLinkBtn() {
        return linkBtn;
    }

    public Button getImageBtn() {
        return imageBtn;
    }

    public Button getTodoBtn() {
        return todoBtn;
    }

    public Button getBulletBtn() {
        return bulletBtn;
    }

    public Button getNumberBtn() {
        return numberBtn;
    }

    public Button getQuoteBtn() {
        return quoteBtn;
    }

    public Button getCodeBtn() {
        return codeBtn;
    }

    public Label getWordCountLabel() {
        return wordCountLabel;
    }

    public VBox getPreviewPane() {
        return previewPane;
    }

    public WebView getPreviewWebView() {
        return previewWebView;
    }

    // --- ACTION EVENTS ---
    @FXML
    private void handleToggleTags(ActionEvent event) {
        publish(SystemActionEvent.ActionType.TOGGLE_TAGS);
    }

    @FXML
    private void handleEditorOnlyMode(ActionEvent event) {
        publish(SystemActionEvent.ActionType.EDITOR_ONLY_MODE);
    }

    @FXML
    private void handleSplitViewMode(ActionEvent event) {
        publish(SystemActionEvent.ActionType.SPLIT_VIEW_MODE);
    }

    @FXML
    private void handlePreviewOnlyMode(ActionEvent event) {
        publish(SystemActionEvent.ActionType.PREVIEW_ONLY_MODE);
    }

    @FXML
    private void handleTogglePin(ActionEvent event) {
        publish(SystemActionEvent.ActionType.TOGGLE_PIN);
    }

    @FXML
    private void handleToggleFavorite(ActionEvent event) {
        publish(SystemActionEvent.ActionType.TOGGLE_FAVORITE);
    }

    @FXML
    private void handleToggleRightPanel(ActionEvent event) {
        publish(SystemActionEvent.ActionType.TOGGLE_RIGHT_PANEL);
    }

    @FXML
    private void handleHeading1(ActionEvent event) {
        publish(SystemActionEvent.ActionType.HEADING1);
    }

    @FXML
    private void handleHeading2(ActionEvent event) {
        publish(SystemActionEvent.ActionType.HEADING2);
    }

    @FXML
    private void handleHeading3(ActionEvent event) {
        publish(SystemActionEvent.ActionType.HEADING3);
    }

    @FXML
    private void handleBold(ActionEvent event) {
        publish(SystemActionEvent.ActionType.BOLD);
    }

    @FXML
    private void handleItalic(ActionEvent event) {
        publish(SystemActionEvent.ActionType.ITALIC);
    }

    @FXML
    private void handleUnderline(ActionEvent event) {
        publish(SystemActionEvent.ActionType.STRIKE);
    }

    @FXML
    private void handleRealUnderline(ActionEvent event) {
        publish(SystemActionEvent.ActionType.UNDERLINE);
    }

    @FXML
    private void handleHighlight(ActionEvent event) {
        publish(SystemActionEvent.ActionType.HIGHLIGHT);
    }

    @FXML
    private void handleLink(ActionEvent event) {
        publish(SystemActionEvent.ActionType.LINK);
    }

    @FXML
    private void handleImage(ActionEvent event) {
        publish(SystemActionEvent.ActionType.IMAGE);
    }

    @FXML
    private void handleTodoList(ActionEvent event) {
        publish(SystemActionEvent.ActionType.TODO_LIST);
    }

    @FXML
    private void handleBulletList(ActionEvent event) {
        publish(SystemActionEvent.ActionType.BULLET_LIST);
    }

    @FXML
    private void handleNumberedList(ActionEvent event) {
        publish(SystemActionEvent.ActionType.NUMBERED_LIST);
    }

    @FXML
    private void handleQuote(ActionEvent event) {
        publish(SystemActionEvent.ActionType.QUOTE);
    }

    @FXML
    private void handleCode(ActionEvent event) {
        publish(SystemActionEvent.ActionType.CODE);
    }

    private void publish(SystemActionEvent.ActionType actionType) {
        if (eventBus != null) {
            eventBus.publish(new SystemActionEvent(actionType));
        }
    }
}
