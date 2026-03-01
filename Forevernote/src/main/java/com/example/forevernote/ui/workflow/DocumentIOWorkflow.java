package com.example.forevernote.ui.workflow;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;

/**
 * Encapsulates import/export document operations.
 */
public class DocumentIOWorkflow {
    private static final Logger logger = LoggerConfig.getLogger(DocumentIOWorkflow.class);
    private static final String ROOT_ID = "ROOT";
    private static final String ALL_NOTES_VIRTUAL_ID = "ALL_NOTES_VIRTUAL";

    public interface ImportPort {
        Note createNote(Note note);

        void addNoteToFolder(Folder folder, Note note);
    }

    public record ImportResult(int importedCount, int failedCount, List<String> failures) {
    }

    public record ExportResult(boolean success, String errorMessage) {
    }

    public ImportResult importFiles(
            List<File> files,
            Folder currentFolder,
            boolean isFileSystem,
            ImportPort importPort) {
        if (files == null || files.isEmpty()) {
            return new ImportResult(0, 0, List.of());
        }
        if (importPort == null) {
            return new ImportResult(0, files.size(), List.of("ImportPort is null"));
        }

        int imported = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath());
                String title = extractTitleFromFileName(file.getName());

                Note newNote = new Note(title, content);
                if (isFileSystem && isConcreteFolder(currentFolder)) {
                    String safeTitle = sanitizeFileName(title);
                    newNote.setId(currentFolder.getId() + File.separator + safeTitle);
                }

                Note createdNote = importPort.createNote(newNote);
                if (createdNote == null || createdNote.getId() == null || createdNote.getId().isBlank()) {
                    throw new IllegalStateException("Created note has null/blank ID");
                }
                newNote.setId(createdNote.getId());

                if (!isFileSystem && isConcreteFolder(currentFolder)) {
                    importPort.addNoteToFolder(currentFolder, createdNote);
                }
                imported++;
            } catch (Exception e) {
                failed++;
                String msg = "Failed to import file " + file.getName() + ": " + e.getMessage();
                failures.add(msg);
                logger.warning(msg);
            }
        }

        return new ImportResult(imported, failed, failures);
    }

    public ExportResult exportNote(Note note, File targetFile) {
        if (note == null) {
            return new ExportResult(false, "Note is null");
        }
        if (targetFile == null) {
            return new ExportResult(false, "Target file is null");
        }

        try (FileWriter writer = new FileWriter(targetFile)) {
            if (targetFile.getName().endsWith(".md")) {
                writer.write("# " + note.getTitle() + "\n\n");
            }
            writer.write(note.getContent() != null ? note.getContent() : "");
            return new ExportResult(true, null);
        } catch (Exception e) {
            return new ExportResult(false, e.getMessage());
        }
    }

    public String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "untitled";
        }
        String sanitized = name.replaceAll("[^a-zA-Z0-9\\-_ ]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 50));
    }

    private String extractTitleFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Untitled";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    private boolean isConcreteFolder(Folder folder) {
        return folder != null
                && folder.getId() != null
                && !folder.getId().isBlank()
                && !ROOT_ID.equals(folder.getId())
                && !ALL_NOTES_VIRTUAL_ID.equals(folder.getId());
    }
}
