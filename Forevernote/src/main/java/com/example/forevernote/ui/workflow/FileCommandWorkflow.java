package com.example.forevernote.ui.workflow;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.ui.controller.MainController;

import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;

/**
 * Encapsulates file-level commands (new/save/import/export/delete).
 */
public class FileCommandWorkflow {

    public interface NoteCreationUiPort {
        void onCreated(Note note);

        void onAfterCreate();

        void onError(String statusKey);
    }

    public interface FolderCreationUiPort {
        void refreshFolders();

        void refreshTree();

        void onStatus(String message);
    }

    public interface ImportUiPort {
        void refreshAfterImport();

        void onStatus(String message);

        void showInfo(String title, String header, String content);
    }

    public interface SaveUiPort {
        void refreshAfterSave();
    }

    public interface ExportUiPort {
        void onStatus(String message);

        void showWarning(String title, String header, String content);

        void showInfo(String title, String header, String content);

        void showError(String title, String header, String content);
    }

    public boolean handleNewNote(NoteWorkflow noteWorkflow, NoteService noteService, FolderService folderService,
            Folder currentFolder, Function<String, String> i18n, NoteCreationUiPort uiPort) {
        if (noteWorkflow == null || noteService == null || folderService == null || i18n == null || uiPort == null) {
            return false;
        }

        Preferences prefs = Preferences.userNodeForPackage(MainController.class);
        boolean isFileSystem = !"sqlite".equals(prefs.get("storage_type", "sqlite"));

        NoteWorkflow.NoteCreationResult creation = noteWorkflow.createNewNote(
                i18n.apply("action.new_note"),
                currentFolder,
                isFileSystem,
                new NoteWorkflow.NoteCreationPort() {
                    @Override
                    public Note createNote(Note note) {
                        return noteService.createNote(note);
                    }

                    @Override
                    public void addNoteToFolder(Folder folder, Note note) {
                        folderService.addNoteToFolder(folder, note);
                    }
                });

        if (!creation.success() || creation.note() == null) {
            uiPort.onError("status.error_creating_note");
            return false;
        }

        uiPort.onCreated(creation.note());
        uiPort.onAfterCreate();
        return true;
    }

    public boolean handleNewFolder(FolderDAO folderDAO, FolderWorkflow folderWorkflow, Folder currentFolder,
            Function<String, String> i18n, FolderCreationUiPort uiPort) {
        if (folderDAO == null || folderWorkflow == null || i18n == null || uiPort == null) {
            return false;
        }

        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle(i18n.apply("dialog.new_folder.title"));

        boolean createInRoot = (currentFolder == null || "ALL_NOTES_VIRTUAL".equals(currentFolder.getId()));
        String headerText = createInRoot
                ? i18n.apply("dialog.new_folder.header_root")
                : java.text.MessageFormat.format(i18n.apply("dialog.new_folder.header_sub"), currentFolder.getTitle());

        dialog.setHeaderText(headerText);
        dialog.setContentText(i18n.apply("dialog.new_folder.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            return false;
        }

        FolderWorkflow.FolderCreationResult creation = folderWorkflow.createFolder(
                folderDAO,
                result.get().trim(),
                currentFolder,
                createInRoot);

        if (!creation.success() || creation.folder() == null) {
            if (creation.errorMessage() != null && !creation.errorMessage().isBlank()) {
                uiPort.onStatus(
                        java.text.MessageFormat.format(i18n.apply("status.error_details"), creation.errorMessage()));
            } else {
                uiPort.onStatus(i18n.apply("status.error_creating_folder"));
            }
            return false;
        }

        uiPort.refreshFolders();
        uiPort.refreshTree();
        uiPort.onStatus(
                java.text.MessageFormat.format(i18n.apply("status.folder_created"), creation.folder().getTitle()));
        return true;
    }

    public boolean handleNewSubfolder(FolderDAO folderDAO, FolderWorkflow folderWorkflow, Folder currentFolder,
            Function<String, String> i18n, FolderCreationUiPort uiPort, Runnable fallbackToNewFolder) {
        if (currentFolder == null || "ALL_NOTES_VIRTUAL".equals(currentFolder.getId())) {
            if (fallbackToNewFolder != null) {
                fallbackToNewFolder.run();
                return true;
            }
            return false;
        }
        if (folderDAO == null || folderWorkflow == null || i18n == null || uiPort == null) {
            return false;
        }

        TextInputDialog dialog = new TextInputDialog(i18n.apply("dialog.new_subfolder.default_name"));
        dialog.setTitle(i18n.apply("dialog.new_subfolder.title"));
        dialog.setHeaderText(java.text.MessageFormat.format(i18n.apply("dialog.new_subfolder.header"),
                currentFolder.getTitle()));
        dialog.setContentText(i18n.apply("dialog.new_subfolder.content"));

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) {
            return false;
        }

        FolderWorkflow.FolderCreationResult creation = folderWorkflow.createSubfolder(folderDAO, result.get().trim(),
                currentFolder);
        if (!creation.success() || creation.folder() == null) {
            uiPort.onStatus(i18n.apply("status.subfolder_error"));
            return false;
        }

        uiPort.refreshFolders();
        uiPort.refreshTree();
        uiPort.onStatus(
                java.text.MessageFormat.format(i18n.apply("status.subfolder_created"), creation.folder().getTitle()));
        return true;
    }

    public boolean handleImport(FileChooser fileChooser, Supplier<javafx.stage.Window> windowSupplier,
            DocumentIOWorkflow documentIOWorkflow, NoteService noteService, FolderService folderService,
            Folder currentFolder, Function<String, String> i18n, ImportUiPort uiPort) {
        if (fileChooser == null || windowSupplier == null || documentIOWorkflow == null || noteService == null
                || folderService == null || i18n == null || uiPort == null) {
            return false;
        }

        fileChooser.setTitle(i18n.apply("dialog.import.title"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(i18n.apply("file_filter.supported"), "*.md", "*.txt", "*.markdown"),
                new FileChooser.ExtensionFilter(i18n.apply("file_filter.markdown"), "*.md", "*.markdown"),
                new FileChooser.ExtensionFilter(i18n.apply("file_filter.text"), "*.txt"),
                new FileChooser.ExtensionFilter(i18n.apply("file_filter.all"), "*.*"));

        List<File> files = fileChooser.showOpenMultipleDialog(windowSupplier.get());
        if (files == null || files.isEmpty()) {
            return false;
        }

        Preferences prefs = Preferences.userNodeForPackage(MainController.class);
        boolean isFileSystem = !"sqlite".equals(prefs.get("storage_type", "sqlite"));

        DocumentIOWorkflow.ImportResult importResult = documentIOWorkflow.importFiles(
                files,
                currentFolder,
                isFileSystem,
                new DocumentIOWorkflow.ImportPort() {
                    @Override
                    public Note createNote(Note note) {
                        return noteService.createNote(note);
                    }

                    @Override
                    public void addNoteToFolder(Folder folder, Note note) {
                        folderService.addNoteToFolder(folder, note);
                    }
                });

        uiPort.refreshAfterImport();

        String message = java.text.MessageFormat.format(i18n.apply("status.imported_notes"),
                importResult.importedCount());
        if (importResult.failedCount() > 0) {
            message += "\n" + java.text.MessageFormat.format(i18n.apply("status.import_failed_count"),
                    importResult.failedCount());
        }
        uiPort.onStatus(message);
        uiPort.showInfo(i18n.apply("status.import_complete"), i18n.apply("dialog.import_finished"), message);
        return true;
    }

    public void handleSave(Consumer<Void> saveAction, SaveUiPort uiPort) {
        if (saveAction != null) {
            saveAction.accept(null);
        }
        if (uiPort != null) {
            uiPort.refreshAfterSave();
        }
    }

    public void handleDelete(Consumer<Void> deleteAction) {
        if (deleteAction != null) {
            deleteAction.accept(null);
        }
    }

    public boolean handleExport(FileChooser fileChooser, Supplier<javafx.stage.Window> windowSupplier,
            Supplier<Note> currentNoteSupplier, DocumentIOWorkflow documentIOWorkflow,
            Function<String, String> i18n, ExportUiPort uiPort) {
        if (fileChooser == null || windowSupplier == null || currentNoteSupplier == null || documentIOWorkflow == null
                || i18n == null || uiPort == null) {
            return false;
        }

        Note currentNote = currentNoteSupplier.get();
        if (currentNote == null) {
            uiPort.showWarning(i18n.apply("dialog.export.title"), i18n.apply("dialog.export.no_note_header"),
                    i18n.apply("dialog.export.no_note_content"));
            return false;
        }

        fileChooser.setTitle(i18n.apply("dialog.export.save_title"));
        fileChooser.setInitialFileName(documentIOWorkflow.sanitizeFileName(currentNote.getTitle()));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(i18n.apply("file_filter.markdown"), "*.md"),
                new FileChooser.ExtensionFilter(i18n.apply("file_filter.text"), "*.txt"),
                new FileChooser.ExtensionFilter(i18n.apply("file_filter.all"), "*.*"));

        File file = fileChooser.showSaveDialog(windowSupplier.get());
        if (file == null) {
            return false;
        }

        DocumentIOWorkflow.ExportResult exportResult = documentIOWorkflow.exportNote(currentNote, file);
        if (exportResult.success()) {
            uiPort.onStatus(java.text.MessageFormat.format(i18n.apply("status.exported"), file.getName()));
            uiPort.showInfo(i18n.apply("status.export_success"), i18n.apply("dialog.export.success_header"),
                    java.text.MessageFormat.format(i18n.apply("dialog.export.saved_to"), file.getAbsolutePath()));
            return true;
        }

        String errorMessage = exportResult.errorMessage() == null ? "" : exportResult.errorMessage();
        uiPort.showError(i18n.apply("status.export_failed"), i18n.apply("dialog.export.failed_header"), errorMessage);
        return false;
    }

    public void handleSaveAll(Runnable saveAction, Function<String, String> i18n, Consumer<String> statusConsumer) {
        if (saveAction != null) {
            saveAction.run();
        }
        if (i18n != null && statusConsumer != null) {
            statusConsumer.accept(i18n.apply("status.saved_all"));
        }
    }
}
