package com.example.forevernote.ui.workflow;

import java.util.function.Consumer;
import java.util.function.Function;

import com.example.forevernote.ui.controller.ToolbarController;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Encapsulates lightweight UI initialization blocks for MainController.
 */
public class UiInitializationWorkflow {

    public interface ToolbarOverflowActions {
        void focusSearch();

        void newNote();

        void newFolder();

        void newTag();

        void save();

        void delete();

        void toggleSidebar();

        void toggleNotesPanel();

        void switchLayout();
    }

    public void initializeSortOptions(ComboBox<String> sortComboBox, Function<String, String> i18n,
            Consumer<String> sorter) {
        if (sortComboBox == null || i18n == null) {
            return;
        }

        sortComboBox.getItems().addAll(
                i18n.apply("sort.title_az"),
                i18n.apply("sort.title_za"),
                i18n.apply("sort.created_newest"),
                i18n.apply("sort.created_oldest"),
                i18n.apply("sort.modified_newest"),
                i18n.apply("sort.modified_oldest"));
        sortComboBox.getSelectionModel().selectFirst();

        sortComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (sorter != null) {
                sorter.accept(newValue);
            }
        });
    }

    public void initializeViewModeButtons(
            ToggleButton editorOnlyButton,
            ToggleButton splitViewButton,
            ToggleButton previewOnlyButton,
            ToolbarController toolbarController,
            Runnable initializeGridView,
            Runnable applyViewMode) {
        ToggleGroup viewModeGroup = new ToggleGroup();
        if (editorOnlyButton != null) {
            editorOnlyButton.setToggleGroup(viewModeGroup);
        }
        if (splitViewButton != null) {
            splitViewButton.setToggleGroup(viewModeGroup);
            splitViewButton.setSelected(true);
        }
        if (previewOnlyButton != null) {
            previewOnlyButton.setToggleGroup(viewModeGroup);
        }

        ToggleGroup notesViewGroup = new ToggleGroup();
        if (toolbarController != null && toolbarController.getListViewButton() != null) {
            toolbarController.getListViewButton().setToggleGroup(notesViewGroup);
            toolbarController.getListViewButton().setSelected(true);
        }
        if (toolbarController != null && toolbarController.getGridViewButton() != null) {
            toolbarController.getGridViewButton().setToggleGroup(notesViewGroup);
        }

        if (initializeGridView != null) {
            initializeGridView.run();
        }
        if (applyViewMode != null) {
            applyViewMode.run();
        }
    }

    public void initializeRightPanelSections(HBox noteInfoHeader, VBox noteInfoContent, Label noteInfoCollapseIcon,
            VBox pluginPanelsContainer) {
        if (noteInfoHeader != null && noteInfoContent != null && noteInfoCollapseIcon != null) {
            noteInfoHeader.setOnMouseClicked(e -> {
                boolean isCollapsed = !noteInfoContent.isVisible();
                noteInfoContent.setVisible(isCollapsed);
                noteInfoContent.setManaged(isCollapsed);
                noteInfoCollapseIcon.setText(isCollapsed ? "▼" : "▶");
            });
            noteInfoHeader.setStyle("-fx-cursor: hand;");
        }

        if (pluginPanelsContainer != null) {
            pluginPanelsContainer.setVisible(true);
            pluginPanelsContainer.setManaged(true);
        }
    }

    public void setupToolbarResponsiveness(ToolbarController toolbarController, Consumer<Double> widthConsumer) {
        if (toolbarController == null || toolbarController.getToolbarHBox() == null
                || toolbarController.getToolbarOverflowBtn() == null) {
            return;
        }

        PauseTransition resizeDebounce = new PauseTransition(Duration.millis(90));
        resizeDebounce.setOnFinished(e -> {
            if (widthConsumer != null) {
                widthConsumer.accept(toolbarController.getToolbarHBox().getWidth());
            }
        });

        toolbarController.getToolbarHBox().widthProperty().addListener((obs, oldVal, newVal) -> {
            resizeDebounce.playFromStart();
        });

        Platform.runLater(() -> {
            if (widthConsumer != null) {
                widthConsumer.accept(toolbarController.getToolbarHBox().getWidth());
            }
        });
    }

    public void updateToolbarOverflow(ToolbarController toolbarController, double width, Function<String, String> i18n,
            ToolbarOverflowActions actions) {
        if (toolbarController == null || toolbarController.getToolbarHBox() == null
                || toolbarController.getToolbarOverflowBtn() == null || i18n == null || actions == null) {
            return;
        }

        int responsiveBucket = 0;
        if (width > 400) {
            responsiveBucket |= 1;
        }
        if (width > 550) {
            responsiveBucket |= 2;
        }
        if (width > 750) {
            responsiveBucket |= 4;
        }

        Object previousBucket = toolbarController.getToolbarOverflowBtn().getProperties().get("fn.responsive.bucket");
        if (previousBucket instanceof Integer previous && previous == responsiveBucket) {
            return;
        }
        toolbarController.getToolbarOverflowBtn().getProperties().put("fn.responsive.bucket", responsiveBucket);

        boolean showSearch = width > 750;
        boolean showFileActions = width > 550;
        boolean showLayoutToggles = width > 400;

        toolbarController.setResponsiveState(showSearch, showLayoutToggles, showFileActions);

        toolbarController.getSidebarToggleBtn().setVisible(showLayoutToggles);
        toolbarController.getSidebarToggleBtn().setManaged(showLayoutToggles);
        toolbarController.getNotesPanelToggleBtn().setVisible(showLayoutToggles);
        toolbarController.getNotesPanelToggleBtn().setManaged(showLayoutToggles);
        toolbarController.getSearchField().setVisible(showSearch);
        toolbarController.getSearchField().setManaged(showSearch);
        toolbarController.getLayoutSwitchBtn().setVisible(showLayoutToggles);
        toolbarController.getLayoutSwitchBtn().setManaged(showLayoutToggles);
        toolbarController.getToolbarSeparator1().setVisible(showLayoutToggles);
        toolbarController.getToolbarSeparator1().setManaged(showLayoutToggles);

        toolbarController.getToolbarOverflowBtn().getItems().clear();
        boolean needsOverflow = !showFileActions || !showSearch || !showLayoutToggles;

        if (needsOverflow) {
            if (!showSearch) {
                MenuItem searchItem = new MenuItem(i18n.apply("app.search.placeholder"));
                searchItem.setOnAction(e -> actions.focusSearch());
                toolbarController.getToolbarOverflowBtn().getItems().add(searchItem);
                toolbarController.getToolbarOverflowBtn().getItems().add(new SeparatorMenuItem());
            }
            if (!showFileActions) {
                MenuItem newNoteItem = new MenuItem(i18n.apply("action.new_note"));
                newNoteItem.setOnAction(e -> actions.newNote());
                MenuItem newFolderItem = new MenuItem(i18n.apply("action.new_folder"));
                newFolderItem.setOnAction(e -> actions.newFolder());
                MenuItem newTagItem = new MenuItem(i18n.apply("action.new_tag"));
                newTagItem.setOnAction(e -> actions.newTag());
                MenuItem saveItem = new MenuItem(i18n.apply("action.save"));
                saveItem.setOnAction(e -> actions.save());
                MenuItem deleteItem = new MenuItem(i18n.apply("action.delete"));
                deleteItem.setOnAction(e -> actions.delete());
                toolbarController.getToolbarOverflowBtn().getItems().addAll(
                        newNoteItem, newFolderItem, newTagItem, saveItem, new SeparatorMenuItem(), deleteItem);
            }
            if (!showLayoutToggles) {
                if (!toolbarController.getToolbarOverflowBtn().getItems().isEmpty()) {
                    toolbarController.getToolbarOverflowBtn().getItems().add(new SeparatorMenuItem());
                }
                MenuItem toggleSidebar = new MenuItem(i18n.apply("action.toggle_sidebar"));
                toggleSidebar.setOnAction(e -> actions.toggleSidebar());
                MenuItem toggleNotes = new MenuItem(i18n.apply("action.toggle_notes_list"));
                toggleNotes.setOnAction(e -> actions.toggleNotesPanel());
                MenuItem switchLayout = new MenuItem(i18n.apply("action.switch_layout"));
                switchLayout.setOnAction(e -> actions.switchLayout());
                toolbarController.getToolbarOverflowBtn().getItems().addAll(toggleSidebar, toggleNotes, switchLayout);
            }

            toolbarController.getToolbarOverflowBtn().setVisible(true);
            toolbarController.getToolbarOverflowBtn().setManaged(true);
        } else {
            toolbarController.getToolbarOverflowBtn().getItems().clear();
            toolbarController.getToolbarOverflowBtn().setVisible(false);
            toolbarController.getToolbarOverflowBtn().setManaged(false);
        }
    }
}
