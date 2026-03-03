package com.example.forevernote.ui.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.kordamp.ikonli.javafx.FontIcon;

import com.example.forevernote.data.models.Note;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Encapsulates notes list/grid view behavior and note-card rendering.
 */
public class NotesGridWorkflow {
    private String lastGridSignature = "";

    public VBox applyNotesViewMode(
            boolean isGridMode,
            ListView<Note> notesListView,
            ScrollPane gridScrollPane,
            VBox currentNotesPanelContainer,
            Runnable refreshGridViewAction,
            Consumer<String> warningLogger) {
        if (notesListView == null || gridScrollPane == null) {
            return currentNotesPanelContainer;
        }

        VBox notesPanelContainer = currentNotesPanelContainer;
        if (notesPanelContainer == null) {
            Parent parent = notesListView.getParent();
            if (parent instanceof VBox) {
                notesPanelContainer = (VBox) parent;
            } else {
                parent = gridScrollPane.getParent();
                if (parent instanceof VBox) {
                    notesPanelContainer = (VBox) parent;
                }
            }
        }

        if (notesPanelContainer == null) {
            if (warningLogger != null) {
                warningLogger.accept("Could not find notes panel container");
            }
            return null;
        }

        VBox resolvedContainer = notesPanelContainer;
        Runnable applyViewSwitch = () -> {
            if (isGridMode) {
                if (resolvedContainer.getChildren().contains(notesListView)) {
                    resolvedContainer.getChildren().remove(notesListView);
                }
                if (!resolvedContainer.getChildren().contains(gridScrollPane)) {
                    resolvedContainer.getChildren().add(gridScrollPane);
                    VBox.setVgrow(gridScrollPane, Priority.ALWAYS);
                }
                if (refreshGridViewAction != null) {
                    refreshGridViewAction.run();
                }
            } else {
                if (resolvedContainer.getChildren().contains(gridScrollPane)) {
                    resolvedContainer.getChildren().remove(gridScrollPane);
                }
                if (!resolvedContainer.getChildren().contains(notesListView)) {
                    resolvedContainer.getChildren().add(notesListView);
                    VBox.setVgrow(notesListView, Priority.ALWAYS);
                }
            }
        };

        if (Platform.isFxApplicationThread()) {
            applyViewSwitch.run();
        } else {
            Platform.runLater(applyViewSwitch);
        }

        return notesPanelContainer;
    }

    public void refreshGridView(
            TilePane notesGridPane,
            ListView<Note> notesListView,
            boolean isDarkTheme,
            Function<String, String> i18n,
            Consumer<Note> openNoteAction,
            Consumer<String> statusUpdate) {
        if (notesGridPane == null || notesListView == null || i18n == null || openNoteAction == null
                || statusUpdate == null) {
            return;
        }

        List<Note> notes = new ArrayList<>(notesListView.getItems());
        StringBuilder signatureBuilder = new StringBuilder(notes.size() * 24);
        signatureBuilder.append(isDarkTheme ? "dark|" : "light|");
        for (Note note : notes) {
            signatureBuilder.append(note.getId()).append('|')
                    .append(note.getModifiedDate() != null ? note.getModifiedDate() : "")
                    .append('|').append(note.isPinned() ? '1' : '0')
                    .append(note.isFavorite() ? '1' : '0').append(';');
        }
        String signature = signatureBuilder.toString();
        if (signature.equals(lastGridSignature) && notesGridPane.getChildren().size() == notes.size()) {
            return;
        }

        notesGridPane.getChildren().clear();
        for (Note note : notes) {
            VBox card = createNoteCard(note, notesListView, isDarkTheme, i18n, openNoteAction, statusUpdate);
            notesGridPane.getChildren().add(card);
        }
        lastGridSignature = signature;
    }

    private VBox createNoteCard(
            Note note,
            ListView<Note> notesListView,
            boolean isDarkTheme,
            Function<String, String> i18n,
            Consumer<Note> openNoteAction,
            Consumer<String> statusUpdate) {
        VBox card = new VBox(8);
        card.setPrefWidth(180);
        card.setPrefHeight(140);
        card.setPadding(new javafx.geometry.Insets(12));
        card.getStyleClass().add("note-card");
        card.setCursor(javafx.scene.Cursor.HAND);

        HBox titleRow = new HBox(5);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        if (note.isPinned()) {
            FontIcon pinIcon = new FontIcon("fth-map-pin");
            pinIcon.getStyleClass().add("feather-pin-active");
            pinIcon.setIconSize(12);
            titleRow.getChildren().add(pinIcon);
        }

        if (note.isFavorite()) {
            FontIcon favIcon = new FontIcon("fth-star");
            favIcon.getStyleClass().add("feather-favorite-active");
            favIcon.setIconSize(12);
            titleRow.getChildren().add(favIcon);
        }

        Label titleLabel = new Label(note.getTitle() != null ? note.getTitle() : i18n.apply("app.untitled"));
        titleLabel.getStyleClass().add("note-card-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxHeight(40);
        titleRow.getChildren().add(titleLabel);

        String preview = note.getContent() != null && !note.getContent().isEmpty()
                ? note.getContent().replaceAll("^#+\\s*", "").replaceAll("\\n", " ").trim()
                : "";
        if (preview.length() > 80) {
            preview = preview.substring(0, 77) + "...";
        }
        Label previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("note-card-preview");
        previewLabel.setWrapText(true);
        previewLabel.setMaxHeight(60);
        VBox.setVgrow(previewLabel, Priority.ALWAYS);

        String dateText = note.getModifiedDate() != null ? note.getModifiedDate() : note.getCreatedDate();
        if (dateText != null && dateText.length() > 10) {
            dateText = dateText.substring(0, 10);
        }
        Label dateLabel = new Label(dateText != null ? dateText : "");
        dateLabel.getStyleClass().add("note-card-date");

        card.getChildren().addAll(titleRow, previewLabel, dateLabel);

        card.setOnMouseClicked(e -> {
            notesListView.getSelectionModel().select(note);
            openNoteAction.accept(note);
        });

        setupNoteCardDrag(card, note, i18n, statusUpdate);
        return card;
    }

    private void setupNoteCardDrag(
            VBox card,
            Note note,
            Function<String, String> i18n,
            Consumer<String> statusUpdate) {
        card.setOnDragDetected(event -> {
            javafx.scene.input.Dragboard db = card.startDragAndDrop(javafx.scene.input.TransferMode.MOVE);
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString("note:" + note.getId());
            db.setContent(content);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            db.setDragView(card.snapshot(params, null));

            event.consume();
            statusUpdate.accept(java.text.MessageFormat.format(i18n.apply("status.dragging"), note.getTitle()));
        });

        card.setOnDragDone(event -> {
            if (event.getTransferMode() == javafx.scene.input.TransferMode.MOVE) {
                statusUpdate.accept(i18n.apply("status.note_moved"));
            }
            event.consume();
        });
    }
}
