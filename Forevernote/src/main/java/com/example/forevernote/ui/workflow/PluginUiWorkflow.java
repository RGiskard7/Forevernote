package com.example.forevernote.ui.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.example.forevernote.plugin.PluginManager;
import com.example.forevernote.ui.controller.ToolbarController;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Encapsulates plugin dynamic UI registration/removal for menu, side panels and status bar.
 */
public class PluginUiWorkflow {

    public void registerMenuItem(String pluginId, String category, String itemName, String shortcut, Runnable action,
            ToolbarController toolbarController,
            Map<String, Menu> pluginCategoryMenus,
            Map<String, List<MenuItem>> pluginMenuItems,
            Supplier<PluginManager> pluginManagerSupplier,
            Function<String, String> i18n,
            Consumer<String> statusConsumer,
            Consumer<String> fineLogger,
            Consumer<String> warningLogger,
            Consumer<Exception> warningWithExceptionLogger) {
        Platform.runLater(() -> {
            if (toolbarController == null || toolbarController.getPluginsMenu() == null) {
                if (warningLogger != null) {
                    warningLogger.accept("Plugins menu not available for registration: " + itemName);
                }
                return;
            }

            Menu categoryMenu = pluginCategoryMenus.get(category);
            if (categoryMenu == null) {
                categoryMenu = new Menu(category);
                pluginCategoryMenus.put(category, categoryMenu);
                int insertIndex = Math.min(toolbarController.getPluginsMenu().getItems().size(), 2);
                toolbarController.getPluginsMenu().getItems().add(insertIndex, categoryMenu);
            }

            MenuItem menuItem = new MenuItem(itemName);
            menuItem.setOnAction(e -> {
                PluginManager pluginManager = pluginManagerSupplier != null ? pluginManagerSupplier.get() : null;
                if (pluginManager != null && pluginManager.isPluginEnabled(pluginId)) {
                    action.run();
                } else if (statusConsumer != null && i18n != null) {
                    statusConsumer.accept(java.text.MessageFormat.format(i18n.apply("status.plugin_not_enabled"), pluginId));
                }
            });

            if (shortcut != null && !shortcut.isEmpty()) {
                try {
                    menuItem.setAccelerator(KeyCombination.keyCombination(shortcut));
                } catch (Exception ex) {
                    if (warningWithExceptionLogger != null) {
                        warningWithExceptionLogger.accept(ex);
                    }
                }
            }

            categoryMenu.getItems().add(menuItem);
            pluginMenuItems.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(menuItem);

            if (fineLogger != null) {
                fineLogger.accept("Registered menu item: " + category + " > " + itemName + " for plugin " + pluginId);
            }
        });
    }

    public void addMenuSeparator(String pluginId, String category,
            Map<String, Menu> pluginCategoryMenus,
            Map<String, List<MenuItem>> pluginMenuItems) {
        Platform.runLater(() -> {
            Menu categoryMenu = pluginCategoryMenus.get(category);
            if (categoryMenu != null) {
                SeparatorMenuItem separator = new SeparatorMenuItem();
                categoryMenu.getItems().add(separator);
                pluginMenuItems.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(separator);
            }
        });
    }

    public void removePluginMenuItems(String pluginId,
            ToolbarController toolbarController,
            Map<String, Menu> pluginCategoryMenus,
            Map<String, List<MenuItem>> pluginMenuItems,
            Consumer<String> infoLogger) {
        Platform.runLater(() -> {
            List<MenuItem> items = pluginMenuItems.remove(pluginId);
            if (items == null) {
                return;
            }

            for (MenuItem item : items) {
                for (Menu categoryMenu : pluginCategoryMenus.values()) {
                    categoryMenu.getItems().remove(item);
                }
            }

            pluginCategoryMenus.entrySet().removeIf(entry -> {
                Menu menu = entry.getValue();
                if (menu.getItems().isEmpty()) {
                    if (toolbarController != null && toolbarController.getPluginsMenu() != null) {
                        toolbarController.getPluginsMenu().getItems().remove(menu);
                    }
                    return true;
                }
                return false;
            });

            if (infoLogger != null) {
                infoLogger.accept("Removed menu items for plugin: " + pluginId);
            }
        });
    }

    public void registerSidePanel(String pluginId, String panelId, String title, Node content, String icon,
            VBox pluginPanelsContainer,
            Map<String, VBox> pluginPanels,
            Map<String, List<String>> pluginPanelIds,
            Consumer<String> fineLogger,
            Consumer<String> warningLogger) {
        Platform.runLater(() -> {
            if (pluginPanelsContainer == null) {
                if (warningLogger != null) {
                    warningLogger.accept("Plugin panels container not available for registration: " + panelId);
                }
                return;
            }

            String fullPanelId = pluginId + ":" + panelId;

            VBox panelWrapper = new VBox();
            panelWrapper.getStyleClass().add("plugin-panel");
            panelWrapper.setSpacing(8);

            HBox header = new HBox();
            header.getStyleClass().add("plugin-panel-header");
            header.setAlignment(Pos.CENTER_LEFT);
            header.setSpacing(8);

            String headerText = (icon != null ? icon + " " : "") + title;
            Label titleLabel = new Label(headerText);
            titleLabel.getStyleClass().add("plugin-panel-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button collapseBtn = new Button("▼");
            collapseBtn.getStyleClass().add("plugin-panel-collapse-btn");
            collapseBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-color: transparent;");

            header.getChildren().addAll(titleLabel, spacer, collapseBtn);

            VBox contentWrapper = new VBox();
            contentWrapper.getStyleClass().add("plugin-panel-content");
            contentWrapper.getChildren().add(content);

            collapseBtn.setOnAction(e -> {
                boolean isCollapsed = !contentWrapper.isVisible();
                contentWrapper.setVisible(isCollapsed);
                contentWrapper.setManaged(isCollapsed);
                collapseBtn.setText(isCollapsed ? "▼" : "▶");
            });

            panelWrapper.getChildren().addAll(header, contentWrapper);
            pluginPanelsContainer.getChildren().add(panelWrapper);
            pluginPanels.put(fullPanelId, panelWrapper);
            pluginPanelIds.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(fullPanelId);

            pluginPanelsContainer.setVisible(true);
            pluginPanelsContainer.setManaged(true);

            if (fineLogger != null) {
                fineLogger.accept("Registered side panel: " + title + " for plugin " + pluginId);
            }
        });
    }

    public void removeSidePanel(String pluginId, String panelId,
            VBox pluginPanelsContainer,
            Map<String, VBox> pluginPanels,
            Map<String, List<String>> pluginPanelIds,
            Consumer<String> infoLogger) {
        Platform.runLater(() -> {
            String fullPanelId = pluginId + ":" + panelId;
            VBox panel = pluginPanels.remove(fullPanelId);
            if (panel == null || pluginPanelsContainer == null) {
                return;
            }

            pluginPanelsContainer.getChildren().remove(panel);
            List<String> ids = pluginPanelIds.get(pluginId);
            if (ids != null) {
                ids.remove(fullPanelId);
            }

            if (pluginPanelsContainer.getChildren().isEmpty()) {
                pluginPanelsContainer.setVisible(false);
                pluginPanelsContainer.setManaged(false);
            }

            if (infoLogger != null) {
                infoLogger.accept("Removed side panel: " + panelId);
            }
        });
    }

    public void removeAllSidePanels(String pluginId,
            VBox pluginPanelsContainer,
            Map<String, VBox> pluginPanels,
            Map<String, List<String>> pluginPanelIds,
            Consumer<String> infoLogger) {
        Platform.runLater(() -> {
            List<String> ids = pluginPanelIds.remove(pluginId);
            if (ids == null || pluginPanelsContainer == null) {
                return;
            }

            for (String fullPanelId : ids) {
                VBox panel = pluginPanels.remove(fullPanelId);
                if (panel != null) {
                    pluginPanelsContainer.getChildren().remove(panel);
                }
            }

            if (pluginPanelsContainer.getChildren().isEmpty()) {
                pluginPanelsContainer.setVisible(false);
                pluginPanelsContainer.setManaged(false);
            }

            if (infoLogger != null) {
                infoLogger.accept("Removed all side panels for plugin: " + pluginId);
            }
        });
    }

    public void setPluginPanelsVisible(boolean visible, VBox pluginPanelsContainer) {
        Platform.runLater(() -> {
            if (pluginPanelsContainer != null) {
                pluginPanelsContainer.setVisible(visible);
                pluginPanelsContainer.setManaged(visible);
            }
        });
    }

    public boolean isPluginPanelsVisible(VBox pluginPanelsContainer) {
        return pluginPanelsContainer != null && pluginPanelsContainer.isVisible();
    }

    public void registerStatusBarItem(String pluginId, String itemId, Node content,
            HBox pluginStatusBarContainer,
            Map<String, Node> pluginStatusBarItems,
            Map<String, List<String>> pluginStatusBarItemIds,
            Consumer<String> fineLogger,
            Consumer<String> warningLogger) {
        Platform.runLater(() -> {
            if (pluginStatusBarContainer == null) {
                if (warningLogger != null) {
                    warningLogger.accept("Status bar container not available for: " + itemId);
                }
                return;
            }

            String fullItemId = pluginId + ":" + itemId;
            HBox wrapper = new HBox(8);
            wrapper.setAlignment(Pos.CENTER_LEFT);

            Separator sep = new Separator();
            sep.setOrientation(Orientation.VERTICAL);
            sep.getStyleClass().add("status-separator");

            wrapper.getChildren().addAll(sep, content);
            pluginStatusBarContainer.getChildren().add(wrapper);
            pluginStatusBarItems.put(fullItemId, wrapper);
            pluginStatusBarItemIds.computeIfAbsent(pluginId, k -> new ArrayList<>()).add(fullItemId);

            if (fineLogger != null) {
                fineLogger.accept("Registered status bar item: " + itemId + " for plugin " + pluginId);
            }
        });
    }

    public void removeStatusBarItem(String pluginId, String itemId,
            HBox pluginStatusBarContainer,
            Map<String, Node> pluginStatusBarItems,
            Map<String, List<String>> pluginStatusBarItemIds) {
        Platform.runLater(() -> {
            String fullItemId = pluginId + ":" + itemId;
            Node item = pluginStatusBarItems.remove(fullItemId);
            if (item == null || pluginStatusBarContainer == null) {
                return;
            }

            pluginStatusBarContainer.getChildren().remove(item);
            List<String> ids = pluginStatusBarItemIds.get(pluginId);
            if (ids != null) {
                ids.remove(fullItemId);
            }
        });
    }

    public void updateStatusBarItem(String pluginId, String itemId, Node content,
            Map<String, Node> pluginStatusBarItems) {
        Platform.runLater(() -> {
            String fullItemId = pluginId + ":" + itemId;
            Node wrapper = pluginStatusBarItems.get(fullItemId);
            if (wrapper instanceof HBox) {
                HBox box = (HBox) wrapper;
                if (box.getChildren().size() > 1) {
                    box.getChildren().set(1, content);
                }
            }
        });
    }

    public void removeAllStatusBarItems(String pluginId,
            HBox pluginStatusBarContainer,
            Map<String, Node> pluginStatusBarItems,
            Map<String, List<String>> pluginStatusBarItemIds) {
        Platform.runLater(() -> {
            List<String> ids = pluginStatusBarItemIds.remove(pluginId);
            if (ids == null || pluginStatusBarContainer == null) {
                return;
            }

            for (String fullItemId : ids) {
                Node item = pluginStatusBarItems.remove(fullItemId);
                if (item != null) {
                    pluginStatusBarContainer.getChildren().remove(item);
                }
            }
        });
    }
}
