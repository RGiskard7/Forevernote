package com.example.forevernote.ui.workflow;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

/**
 * Orchestrates note-related UI workflows without holding controller state.
 */
public class NoteWorkflow {
    private static final Logger logger = LoggerConfig.getLogger(NoteWorkflow.class);

    public interface NotesListPort {
        void loadAllNotes();

        void loadNotesForFolder(Folder folder);

        void loadNotesForTag(String tagTitle);

        void performSearch(String query);
    }

    public interface SidebarPort {
        void loadFavorites();
    }

    private final NoteDAO noteDAO;

    public NoteWorkflow(NoteDAO noteDAO) {
        this.noteDAO = noteDAO;
    }

    public void loadNoteTags(
            Note note,
            FlowPane tagsFlowPane,
            Runnable onAddTagRequested,
            Consumer<Tag> onTagRemoveRequested) {
        if (tagsFlowPane == null) {
            return;
        }

        if (note == null || note.getId() == null || note.getId().isEmpty()) {
            tagsFlowPane.getChildren().clear();
            return;
        }

        try {
            List<Tag> tags = noteDAO.fetchTags(note.getId());
            tagsFlowPane.getChildren().clear();

            for (Tag tag : tags) {
                HBox tagContainer = new HBox(4);
                tagContainer.getStyleClass().add("tag-container");
                tagContainer.setAlignment(Pos.CENTER_LEFT);

                Label tagLabel = new Label(tag.getTitle());
                tagLabel.getStyleClass().add("tag-label");

                Button removeBtn = new Button("×");
                removeBtn.getStyleClass().add("tag-remove-btn");
                removeBtn.setTooltip(new Tooltip("Remove tag from note"));

                final String tagId = tag.getId();
                final String tagTitle = tag.getTitle();
                removeBtn.setOnAction(e -> onTagRemoveRequested.accept(new Tag(tagId, tagTitle, null, null)));

                tagLabel.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        onTagRemoveRequested.accept(new Tag(tagId, tagTitle, null, null));
                    }
                });
                tagLabel.setTooltip(new Tooltip("Double-click to remove"));

                tagContainer.getChildren().addAll(tagLabel, removeBtn);
                tagsFlowPane.getChildren().add(tagContainer);
            }

            Button addTagButton = new Button("+ Add Tag");
            addTagButton.getStyleClass().add("add-tag-button");
            addTagButton.setOnAction(e -> onAddTagRequested.run());
            tagsFlowPane.getChildren().add(addTagButton);
        } catch (Exception e) {
            logger.warning("Failed to load tags for note " + note.getId() + ": " + e.getMessage());
        }
    }

    public void updateNoteMetadata(
            Note note,
            Label modifiedDateLabel,
            Label infoCreatedLabel,
            Label infoModifiedLabel,
            Label infoWordsLabel,
            Label infoCharsLabel,
            Label infoLatitudeLabel,
            Label infoLongitudeLabel,
            Label infoAuthorLabel,
            Label infoSourceUrlLabel,
            Function<String, String> i18n) {
        if (note == null) {
            if (modifiedDateLabel != null) {
                modifiedDateLabel.setText("");
            }
            if (infoCreatedLabel != null) {
                infoCreatedLabel.setText("-");
            }
            if (infoModifiedLabel != null) {
                infoModifiedLabel.setText("-");
            }
            if (infoWordsLabel != null) {
                infoWordsLabel.setText("0");
            }
            if (infoCharsLabel != null) {
                infoCharsLabel.setText("0");
            }
            if (infoLatitudeLabel != null) {
                infoLatitudeLabel.setText(MessageFormat.format(i18n.apply("info.lat"), "-"));
            }
            if (infoLongitudeLabel != null) {
                infoLongitudeLabel.setText(MessageFormat.format(i18n.apply("info.lon"), "-"));
            }
            if (infoAuthorLabel != null) {
                infoAuthorLabel.setText(MessageFormat.format(i18n.apply("info.author"), "-"));
            }
            if (infoSourceUrlLabel != null) {
                infoSourceUrlLabel.setText(MessageFormat.format(i18n.apply("info.source"), "-"));
            }
            return;
        }

        if (modifiedDateLabel != null) {
            String modifiedText = note.getModifiedDate() != null ? "Modified " + note.getModifiedDate() : "";
            modifiedDateLabel.setText(modifiedText);
        }
        if (infoCreatedLabel != null) {
            infoCreatedLabel.setText(note.getCreatedDate() != null ? note.getCreatedDate() : "-");
        }
        if (infoModifiedLabel != null) {
            infoModifiedLabel.setText(note.getModifiedDate() != null ? note.getModifiedDate() : "-");
        }

        String content = note.getContent() != null ? note.getContent() : "";
        if (infoWordsLabel != null) {
            infoWordsLabel.setText(String.valueOf(countWords(content)));
        }
        if (infoCharsLabel != null) {
            infoCharsLabel.setText(String.valueOf(content.length()));
        }
        if (infoLatitudeLabel != null) {
            String latVal = note.getLatitude() != 0 ? String.valueOf(note.getLatitude()) : "-";
            infoLatitudeLabel.setText(MessageFormat.format(i18n.apply("info.lat"), latVal));
        }
        if (infoLongitudeLabel != null) {
            String lonVal = note.getLongitude() != 0 ? String.valueOf(note.getLongitude()) : "-";
            infoLongitudeLabel.setText(MessageFormat.format(i18n.apply("info.lon"), lonVal));
        }
        if (infoAuthorLabel != null) {
            String authorVal = (note.getAuthor() != null && !note.getAuthor().isEmpty()) ? note.getAuthor() : "-";
            infoAuthorLabel.setText(MessageFormat.format(i18n.apply("info.author"), authorVal));
        }
        if (infoSourceUrlLabel != null) {
            String sourceVal = (note.getSourceUrl() != null && !note.getSourceUrl().isEmpty()) ? note.getSourceUrl() : "-";
            infoSourceUrlLabel.setText(MessageFormat.format(i18n.apply("info.source"), sourceVal));
        }
    }

    public void refreshNotesList(
            String currentFilterType,
            Folder currentFolder,
            Tag currentTag,
            String searchText,
            boolean gridMode,
            NotesListPort notesListPort,
            SidebarPort sidebarPort,
            Runnable refreshGridView) {
        if (notesListPort == null) {
            return;
        }

        switch (currentFilterType) {
            case "folder":
                if (currentFolder != null) {
                    notesListPort.loadNotesForFolder(currentFolder);
                } else {
                    notesListPort.loadAllNotes();
                }
                break;
            case "tag":
                if (currentTag != null && currentTag.getTitle() != null) {
                    notesListPort.loadNotesForTag(currentTag.getTitle());
                } else {
                    notesListPort.loadAllNotes();
                }
                break;
            case "favorites":
                if (sidebarPort != null) {
                    sidebarPort.loadFavorites();
                }
                break;
            case "search":
                notesListPort.performSearch(searchText != null ? searchText : "");
                break;
            case "all":
            default:
                notesListPort.loadAllNotes();
                break;
        }

        if (gridMode && refreshGridView != null) {
            refreshGridView.run();
        }
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
