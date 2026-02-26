package com.example.forevernote.ui.controller;

import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.event.events.SystemActionEvent;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.service.NoteService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.util.Optional;
import java.util.ResourceBundle;

public class EditorController {

    private EventBus eventBus;
    private NoteService noteService;
    private ResourceBundle bundle;

    private Note currentNote;
    private boolean isModified = false;

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
        subscribeToEvents();
    }

    public void setServices(NoteService noteService) {
        this.noteService = noteService;
    }

    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    public Note getCurrentNote() {
        return currentNote;
    }

    public boolean isModified() {
        return isModified;
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

    private void subscribeToEvents() {
        if (eventBus == null)
            return;

        eventBus.subscribe(SystemActionEvent.class, event -> {
            Platform.runLater(() -> {
                switch (event.getActionType()) {
                    case BOLD:
                        insertMarkdownFormat("**", "**");
                        break;
                    case ITALIC:
                        insertMarkdownFormat("*", "*");
                        break;
                    case UNDERLINE:
                        insertMarkdownFormat("<u>", "</u>");
                        break;
                    case STRIKE:
                        insertMarkdownFormat("~~", "~~");
                        break;
                    case HIGHLIGHT:
                        insertMarkdownFormat("==", "==");
                        break;
                    case HEADING1:
                        insertLinePrefix("# ");
                        break;
                    case HEADING2:
                        insertLinePrefix("## ");
                        break;
                    case HEADING3:
                        insertLinePrefix("### ");
                        break;
                    case BULLET_LIST:
                        insertLinePrefix("- ");
                        break;
                    case NUMBERED_LIST:
                        insertLinePrefix("1. ");
                        break;
                    case TODO_LIST:
                        insertTodoList();
                        break;
                    case QUOTE:
                        insertLinePrefix("> ");
                        break;
                    case CODE:
                        insertCodeBlock();
                        break;
                    case LINK:
                        handleLink();
                        break;
                    case IMAGE:
                        handleImage();
                        break;
                    case SAVE:
                        handleSave();
                        break;
                    default:
                        break;
                }
            });
        });

        // Listen for internal text changes to set isModified
        if (noteContentArea != null) {
            noteContentArea.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentNote != null
                        && !newVal.equals(currentNote.getContent() != null ? currentNote.getContent() : "")) {
                    isModified = true;
                    if (eventBus != null) {
                        eventBus.publish(new NoteEvents.NoteModifiedEvent(currentNote));
                    }
                }
            });
        }

        if (noteTitleField != null) {
            noteTitleField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (currentNote != null
                        && !newVal.equals(currentNote.getTitle() != null ? currentNote.getTitle() : "")) {
                    isModified = true;
                    if (eventBus != null) {
                        eventBus.publish(new NoteEvents.NoteModifiedEvent(currentNote));
                    }
                }
            });
        }
    }

    public void loadNote(Note note) {
        if (note == null) {
            currentNote = null;
            if (noteTitleField != null)
                noteTitleField.clear();
            if (noteContentArea != null)
                noteContentArea.clear();
            isModified = false;
            return;
        }

        if (isModified && currentNote != null) {
            handleSave();
        }

        if (noteService != null) {
            Optional<Note> optionalNote = noteService.getNoteById(note.getId());
            currentNote = optionalNote.orElse(note);
        } else {
            currentNote = note;
        }

        if (noteTitleField != null)
            noteTitleField.setText(currentNote.getTitle() != null ? currentNote.getTitle() : "");
        if (noteContentArea != null)
            noteContentArea.setText(currentNote.getContent() != null ? currentNote.getContent() : "");

        isModified = false;
    }

    public void handleSave() {
        if (currentNote != null && isModified && noteService != null) {
            if (noteTitleField != null)
                currentNote.setTitle(noteTitleField.getText());
            if (noteContentArea != null)
                currentNote.setContent(noteContentArea.getText());
            noteService.updateNote(currentNote);
            isModified = false;

            if (eventBus != null) {
                eventBus.publish(new NoteEvents.NoteSavedEvent(currentNote));
            }
        }
    }

    private void insertMarkdownFormat(String prefix, String suffix) {
        if (noteContentArea == null)
            return;

        String selectedText = noteContentArea.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            String formatted = prefix + selectedText + suffix;
            noteContentArea.replaceSelection(formatted);
        } else {
            int caretPos = noteContentArea.getCaretPosition();
            String text = noteContentArea.getText() != null ? noteContentArea.getText() : "";
            String newText = text.substring(0, caretPos) + prefix + suffix + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + prefix.length());
        }
        noteContentArea.requestFocus();
        isModified = true;
    }

    private void insertLinePrefix(String prefix) {
        if (noteContentArea == null)
            return;
        int caretPos = noteContentArea.getCaretPosition();
        String text = noteContentArea.getText() != null ? noteContentArea.getText() : "";

        int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
        String lineText = text.substring(lineStart, caretPos);

        if (lineText.trim().isEmpty() && lineStart == caretPos) {
            String newText = text.substring(0, caretPos) + prefix + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + prefix.length());
        } else {
            String newText = text.substring(0, caretPos) + "\n" + prefix + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + prefix.length() + 1);
        }
        noteContentArea.requestFocus();
        isModified = true;
    }

    private void insertTodoList() {
        if (noteContentArea == null)
            return;
        int caretPos = noteContentArea.getCaretPosition();
        String text = noteContentArea.getText();
        String newLine = "- [ ] ";
        int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;
        String lineText = text.substring(lineStart, caretPos);

        if (lineText.trim().isEmpty()) {
            String newText = text.substring(0, caretPos) + newLine + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + newLine.length());
        } else {
            String newText = text.substring(0, caretPos) + "\n" + newLine + text.substring(caretPos);
            noteContentArea.setText(newText);
            noteContentArea.positionCaret(caretPos + newLine.length() + 1);
        }
        noteContentArea.requestFocus();
        isModified = true;
    }

    private void insertCodeBlock() {
        if (noteContentArea == null)
            return;
        String selectedText = noteContentArea.getSelectedText();
        if (selectedText != null && selectedText.contains("\n")) {
            insertMarkdownFormat("```\n", "\n```");
        } else {
            insertMarkdownFormat("`", "`");
        }
    }

    private void handleLink() {
        if (noteContentArea == null)
            return;
        TextInputDialog dialog = new TextInputDialog(getString("dialog.link.default_url", "https://"));
        dialog.setTitle(getString("dialog.link.title", "Insert Link"));
        dialog.setHeaderText(getString("dialog.link.header", "Enter URL:"));
        dialog.setContentText(getString("dialog.link.content", "URL:"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String url = result.get().trim();
            String selectedText = noteContentArea.getSelectedText();
            String linkText = (selectedText != null && !selectedText.isEmpty()) ? selectedText
                    : getString("dialog.link.default_text", "link text");
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
        }
    }

    private void handleImage() {
        if (noteContentArea == null)
            return;
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle(getString("dialog.image.title", "Insert Image"));
        dialog.setHeaderText(getString("dialog.image.header", "Enter image URL or path:"));
        dialog.setContentText(getString("dialog.image.content", "Image:"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String imagePath = result.get().trim();
            String selectedText = noteContentArea.getSelectedText();
            String altText = (selectedText != null && !selectedText.isEmpty()) ? selectedText
                    : getString("dialog.image.default_alt", "image");
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
        }
    }

    private String getString(String key, String fallback) {
        if (bundle == null) {
            return fallback;
        }
        return bundle.containsKey(key) ? bundle.getString(key) : fallback;
    }
}
