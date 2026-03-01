package com.example.forevernote.ui.workflow;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.example.forevernote.AppConfig;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.service.TagService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

/**
 * Centralizes lightweight app dialogs used from MainController.
 */
public class UiDialogWorkflow {

    public void showPreferences(Function<String, String> i18n) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(i18n.apply("dialog.preferences.title"));
        dialog.setHeaderText(i18n.apply("dialog.preferences.header"));

        ButtonType closeButton = new ButtonType(i18n.apply("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label dbLabel = new Label(i18n.apply("dialog.preferences.db_location"));
        Label dbPathLabel = new Label("Forevernote/data/database.db");
        dbPathLabel.setStyle("-fx-text-fill: gray;");

        Label autoSaveLabel = new Label(i18n.apply("dialog.preferences.autosave_placeholder"));
        autoSaveLabel.setStyle("-fx-text-fill: gray;");

        content.getChildren().addAll(
                new Label(i18n.apply("dialog.preferences.general_settings")),
                dbLabel, dbPathLabel,
                new Separator(),
                autoSaveLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(400, 300);
        dialog.showAndWait();
    }

    public void showDocumentation(Function<String, String> i18n) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.apply("dialog.documentation.title"));
        alert.setHeaderText(i18n.apply("dialog.documentation.header"));
        alert.setContentText(i18n.apply("dialog.documentation.content"));
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500, 400);
        alert.showAndWait();
    }

    public void showKeyboardShortcuts(Function<String, String> i18n) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.apply("dialog.shortcuts.title"));
        alert.setHeaderText(i18n.apply("dialog.shortcuts.header"));
        alert.setContentText(i18n.apply("dialog.shortcuts.content"));
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(450, 500);
        alert.showAndWait();
    }

    public void showAbout(Function<String, String> i18n) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(i18n.apply("dialog.about.title"));

        ButtonType closeButton = new ButtonType(i18n.apply("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeButton);

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(i18n.apply("about.app_name"));
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        Label versionLabel = new Label("Version " + AppConfig.getAppVersion());
        versionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");

        Label descLabel = new Label(AppConfig.getAppDescription());
        descLabel.setStyle("-fx-font-size: 13px;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(350);
        descLabel.setAlignment(Pos.CENTER);

        Separator separator = new Separator();
        separator.setPrefWidth(300);

        Label techLabel = new Label(i18n.apply("about.tech_stack"));
        techLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        Label copyrightLabel = new Label(AppConfig.getAppCopyright());
        copyrightLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        Label developerLabel = new Label(i18n.apply("about.developer_credit"));
        developerLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        content.getChildren().addAll(
                titleLabel, versionLabel, descLabel,
                separator,
                techLabel, copyrightLabel, developerLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(400, 320);
        dialog.showAndWait();
    }

    public void handleNewTag(TagService tagService, Function<String, String> i18n,
            Runnable refreshTagsAction, Consumer<String> statusConsumer, Consumer<Exception> errorConsumer) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(i18n.apply("dialog.new_tag.title"));
        dialog.setHeaderText(i18n.apply("dialog.new_tag.header"));
        dialog.setContentText(i18n.apply("dialog.new_tag.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            return;
        }

        try {
            String tagName = result.get().trim();
            if (tagService.tagExists(tagName)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(i18n.apply("dialog.tag_exists.title"));
                alert.setHeaderText(i18n.apply("dialog.tag_exists.header"));
                alert.setContentText(i18n.apply("dialog.tag_exists.content"));
                alert.showAndWait();
                return;
            }

            Tag createdTag = tagService.createTag(tagName);
            Tag newTag = new Tag(tagName);
            newTag.setId(createdTag.getId());
            if (refreshTagsAction != null) {
                refreshTagsAction.run();
            }
            if (statusConsumer != null) {
                statusConsumer.accept(java.text.MessageFormat.format(i18n.apply("status.tag_created"), tagName));
            }
        } catch (Exception e) {
            if (errorConsumer != null) {
                errorConsumer.accept(e);
            }
            if (statusConsumer != null) {
                statusConsumer.accept(i18n.apply("status.error") + ": " + e.getMessage());
            }
        }
    }
}
