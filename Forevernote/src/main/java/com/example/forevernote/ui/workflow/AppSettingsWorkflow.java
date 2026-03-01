package com.example.forevernote.ui.workflow;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import com.example.forevernote.ui.controller.ToolbarController;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/**
 * Encapsulates app settings UI flows (theme/language/storage).
 */
public class AppSettingsWorkflow {

    public void handleSwitchStorage(Window owner, Function<String, String> i18n, Preferences prefs) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(i18n.apply("pref.storage"));
        alert.setHeaderText(i18n.apply("pref.storage.header"));
        alert.setContentText(i18n.apply("pref.storage.content"));

        ButtonType sqliteBtn = new ButtonType(i18n.apply("pref.storage.sqlite"));
        ButtonType filesystemBtn = new ButtonType(i18n.apply("pref.storage.filesystem"));
        ButtonType cancelBtn = new ButtonType(i18n.apply("action.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(sqliteBtn, filesystemBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == cancelBtn) {
            return;
        }

        String newType = "sqlite";
        String customPath = "";
        boolean changed = false;

        String currentType = prefs.get("storage_type", "sqlite");
        String currentPath = prefs.get("filesystem_path", "");

        if (result.get() == sqliteBtn) {
            newType = "sqlite";
            changed = !newType.equals(currentType);
        } else if (result.get() == filesystemBtn) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(i18n.apply("pref.storage.browse"));

            File selectedDirectory = directoryChooser.showDialog(owner);
            if (selectedDirectory == null) {
                return;
            }
            newType = "filesystem";
            customPath = selectedDirectory.getAbsolutePath();
            changed = !newType.equals(currentType) || !customPath.equals(currentPath);
        }

        if (!changed) {
            return;
        }

        prefs.put("storage_type", newType);
        prefs.put("filesystem_path", customPath);

        Alert restartAlert = new Alert(Alert.AlertType.INFORMATION);
        restartAlert.setTitle(i18n.apply("app.restart_required"));
        restartAlert.setHeaderText(null);
        restartAlert.setContentText(i18n.apply("app.restart_storage_message"));
        restartAlert.showAndWait();
    }

    public ToggleGroup initializeThemeMenu(ToolbarController toolbarController, String currentTheme,
            Supplier<String> detectSystemTheme,
            Consumer<String> setCurrentTheme,
            Runnable updateThemeMenuSelection,
            Runnable applyTheme) {
        ToggleGroup themeToggleGroup = new ToggleGroup();

        if (toolbarController != null) {
            if (toolbarController.getLightThemeMenuItem() != null) {
                toolbarController.getLightThemeMenuItem().setToggleGroup(themeToggleGroup);
            }
            if (toolbarController.getDarkThemeMenuItem() != null) {
                toolbarController.getDarkThemeMenuItem().setToggleGroup(themeToggleGroup);
            }
            if (toolbarController.getSystemThemeMenuItem() != null) {
                toolbarController.getSystemThemeMenuItem().setToggleGroup(themeToggleGroup);
            }
        }

        if (updateThemeMenuSelection != null) {
            updateThemeMenuSelection.run();
        }

        Platform.runLater(() -> {
            if ("system".equals(currentTheme) && detectSystemTheme != null && setCurrentTheme != null) {
                String detectedTheme = detectSystemTheme.get();
                String savedTheme = currentTheme;
                setCurrentTheme.accept(detectedTheme);
                if (applyTheme != null) {
                    applyTheme.run();
                }
                setCurrentTheme.accept(savedTheme);
            } else if (applyTheme != null) {
                applyTheme.run();
            }
        });

        return themeToggleGroup;
    }

    public ToggleGroup initializeLanguageMenu(ToolbarController toolbarController, Preferences prefs) {
        ToggleGroup languageToggleGroup = new ToggleGroup();
        if (toolbarController == null) {
            return languageToggleGroup;
        }

        if (toolbarController.getEnglishLangMenuItem() != null) {
            toolbarController.getEnglishLangMenuItem().setToggleGroup(languageToggleGroup);
        }
        if (toolbarController.getSpanishLangMenuItem() != null) {
            toolbarController.getSpanishLangMenuItem().setToggleGroup(languageToggleGroup);
        }

        String currentLang = prefs.get("language", Locale.getDefault().getLanguage());
        if ("es".equals(currentLang)) {
            if (toolbarController.getSpanishLangMenuItem() != null) {
                toolbarController.getSpanishLangMenuItem().setSelected(true);
            }
        } else {
            if (toolbarController.getEnglishLangMenuItem() != null) {
                toolbarController.getEnglishLangMenuItem().setSelected(true);
            }
        }

        return languageToggleGroup;
    }

    public boolean changeLanguage(String lang, Preferences prefs) {
        String currentLang = prefs.get("language", Locale.getDefault().getLanguage());
        if (currentLang.equals(lang)) {
            return false;
        }
        prefs.put("language", lang);
        return true;
    }

    public void updateThemeMenuSelection(ToolbarController toolbarController, ToggleGroup themeToggleGroup,
            String currentTheme) {
        if (themeToggleGroup == null || toolbarController == null) {
            return;
        }

        if ("dark".equals(currentTheme)) {
            if (toolbarController.getDarkThemeMenuItem() != null) {
                toolbarController.getDarkThemeMenuItem().setSelected(true);
            }
            return;
        }

        if ("system".equalsIgnoreCase(currentTheme)) {
            if (toolbarController.getSystemThemeMenuItem() != null) {
                toolbarController.getSystemThemeMenuItem().setSelected(true);
            }
            return;
        }

        if (toolbarController.getLightThemeMenuItem() != null) {
            toolbarController.getLightThemeMenuItem().setSelected(true);
        }
    }
}
