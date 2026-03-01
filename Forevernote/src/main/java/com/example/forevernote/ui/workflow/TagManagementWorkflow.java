package com.example.forevernote.ui.workflow;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.service.TagService;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Encapsulates tag management UI workflows used by MainController.
 */
public class TagManagementWorkflow {
    private static final Logger logger = LoggerConfig.getLogger(TagManagementWorkflow.class);

    public void handleAddTagToNote(
            Note currentNote,
            TagService tagService,
            NoteDAO noteDAO,
            Function<String, String> i18n,
            Consumer<String> statusUpdater,
            Runnable refreshSidebarTags,
            Consumer<Note> reloadCurrentNoteTags) {
        if (currentNote == null) {
            statusUpdater.accept(i18n.apply("status.no_note"));
            return;
        }

        try {
            List<Tag> existingTags = tagService.getAllTags();
            List<Tag> noteTags = noteDAO.fetchTags(currentNote.getId());

            List<String> availableTagNames = existingTags.stream()
                    .filter(tag -> !noteTags.stream().anyMatch(nt -> nt.getId().equals(tag.getId())))
                    .map(Tag::getTitle)
                    .sorted()
                    .toList();

            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle(i18n.apply("dialog.add_tag.title"));
            dialog.setHeaderText(availableTagNames.isEmpty()
                    ? i18n.apply("dialog.add_tag.header_new")
                    : i18n.apply("dialog.add_tag.header_select"));

            ButtonType addButtonType = new ButtonType(i18n.apply("action.add"), ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

            VBox content = new VBox(10);
            ComboBox<String> tagComboBox = new ComboBox<>();
            tagComboBox.setEditable(true);
            tagComboBox.getItems().addAll(availableTagNames);
            tagComboBox.setPromptText(i18n.apply("dialog.add_tag.prompt"));
            tagComboBox.setPrefWidth(300);
            content.getChildren().add(new Label(i18n.apply("label.tag")));
            content.getChildren().add(tagComboBox);
            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(dialogButton -> dialogButton == addButtonType
                    ? tagComboBox.getEditor().getText()
                    : null);

            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty() || result.get().trim().isEmpty()) {
                return;
            }

            String tagName = result.get().trim();
            Optional<Tag> existingTag = existingTags.stream()
                    .filter(t -> t.getTitle().equals(tagName))
                    .findFirst();

            Tag tag;
            if (existingTag.isPresent()) {
                tag = existingTag.get();
            } else {
                tag = new Tag(tagName);
                Tag createdTag = tagService.createTag(tag.getTitle());
                tag.setId(createdTag.getId());
            }

            boolean alreadyHasTag = noteDAO.fetchTags(currentNote.getId()).stream()
                    .anyMatch(t -> t.getId().equals(tag.getId()));

            if (alreadyHasTag) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(i18n.apply("dialog.tag_already_assigned.title"));
                alert.setHeaderText(MessageFormat.format(i18n.apply("dialog.tag_already_assigned.header"), tagName));
                alert.showAndWait();
                return;
            }

            noteDAO.addTag(currentNote, tag);
            reloadCurrentNoteTags.accept(currentNote);
            refreshSidebarTags.run();
            statusUpdater.accept(MessageFormat.format(i18n.apply("status.tag_added"), tagName));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to add tag to note", e);
            statusUpdater.accept(i18n.apply("status.tag_add_error"));
        }
    }

    public void removeTagFromNote(
            Note currentNote,
            Tag tag,
            NoteDAO noteDAO,
            Function<String, String> i18n,
            Consumer<String> statusUpdater,
            Consumer<Note> reloadCurrentNoteTags) {
        if (currentNote == null) {
            statusUpdater.accept(i18n.apply("status.no_note_selected"));
            return;
        }
        if (tag == null || tag.getId() == null) {
            statusUpdater.accept(i18n.apply("status.invalid_tag"));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(i18n.apply("dialog.remove_tag.title"));
        confirm.setHeaderText(MessageFormat.format(i18n.apply("dialog.remove_tag.header"), tag.getTitle()));
        confirm.setContentText(i18n.apply("dialog.remove_tag.content"));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                noteDAO.removeTag(currentNote, tag);
                reloadCurrentNoteTags.accept(currentNote);
                statusUpdater.accept(MessageFormat.format(i18n.apply("status.tag_removed"), tag.getTitle()));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to remove tag from note", e);
                statusUpdater.accept(MessageFormat.format(i18n.apply("status.tag_remove_error"), e.getMessage()));
            }
        }
    }

    public void showTagsManager(
            TagService tagService,
            Function<String, String> i18n,
            Runnable refreshSidebarTags,
            Consumer<String> statusUpdater) {
        try {
            List<Tag> allTags = tagService.getAllTags();

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle(i18n.apply("dialog.tags_manager.title"));
            dialog.setHeaderText(i18n.apply("dialog.tags_manager.header"));

            ButtonType closeButton = new ButtonType(i18n.apply("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
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
                        Label dateLabel = new Label(tag.getCreatedDate() != null ? tag.getCreatedDate() : i18n.apply("label.not_available"));
                        dateLabel.setStyle("-fx-text-fill: gray;");

                        ButtonType deleteType = new ButtonType(i18n.apply("action.delete"), ButtonBar.ButtonData.OK_DONE);
                        javafx.scene.control.Button deleteButton = new javafx.scene.control.Button(i18n.apply("action.delete"));
                        deleteButton.setOnAction(e -> {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle(i18n.apply("dialog.delete_tag.title"));
                            confirm.setHeaderText(i18n.apply("dialog.tags_manager.delete_header"));
                            confirm.setContentText(i18n.apply("dialog.tags_manager.delete_content"));
                            confirm.getButtonTypes().setAll(deleteType, ButtonType.CANCEL);
                            Optional<ButtonType> result = confirm.showAndWait();
                            if (result.isPresent() && result.get() == deleteType) {
                                try {
                                    tagService.deleteTag(tag.getId());
                                    tagListView.getItems().remove(tag);
                                    refreshSidebarTags.run();
                                    statusUpdater.accept(MessageFormat.format(i18n.apply("status.tag_deleted"), tag.getTitle()));
                                } catch (Exception ex) {
                                    logger.log(Level.SEVERE, "Failed to delete tag from tags manager", ex);
                                    statusUpdater.accept(i18n.apply("status.error_deleting_tag"));
                                }
                            }
                        });

                        hbox.getChildren().addAll(nameLabel, dateLabel, deleteButton);
                        setGraphic(hbox);
                    }
                }
            });

            content.getChildren().add(new Label(MessageFormat.format(i18n.apply("dialog.tags_manager.all_tags_count"), allTags.size())));
            content.getChildren().add(tagListView);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefSize(500, 400);

            dialog.showAndWait();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to open tags manager", e);
            statusUpdater.accept(i18n.apply("status.tags_manager_error"));
        }
    }
}
