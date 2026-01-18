package com.example.forevernote.ui.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.forevernote.plugin.Plugin;
import com.example.forevernote.plugin.PluginManager;
import com.example.forevernote.plugin.PluginManager.PluginState;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Plugin Manager Dialog - Obsidian-style plugin management interface.
 * 
 * <p>Provides a visual interface to:</p>
 * <ul>
 *   <li>View all installed plugins</li>
 *   <li>Enable/disable plugins</li>
 *   <li>View plugin information (version, author, description)</li>
 * </ul>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.2.0
 */
public class PluginManagerDialog {
    
    private final Stage parentStage;
    private final PluginManager pluginManager;
    private Stage dialogStage;
    private VBox pluginListContainer;
    private boolean isDarkTheme = false;
    
    // Track toggle buttons for each plugin
    private final Map<String, ToggleButton> toggleButtons = new HashMap<>();
    
    /**
     * Creates a new Plugin Manager Dialog.
     * 
     * @param parentStage   The parent stage
     * @param pluginManager The plugin manager
     */
    public PluginManagerDialog(Stage parentStage, PluginManager pluginManager) {
        this.parentStage = parentStage;
        this.pluginManager = pluginManager;
    }
    
    /**
     * Sets the theme for the dialog.
     */
    public void setDarkTheme(boolean isDark) {
        this.isDarkTheme = isDark;
    }
    
    /**
     * Shows the Plugin Manager dialog.
     */
    public void show() {
        createDialog();
        refreshPluginList();
        dialogStage.showAndWait();
    }
    
    /**
     * Creates the dialog UI.
     */
    private void createDialog() {
        dialogStage = new Stage();
        dialogStage.initOwner(parentStage);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.DECORATED);
        dialogStage.setTitle("Plugin Manager");
        dialogStage.setMinWidth(500);
        dialogStage.setMinHeight(400);
        
        // Colors matching app's dark/light theme (from CSS)
        String bg = isDarkTheme ? "#1e1e1e" : "#ffffff";
        String fg = isDarkTheme ? "#e0e0e0" : "#1e1e1e";
        String cardBg = isDarkTheme ? "#252525" : "#f5f5f5";
        String borderColor = isDarkTheme ? "#3a3a3a" : "#e0e0e0";
        String accentColor = "#7c3aed";
        String mutedColor = isDarkTheme ? "#888888" : "#666666";
        
        // Main container
        VBox mainContainer = new VBox(0);
        mainContainer.setStyle(String.format("-fx-background-color: %s;", bg));
        
        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 25, 15, 25));
        header.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: transparent transparent %s transparent; -fx-border-width: 0 0 1 0;",
            bg, borderColor
        ));
        
        Label titleLabel = new Label("Plugins");
        titleLabel.setStyle(String.format(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: %s;",
            fg
        ));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label countLabel = new Label(pluginManager.getPluginCount() + " plugins installed");
        countLabel.setStyle(String.format(
            "-fx-font-size: 13px; -fx-text-fill: %s;",
            mutedColor
        ));
        
        header.getChildren().addAll(titleLabel, spacer, countLabel);
        
        // Plugin list container
        pluginListContainer = new VBox(0);
        pluginListContainer.setPadding(new Insets(10, 15, 10, 15));
        
        ScrollPane scrollPane = new ScrollPane(pluginListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(String.format(
            "-fx-background: %s; -fx-background-color: %s; -fx-border-color: transparent;",
            bg, bg
        ));
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Footer
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15, 25, 20, 25));
        footer.setStyle(String.format(
            "-fx-background-color: %s; -fx-border-color: %s transparent transparent transparent; -fx-border-width: 1 0 0 0;",
            cardBg, borderColor
        ));
        
        Button closeButton = new Button("Close");
        closeButton.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 13px; " +
            "-fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;",
            accentColor
        ));
        closeButton.setOnAction(e -> dialogStage.close());
        
        footer.getChildren().add(closeButton);
        
        mainContainer.getChildren().addAll(header, scrollPane, footer);
        
        Scene scene = new Scene(mainContainer, 550, 500);
        dialogStage.setScene(scene);
    }
    
    /**
     * Refreshes the plugin list.
     */
    private void refreshPluginList() {
        pluginListContainer.getChildren().clear();
        toggleButtons.clear();
        
        // Create a mutable copy of the plugin list
        List<Plugin> plugins = new java.util.ArrayList<>(pluginManager.getAllPlugins());
        
        if (plugins.isEmpty()) {
            Label emptyLabel = new Label("No plugins installed");
            emptyLabel.setStyle(String.format(
                "-fx-font-size: 14px; -fx-text-fill: %s; -fx-padding: 40;",
                isDarkTheme ? "#888888" : "#666666"
            ));
            pluginListContainer.getChildren().add(emptyLabel);
            return;
        }
        
        // Sort plugins by name
        plugins.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        
        for (Plugin plugin : plugins) {
            VBox pluginCard = createPluginCard(plugin);
            pluginListContainer.getChildren().add(pluginCard);
        }
    }
    
    /**
     * Creates a card for a plugin (Obsidian-style).
     */
    private VBox createPluginCard(Plugin plugin) {
        String cardBg = isDarkTheme ? "#2d2d2d" : "#f8f8f8";
        String fg = isDarkTheme ? "#e0e0e0" : "#1e1e1e";
        String mutedColor = isDarkTheme ? "#888888" : "#666666";
        String borderColor = isDarkTheme ? "#3a3a3a" : "#e0e0e0";
        String enabledColor = "#22c55e";
        String disabledColor = isDarkTheme ? "#555555" : "#9ca3af";
        
        PluginState state = pluginManager.getPluginState(plugin.getId());
        boolean isEnabled = state == PluginState.ENABLED || state == PluginState.INITIALIZED;
        
        VBox card = new VBox(8);
        card.setPadding(new Insets(15, 18, 15, 18));
        card.setStyle(String.format(
            "-fx-background-color: %s; -fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-border-color: %s; -fx-border-width: 1;",
            cardBg, borderColor
        ));
        VBox.setMargin(card, new Insets(0, 0, 10, 0));
        
        // Header row: Name + Toggle
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(plugin.getName());
        nameLabel.setStyle(String.format(
            "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: %s;",
            fg
        ));
        
        Label versionLabel = new Label("v" + plugin.getVersion());
        versionLabel.setStyle(String.format(
            "-fx-font-size: 11px; -fx-text-fill: %s; -fx-background-color: %s; " +
            "-fx-padding: 2 8; -fx-background-radius: 10;",
            mutedColor, isDarkTheme ? "#333333" : "#e5e5e5"
        ));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Toggle button (Obsidian-style switch)
        ToggleButton toggleBtn = createToggleSwitch(isEnabled, enabledColor, disabledColor);
        toggleBtn.setSelected(isEnabled);
        toggleBtn.setOnAction(e -> togglePlugin(plugin.getId(), toggleBtn.isSelected()));
        toggleButtons.put(plugin.getId(), toggleBtn);
        
        headerRow.getChildren().addAll(nameLabel, versionLabel, spacer, toggleBtn);
        
        // Description
        Label descLabel = new Label(plugin.getDescription().isEmpty() ? "No description" : plugin.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle(String.format(
            "-fx-font-size: 13px; -fx-text-fill: %s;",
            mutedColor
        ));
        
        // Author and status row
        HBox infoRow = new HBox(15);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        
        if (!plugin.getAuthor().isEmpty()) {
            Label authorLabel = new Label("by " + plugin.getAuthor());
            authorLabel.setStyle(String.format(
                "-fx-font-size: 11px; -fx-text-fill: %s;",
                mutedColor
            ));
            infoRow.getChildren().add(authorLabel);
        }
        
        Label statusLabel = new Label(isEnabled ? "● Enabled" : "○ Disabled");
        statusLabel.setStyle(String.format(
            "-fx-font-size: 11px; -fx-text-fill: %s;",
            isEnabled ? enabledColor : disabledColor
        ));
        infoRow.getChildren().add(statusLabel);
        
        card.getChildren().addAll(headerRow, descLabel, infoRow);
        
        return card;
    }
    
    /**
     * Creates an Obsidian-style toggle switch.
     */
    private ToggleButton createToggleSwitch(boolean isOn, String enabledColor, String disabledColor) {
        ToggleButton toggle = new ToggleButton();
        toggle.setPrefWidth(44);
        toggle.setPrefHeight(24);
        toggle.setMinWidth(44);
        toggle.setMinHeight(24);
        
        updateToggleStyle(toggle, isOn, enabledColor, disabledColor);
        
        toggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            updateToggleStyle(toggle, isSelected, enabledColor, disabledColor);
        });
        
        return toggle;
    }
    
    /**
     * Updates the toggle switch style.
     */
    private void updateToggleStyle(ToggleButton toggle, boolean isOn, String enabledColor, String disabledColor) {
        String bgColor = isOn ? enabledColor : disabledColor;
        
        toggle.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 12; " +
            "-fx-border-radius: 12; " +
            "-fx-padding: 0; " +
            "-fx-cursor: hand;",
            bgColor
        ));
        
        // We use text to simulate the knob position
        toggle.setText(isOn ? "      ●" : "●      ");
        toggle.setStyle(toggle.getStyle() + 
            String.format("-fx-text-fill: white; -fx-font-size: 14px; -fx-alignment: center-%s;",
                isOn ? "right" : "left"));
    }
    
    /**
     * Toggles a plugin's enabled state.
     */
    private void togglePlugin(String pluginId, boolean enable) {
        if (enable) {
            pluginManager.enablePlugin(pluginId);
        } else {
            pluginManager.disablePlugin(pluginId);
        }
        
        // Refresh the list to update status labels
        refreshPluginList();
    }
}
