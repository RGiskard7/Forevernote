package com.example.forevernote.ui.workflow;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.ui.controller.EditorController;
import com.example.forevernote.ui.controller.NotesListController;
import com.example.forevernote.ui.controller.SidebarController;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import javafx.scene.layout.FlowPane;
import javafx.scene.web.WebView;

/**
 * Encapsulates concrete UI event handling logic to keep MainController lean.
 */
public class UiEventHandlerWorkflow {

    public void onNotesLoaded(NoteEvents.NotesLoadedEvent event, Label noteCountLabel, boolean isGridMode,
            Runnable refreshGridViewAction) {
        if (noteCountLabel != null) {
            noteCountLabel.setText(event.getStatusMessage());
        }
        if (isGridMode && refreshGridViewAction != null) {
            refreshGridViewAction.run();
        }
    }

    public void onNoteDeleted(String noteId, Supplier<Note> currentNoteSupplier, EditorController editorController,
            FlowPane tagsFlowPane, WebView previewWebView, Runnable refreshNotesListAction,
            SidebarController sidebarController) {
        Note current = currentNoteSupplier != null ? currentNoteSupplier.get() : null;
        if (current != null && current.getId().equals(noteId)) {
            if (editorController != null) {
                editorController.loadNote(null);
            }
            if (tagsFlowPane != null) {
                tagsFlowPane.getChildren().clear();
            }
            if (previewWebView != null) {
                previewWebView.getEngine().loadContent("", "text/html");
            }
        }
        if (refreshNotesListAction != null) {
            refreshNotesListAction.run();
        }
        if (sidebarController != null) {
            sidebarController.loadTrashTree();
            sidebarController.loadRecentNotes();
            sidebarController.loadFavorites();
        }
    }

    public Folder onFolderDeleted(String folderId, Folder currentFolder, TreeView<Folder> folderTreeView,
            SidebarController sidebarController) {
        Folder nextCurrentFolder = currentFolder;
        if (nextCurrentFolder != null && nextCurrentFolder.getId().equals(folderId)) {
            nextCurrentFolder = null;
        }
        if (folderTreeView != null) {
            folderTreeView.refresh();
        }
        if (sidebarController != null) {
            sidebarController.loadFolders();
            sidebarController.loadTrashTree();
        }
        return nextCurrentFolder;
    }

    public void onTrashItemDeleted(SidebarController sidebarController) {
        if (sidebarController != null) {
            sidebarController.loadTrashTree();
            sidebarController.loadFolders();
        }
    }

    public Folder onFolderSelected(Folder selectedFolder, NotesListController notesListController,
            Consumer<Folder> folderSelectionAction) {
        if (selectedFolder != null) {
            String id = selectedFolder.getId();
            if (id == null && "INVISIBLE_ROOT".equals(selectedFolder.getTitle())) {
                return null;
            }

            if ("ALL_NOTES_VIRTUAL".equals(id)) {
                if (notesListController != null) {
                    notesListController.loadAllNotes();
                }
                return null;
            }
            if (folderSelectionAction != null) {
                folderSelectionAction.accept(selectedFolder);
            }
            return selectedFolder;
        }
        return null;
    }

    public TagWorkflow onTagSelected(Tag tag, TagWorkflow currentTagWorkflow, Consumer<Folder> setCurrentFolder,
            Consumer<Tag> setCurrentTag, Consumer<String> setCurrentFilterType, NotesListController notesListController) {
        TagWorkflow effectiveTagWorkflow = currentTagWorkflow != null ? currentTagWorkflow : new TagWorkflow();
        effectiveTagWorkflow.selectTag(tag, new TagWorkflow.TagSelectionPort() {
            @Override
            public void setCurrentFolderToNull() {
                if (setCurrentFolder != null) {
                    setCurrentFolder.accept(null);
                }
            }

            @Override
            public void setCurrentTag(Tag selectedTag) {
                if (setCurrentTag != null) {
                    setCurrentTag.accept(selectedTag);
                }
            }

            @Override
            public void setCurrentFilterType(String filterType) {
                if (setCurrentFilterType != null) {
                    setCurrentFilterType.accept(filterType);
                }
            }

            @Override
            public void loadNotesForTag(String tagTitle) {
                if (notesListController != null) {
                    notesListController.loadNotesForTag(tagTitle);
                }
            }
        });
        return effectiveTagWorkflow;
    }

    public Note resolveNoteToOpen(Note requestedNote, Supplier<List<Note>> allNotesSupplier) {
        if (requestedNote == null) {
            return null;
        }
        if (requestedNote.getId() != null) {
            return requestedNote;
        }
        if (requestedNote.getTitle() == null || allNotesSupplier == null) {
            return requestedNote;
        }
        Optional<Note> fullNote = allNotesSupplier.get().stream()
                .filter(n -> requestedNote.getTitle().equals(n.getTitle()))
                .findFirst();
        return fullNote.orElse(requestedNote);
    }

    public void onNoteOpenRequest(Note noteToOpen, Consumer<Note> openInEditorAction, ListView<Note> notesListView) {
        if (noteToOpen == null) {
            return;
        }
        if (openInEditorAction != null) {
            openInEditorAction.accept(noteToOpen);
        }
        if (notesListView != null) {
            notesListView.getSelectionModel().select(noteToOpen);
        }
    }

    public void onTrashItemSelected(Component component, Consumer<Note> openNoteAction,
            Consumer<Folder> folderSelectionAction) {
        if (component instanceof Note && openNoteAction != null) {
            openNoteAction.accept((Note) component);
        } else if (component instanceof Folder && folderSelectionAction != null) {
            folderSelectionAction.accept((Folder) component);
        }
    }
}
