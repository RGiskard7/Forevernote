package com.example.forevernote.ui.workflow;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.SystemActionEvent;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Encapsulates editor commands and formatting actions.
 */
public class EditorCommandWorkflow {

    public void handleUndo(TextArea noteContentArea) {
        if (noteContentArea != null) {
            noteContentArea.undo();
        }
    }

    public void handleRedo(Function<String, String> i18n, Consumer<String> statusConsumer) {
        if (i18n != null && statusConsumer != null) {
            statusConsumer.accept(i18n.apply("status.redo_not_available"));
        }
    }

    public void handleCut(TextArea noteContentArea, TextField noteTitleField) {
        if (noteContentArea != null && noteContentArea.getSelectedText() != null) {
            noteContentArea.cut();
        } else if (noteTitleField != null && noteTitleField.getSelectedText() != null) {
            noteTitleField.cut();
        }
    }

    public void handleCopy(TextArea noteContentArea, TextField noteTitleField) {
        if (noteContentArea != null && noteContentArea.getSelectedText() != null) {
            noteContentArea.copy();
        } else if (noteTitleField != null && noteTitleField.getSelectedText() != null) {
            noteTitleField.copy();
        }
    }

    public void handlePaste(TextArea noteContentArea, TextField noteTitleField) {
        if (noteContentArea != null && noteContentArea.isFocused()) {
            noteContentArea.paste();
        } else if (noteTitleField != null && noteTitleField.isFocused()) {
            noteTitleField.paste();
        }
    }

    public void handleFind(TextArea noteContentArea, Function<String, String> i18n, Consumer<String> statusConsumer) {
        if (noteContentArea == null || i18n == null || statusConsumer == null) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(i18n.apply("dialog.find.title"));
        dialog.setHeaderText(i18n.apply("dialog.find.header"));
        dialog.setContentText(i18n.apply("dialog.find.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            return;
        }

        String searchText = result.get().trim();
        String content = noteContentArea.getText();
        int index = content.indexOf(searchText);
        if (index >= 0) {
            noteContentArea.selectRange(index, index + searchText.length());
            noteContentArea.requestFocus();
            statusConsumer.accept(java.text.MessageFormat.format(i18n.apply("status.found_text"), searchText));
        } else {
            statusConsumer.accept(java.text.MessageFormat.format(i18n.apply("status.text_not_found"), searchText));
        }
    }

    public void handleReplace(TextArea noteContentArea, Function<String, String> i18n, Consumer<String> statusConsumer) {
        if (noteContentArea == null || i18n == null || statusConsumer == null) {
            if (i18n != null && statusConsumer != null) {
                statusConsumer.accept(i18n.apply("status.no_note_open"));
            }
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(i18n.apply("dialog.replace.title"));
        dialog.setHeaderText(i18n.apply("dialog.replace.header"));

        ButtonType replaceButton = new ButtonType(i18n.apply("action.replace_one"), ButtonBar.ButtonData.OK_DONE);
        ButtonType replaceAllButton = new ButtonType(i18n.apply("action.replace_all"), ButtonBar.ButtonData.APPLY);
        dialog.getDialogPane().getButtonTypes().addAll(replaceButton, replaceAllButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(20));

        TextField findField = new TextField();
        findField.setPromptText(i18n.apply("dialog.replace.find_prompt"));
        TextField replaceField = new TextField();
        replaceField.setPromptText(i18n.apply("dialog.replace.with_prompt"));

        content.getChildren().addAll(
                new Label(i18n.apply("dialog.replace.find_label")), findField,
                new Label(i18n.apply("dialog.replace.with_label")), replaceField);

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(buttonType -> {
            if (buttonType == replaceButton || buttonType == replaceAllButton) {
                return findField.getText() + "|" + replaceField.getText() + "|" +
                        (buttonType == replaceAllButton ? "all" : "one");
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String[] parts = result.get().split("\\|");
        if (parts.length != 3) {
            return;
        }

        String find = parts[0];
        String replace = parts[1];
        boolean replaceAll = parts[2].equals("all");
        String text = noteContentArea.getText();

        if (replaceAll) {
            String newContent = text.replace(find, replace);
            noteContentArea.setText(newContent);
            statusConsumer.accept(i18n.apply("status.replaced_all"));
            return;
        }

        int index = text.indexOf(find);
        if (index >= 0) {
            String newContent = text.substring(0, index) + replace + text.substring(index + find.length());
            noteContentArea.setText(newContent);
            noteContentArea.selectRange(index, index + replace.length());
            statusConsumer.accept(i18n.apply("status.replaced_first"));
        } else {
            statusConsumer.accept(i18n.apply("status.text_not_found_general"));
        }
    }

    public void publishAction(EventBus eventBus, SystemActionEvent.ActionType actionType) {
        if (eventBus != null && actionType != null) {
            eventBus.publish(new SystemActionEvent(actionType));
        }
    }

    public void handleAttachmentNotSupported(Function<String, String> i18n, Consumer<String> statusConsumer) {
        if (i18n != null && statusConsumer != null) {
            statusConsumer.accept(i18n.apply("status.attachments_not_supported"));
        }
    }
}
