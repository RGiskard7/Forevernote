package com.example.forevernote.ui.workflow;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.ui.controller.ToolbarController;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;

/**
 * Encapsulates navigation and refresh commands.
 */
public class NavigationCommandWorkflow {

    public boolean handleSearch(ToolbarController toolbarController, Function<String, String> i18n,
            Consumer<String> statusConsumer) {
        if (toolbarController == null || toolbarController.getSearchField() == null || i18n == null
                || statusConsumer == null) {
            return false;
        }
        toolbarController.getSearchField().requestFocus();
        toolbarController.getSearchField().selectAll();
        statusConsumer.accept(i18n.apply("status.search_focused"));
        return true;
    }

    public boolean toggleSidebar(boolean isStackedLayout, SplitPane navSplitPane, VBox sidebarPane,
            SplitPane mainSplitPane, ToolbarController toolbarController, Function<String, String> i18n,
            Consumer<String> statusConsumer) {
        if (i18n == null || statusConsumer == null) {
            return false;
        }

        if (isStackedLayout) {
            if (navSplitPane == null) {
                return false;
            }
            boolean isCollapsed = navSplitPane.getMaxWidth() < 10;
            if (isCollapsed) {
                navSplitPane.setMinWidth(200);
                navSplitPane.setMaxWidth(Double.MAX_VALUE);
                navSplitPane.setPrefWidth(300);
                if (mainSplitPane != null) {
                    mainSplitPane.setDividerPositions(0.25);
                }
                statusConsumer.accept(i18n.apply("status.nav_shown"));
                setSidebarToggle(toolbarController, true);
            } else {
                navSplitPane.setMinWidth(0);
                navSplitPane.setMaxWidth(0);
                navSplitPane.setPrefWidth(0);
                statusConsumer.accept(i18n.apply("status.nav_hidden"));
                setSidebarToggle(toolbarController, false);
            }
            return true;
        }

        if (sidebarPane == null) {
            return false;
        }
        boolean isCollapsed = sidebarPane.getMaxWidth() < 10;
        if (isCollapsed) {
            sidebarPane.setMinWidth(200);
            sidebarPane.setMaxWidth(Double.MAX_VALUE);
            sidebarPane.setPrefWidth(250);
            if (mainSplitPane != null) {
                mainSplitPane.setDividerPositions(0.22);
            }
            statusConsumer.accept(i18n.apply("status.sidebar_shown"));
            setSidebarToggle(toolbarController, true);
        } else {
            sidebarPane.setMinWidth(0);
            sidebarPane.setMaxWidth(0);
            sidebarPane.setPrefWidth(0);
            statusConsumer.accept(i18n.apply("status.sidebar_hidden"));
            setSidebarToggle(toolbarController, false);
        }
        return true;
    }

    public boolean toggleNotesPanel(boolean isStackedLayout, VBox notesPanel, SplitPane contentSplitPane,
            ToolbarController toolbarController, Runnable sidebarFallback,
            Function<String, String> i18n, Consumer<String> statusConsumer) {
        if (isStackedLayout) {
            if (sidebarFallback != null) {
                sidebarFallback.run();
                return true;
            }
            return false;
        }

        if (notesPanel == null || i18n == null || statusConsumer == null) {
            return false;
        }

        boolean isCollapsed = notesPanel.getMaxWidth() < 10;
        if (isCollapsed) {
            notesPanel.setMinWidth(180);
            notesPanel.setMaxWidth(Double.MAX_VALUE);
            notesPanel.setPrefWidth(280);
            if (contentSplitPane != null) {
                contentSplitPane.setDividerPositions(0.25);
            }
            statusConsumer.accept(i18n.apply("status.notes_panel_shown"));
            setNotesToggle(toolbarController, true);
        } else {
            notesPanel.setMinWidth(0);
            notesPanel.setMaxWidth(0);
            notesPanel.setPrefWidth(0);
            statusConsumer.accept(i18n.apply("status.notes_panel_hidden"));
            setNotesToggle(toolbarController, false);
        }

        return true;
    }

    public double zoomIn(double currentUiFontSize) {
        return currentUiFontSize + 1.0;
    }

    public double zoomOut(double currentUiFontSize) {
        if (currentUiFontSize > 8.0) {
            return currentUiFontSize - 1.0;
        }
        return currentUiFontSize;
    }

    public double resetUiZoom() {
        return 13.0;
    }

    public void refreshByContext(String currentFilterType, Folder currentFolder, Tag currentTag,
            NoteService noteService, ListView<Note> notesListView, ComboBox<String> sortComboBox, Label noteCountLabel,
            Runnable refreshNotesListAction, Consumer<Folder> folderSelectionAction,
            Consumer<String> loadNotesForTagAction,
            Function<String, String> searchTextProvider, Consumer<String> performSearchAction,
            Function<String, String> i18n, Consumer<String> statusConsumer, Consumer<Exception> errorConsumer) {
        try {
            switch (currentFilterType) {
                case "folder":
                    if (currentFolder != null && folderSelectionAction != null) {
                        folderSelectionAction.accept(currentFolder);
                    } else if (refreshNotesListAction != null) {
                        refreshNotesListAction.run();
                    }
                    break;
                case "tag":
                    if (currentTag != null && loadNotesForTagAction != null) {
                        loadNotesForTagAction.accept(currentTag.getTitle());
                    } else if (refreshNotesListAction != null) {
                        refreshNotesListAction.run();
                    }
                    break;
                case "favorites":
                    List<Note> allNotes = noteService.getAllNotes();
                    List<Note> favoriteNotes = allNotes.stream().filter(Note::isFavorite).toList();
                    notesListView.getSelectionModel().clearSelection();
                    notesListView.getItems().setAll(favoriteNotes);
                    if (sortComboBox != null && sortComboBox.getValue() != null) {
                        // caller can still apply sorting later through existing listeners
                    }
                    if (noteCountLabel != null && i18n != null) {
                        noteCountLabel.setText(java.text.MessageFormat.format(i18n.apply("info.favorite_notes_count"),
                                favoriteNotes.size()));
                    }
                    if (statusConsumer != null && i18n != null) {
                        statusConsumer.accept(i18n.apply("status.favs_refreshed"));
                    }
                    break;
                case "search":
                    String searchText = searchTextProvider != null ? searchTextProvider.apply(currentFilterType) : "";
                    if (searchText != null && !searchText.trim().isEmpty() && performSearchAction != null) {
                        performSearchAction.accept(searchText);
                    } else if (refreshNotesListAction != null) {
                        refreshNotesListAction.run();
                    }
                    break;
                default:
                    if (refreshNotesListAction != null) {
                        refreshNotesListAction.run();
                    }
                    break;
            }
        } catch (Exception e) {
            if (errorConsumer != null) {
                errorConsumer.accept(e);
            }
            if (statusConsumer != null && i18n != null) {
                statusConsumer.accept(i18n.apply("status.refresh_error"));
            }
        }
    }

    public boolean switchLayout(boolean currentStackedLayout, SplitPane mainSplitPane, SplitPane contentSplitPane,
            SplitPane navSplitPane, VBox sidebarPane, VBox notesPanel, VBox editorContainer,
            ToolbarController toolbarController, Function<String, String> i18n, Consumer<String> statusConsumer) {
        if (mainSplitPane == null || contentSplitPane == null || navSplitPane == null || sidebarPane == null
                || notesPanel == null
                || editorContainer == null || i18n == null || statusConsumer == null) {
            return currentStackedLayout;
        }

        boolean isStackedLayout = !currentStackedLayout;

        mainSplitPane.getItems().clear();
        contentSplitPane.getItems().clear();
        navSplitPane.getItems().clear();

        sidebarPane.setMinWidth(200);
        sidebarPane.setMaxWidth(Double.MAX_VALUE);
        notesPanel.setMinWidth(180);
        notesPanel.setMaxWidth(Double.MAX_VALUE);

        if (isStackedLayout) {
            navSplitPane.getItems().addAll(sidebarPane, notesPanel);
            navSplitPane.setDividerPositions(0.5);

            mainSplitPane.getItems().addAll(navSplitPane, editorContainer);
            mainSplitPane.setDividerPositions(0.25);
            statusConsumer.accept(i18n.apply("status.layout_stacked"));
        } else {
            contentSplitPane.getItems().addAll(notesPanel, editorContainer);
            contentSplitPane.setDividerPositions(0.3);

            mainSplitPane.getItems().addAll(sidebarPane, contentSplitPane);
            mainSplitPane.setDividerPositions(0.22);
            statusConsumer.accept(i18n.apply("status.layout_column"));
        }

        if (toolbarController != null && toolbarController.getSidebarToggleBtn() != null) {
            toolbarController.getSidebarToggleBtn().setSelected(true);
        }
        if (toolbarController != null && toolbarController.getNotesPanelToggleBtn() != null) {
            toolbarController.getNotesPanelToggleBtn().setSelected(true);
        }

        return isStackedLayout;
    }

    private void setSidebarToggle(ToolbarController toolbarController, boolean selected) {
        if (toolbarController != null && toolbarController.getSidebarToggleBtn() != null) {
            toolbarController.getSidebarToggleBtn().setSelected(selected);
        }
    }

    private void setNotesToggle(ToolbarController toolbarController, boolean selected) {
        if (toolbarController != null && toolbarController.getNotesPanelToggleBtn() != null) {
            toolbarController.getNotesPanelToggleBtn().setSelected(selected);
        }
    }
}
