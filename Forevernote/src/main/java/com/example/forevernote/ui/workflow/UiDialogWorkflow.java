package com.example.forevernote.ui.workflow;

import java.util.Optional;
import java.util.List;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * Centralizes lightweight app dialogs used from MainController.
 */
public class UiDialogWorkflow {

    public record PreferencesDialogResult(
            String sidebarTabsMode,
            String editorButtonsMode,
            boolean autosaveEnabled,
            int autosaveIdleMs,
            String themeSource,
            String externalThemeId,
            boolean accentEnabled,
            String accentColor) {
    }

    public Optional<PreferencesDialogResult> showPreferences(
            Function<String, String> i18n,
            UiPreferencesWorkflow.UiPreferences current,
            List<ThemeCatalogWorkflow.ThemeDescriptor> themes) {
        Dialog<PreferencesDialogResult> dialog = new Dialog<>();
        dialog.setTitle(i18n.apply("dialog.preferences.title"));
        dialog.setHeaderText(i18n.apply("dialog.preferences.header"));

        ButtonType saveButton = new ButtonType(i18n.apply("action.save"), ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType(i18n.apply("action.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, closeButton);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label dbLabel = new Label(i18n.apply("dialog.preferences.db_location"));
        Label dbPathLabel = new Label("Forevernote/data/database.db");
        dbPathLabel.setStyle("-fx-text-fill: gray;");

        Label sidebarTabsModeLabel = new Label(i18n.apply("dialog.preferences.sidebar_tabs_mode"));
        ComboBox<String> sidebarTabsModeCombo = new ComboBox<>();
        sidebarTabsModeCombo.getItems().addAll(
                i18n.apply("pref.mode.text"),
                i18n.apply("pref.mode.icons"));
        boolean sidebarIcons = UiPreferencesWorkflow.MODE_ICONS.equals(current.sidebarTabsMode());
        sidebarTabsModeCombo.getSelectionModel().select(sidebarIcons ? 1 : 0);

        Label editorButtonsModeLabel = new Label(i18n.apply("dialog.preferences.editor_buttons_mode"));
        ComboBox<String> editorButtonsModeCombo = new ComboBox<>();
        editorButtonsModeCombo.getItems().addAll(
                i18n.apply("pref.mode.text"),
                i18n.apply("pref.mode.icons"),
                i18n.apply("pref.mode.auto"));
        String editorMode = current.editorViewModeButtonsMode();
        int editorIdx = UiPreferencesWorkflow.MODE_ICONS.equals(editorMode) ? 1
                : UiPreferencesWorkflow.MODE_AUTO.equals(editorMode) ? 2 : 0;
        editorButtonsModeCombo.getSelectionModel().select(editorIdx);

        CheckBox autosaveEnabledCheck = new CheckBox(i18n.apply("dialog.preferences.autosave_enabled"));
        autosaveEnabledCheck.setSelected(current.autosaveEnabled());

        Label autosaveIntervalLabel = new Label(i18n.apply("dialog.preferences.autosave_interval"));
        ComboBox<String> autosaveIntervalCombo = new ComboBox<>();
        autosaveIntervalCombo.getItems().addAll("1s", "2s", "5s");
        String autosaveText = current.autosaveIdleMs() <= 1200 ? "1s"
                : current.autosaveIdleMs() <= 3000 ? "2s" : "5s";
        autosaveIntervalCombo.getSelectionModel().select(autosaveText);
        autosaveIntervalCombo.setDisable(!autosaveEnabledCheck.isSelected());
        autosaveEnabledCheck.selectedProperty().addListener((obs, oldVal, newVal) -> autosaveIntervalCombo.setDisable(!newVal));

        Label themeModeLabel = new Label(i18n.apply("dialog.preferences.theme_mode"));
        ComboBox<String> themeModeCombo = new ComboBox<>();
        themeModeCombo.getItems().addAll(i18n.apply("pref.theme.builtin"), i18n.apply("pref.theme.external"));
        themeModeCombo.getSelectionModel().select(
                UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL.equals(current.themeSource()) ? 1 : 0);

        Label externalThemeLabel = new Label(i18n.apply("dialog.preferences.external_theme"));
        ComboBox<String> externalThemeCombo = new ComboBox<>();
        externalThemeCombo.getItems().add(i18n.apply("pref.theme.none"));
        int externalSelected = 0;
        if (themes != null) {
            int idx = 1;
            for (ThemeCatalogWorkflow.ThemeDescriptor descriptor : themes) {
                if (!"external".equals(descriptor.source())) {
                    continue;
                }
                String display = descriptor.name() + " [" + descriptor.id() + "]";
                externalThemeCombo.getItems().add(display);
                if (descriptor.id().equals(current.externalThemeId())) {
                    externalSelected = idx;
                }
                idx++;
            }
        }
        externalThemeCombo.getSelectionModel().select(externalSelected);
        externalThemeCombo.setDisable(themeModeCombo.getSelectionModel().getSelectedIndex() != 1);
        themeModeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            externalThemeCombo.setDisable(themeModeCombo.getSelectionModel().getSelectedIndex() != 1);
        });

        CheckBox accentEnabledCheck = new CheckBox(i18n.apply("dialog.preferences.accent_enabled"));
        accentEnabledCheck.setSelected(current.accentEnabled());
        ColorPicker accentColorPicker = new ColorPicker(parseColor(current.accentColor(), Color.web("#7c3aed")));
        accentColorPicker.setPrefWidth(260);
        accentColorPicker.setMinWidth(260);
        Label accentHintLabel = new Label(i18n.apply("dialog.preferences.accent_scope_hint"));
        accentHintLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        accentHintLabel.setWrapText(true);

        Runnable refreshAccentAvailability = () -> {
            boolean externalSource = themeModeCombo.getSelectionModel().getSelectedIndex() == 1;
            boolean supportsAccent = !externalSource
                    || externalThemeSupportsAccentOverride(themes, externalThemeCombo.getSelectionModel().getSelectedItem());
            accentEnabledCheck.setDisable(!supportsAccent);
            if (!supportsAccent) {
                accentEnabledCheck.setSelected(false);
            }
            accentColorPicker.setDisable(!supportsAccent || !accentEnabledCheck.isSelected());
            String hint = supportsAccent ? i18n.apply("dialog.preferences.accent_scope_hint")
                    : i18n.apply("dialog.preferences.accent_scope_external");
            accentHintLabel.setText(hint);
            accentHintLabel.setTooltip(new Tooltip(hint));
        };
        accentEnabledCheck.selectedProperty()
                .addListener((obs, oldVal, newVal) -> refreshAccentAvailability.run());
        themeModeCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> refreshAccentAvailability.run());
        externalThemeCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> refreshAccentAvailability.run());
        refreshAccentAvailability.run();

        content.getChildren().addAll(
                new Label(i18n.apply("dialog.preferences.general_settings")),
                dbLabel, dbPathLabel,
                new Separator(),
                sidebarTabsModeLabel, sidebarTabsModeCombo,
                editorButtonsModeLabel, editorButtonsModeCombo,
                autosaveEnabledCheck,
                new HBox(8, autosaveIntervalLabel, autosaveIntervalCombo),
                new Separator(),
                themeModeLabel, themeModeCombo,
                externalThemeLabel, externalThemeCombo,
                new Separator(),
                accentEnabledCheck, accentColorPicker, accentHintLabel);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(480, 520);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButton) {
                return null;
            }
            String sidebarMode = sidebarTabsModeCombo.getSelectionModel().getSelectedIndex() == 1
                    ? UiPreferencesWorkflow.MODE_ICONS
                    : UiPreferencesWorkflow.MODE_TEXT;
            String editorButtonsMode = switch (editorButtonsModeCombo.getSelectionModel().getSelectedIndex()) {
                case 1 -> UiPreferencesWorkflow.MODE_ICONS;
                case 2 -> UiPreferencesWorkflow.MODE_AUTO;
                default -> UiPreferencesWorkflow.MODE_TEXT;
            };
            int autosaveMs = switch (autosaveIntervalCombo.getSelectionModel().getSelectedItem()) {
                case "1s" -> 1000;
                case "5s" -> 5000;
                default -> 2000;
            };
            String themeSource = themeModeCombo.getSelectionModel().getSelectedIndex() == 1
                    ? UiPreferencesWorkflow.THEME_SOURCE_EXTERNAL
                    : UiPreferencesWorkflow.THEME_SOURCE_BUILTIN;
            String externalThemeId = "";
            String selectedExternal = externalThemeCombo.getSelectionModel().getSelectedItem();
            if (selectedExternal != null && selectedExternal.contains("[") && selectedExternal.endsWith("]")) {
                int start = selectedExternal.lastIndexOf('[');
                externalThemeId = selectedExternal.substring(start + 1, selectedExternal.length() - 1).trim();
            }
            return new PreferencesDialogResult(
                    sidebarMode,
                    editorButtonsMode,
                    autosaveEnabledCheck.isSelected(),
                    autosaveMs,
                    themeSource,
                    externalThemeId,
                    accentEnabledCheck.isSelected(),
                    toHex(accentColorPicker.getValue()));
        });
        return dialog.showAndWait();
    }

    private Color parseColor(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        try {
            return Color.web(hex.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String toHex(Color color) {
        if (color == null) {
            return "#7c3aed";
        }
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private boolean externalThemeSupportsAccentOverride(
            List<ThemeCatalogWorkflow.ThemeDescriptor> themes,
            String selectedExternalDisplay) {
        if (themes == null || selectedExternalDisplay == null || selectedExternalDisplay.isBlank()) {
            return false;
        }
        String id = extractExternalThemeId(selectedExternalDisplay);
        if (id.isBlank()) {
            return false;
        }
        for (ThemeCatalogWorkflow.ThemeDescriptor descriptor : themes) {
            if (id.equals(descriptor.id())) {
                return descriptor.supportsAccentOverride();
            }
        }
        return false;
    }

    private String extractExternalThemeId(String selectedExternalDisplay) {
        if (selectedExternalDisplay == null) {
            return "";
        }
        String value = selectedExternalDisplay.trim();
        if (value.contains("[") && value.endsWith("]")) {
            int start = value.lastIndexOf('[');
            if (start >= 0 && start < value.length() - 1) {
                return value.substring(start + 1, value.length() - 1).trim();
            }
        }
        return "";
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
