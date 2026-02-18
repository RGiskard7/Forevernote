package com.example.forevernote.data.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.exceptions.DataAccessException;
import com.example.forevernote.exceptions.InvalidParameterException;

/**
 * File System implementation of NoteDAO.
 * Stores notes as Markdown files with YAML frontmatter.
 */
public class NoteDAOFileSystem implements NoteDAO {

    private static final Logger logger = LoggerConfig.getLogger(NoteDAOFileSystem.class);
    private final Path rootPath;

    // Cache to map Note ID (Relative Path) -> Absolute Path
    private final Map<String, Path> idToPathMap = new ConcurrentHashMap<>();

    public NoteDAOFileSystem(String rootDirectory) {
        this.rootPath = Paths.get(rootDirectory);
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectories(rootPath);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create root directory for notes: " + rootDirectory, e);
                throw new DataAccessException("Could not initialize file storage", e);
            }
        }
        refreshCache();
    }

    private void refreshCache() {
        idToPathMap.clear();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .forEach(path -> {
                        String relativePath = rootPath.relativize(path).toString();
                        idToPathMap.put(relativePath, path);
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to walk directory for cache refresh", e);
        }
    }

    private Note createLightweightNote(String id, Path path) {
        // Create Note object WITHOUT reading file content
        String filename = path.getFileName().toString();
        String title = filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;

        // Note constructor: id, title, content
        // We set content to empty string or null contextually. Using empty string for
        // safety.
        Note note = new Note(id, title, "");
        // We don't know dates or preview without reading.
        return note;
    }

    @Override
    public String createNote(Note note) {
        if (note == null)
            throw new InvalidParameterException("Note cannot be null");

        // Determine parent directory
        Path parentDir = rootPath;
        String suggestedId = note.getId();

        if (suggestedId != null && !suggestedId.isEmpty()) {
            // Check if the ID implies a folder path (e.g. "Folder/Note.md" or just
            // "Folder/")
            // If the ID comes from MainController as "Folder/New Note", we want to use
            // "Folder" as parent.
            if (suggestedId.contains(File.separator)) {
                int lastSeparator = suggestedId.lastIndexOf(File.separator);
                String folderPath = suggestedId.substring(0, lastSeparator);
                Path potentialDir = rootPath.resolve(folderPath);
                if (Files.exists(potentialDir) && Files.isDirectory(potentialDir)) {
                    parentDir = potentialDir;
                }
            }
        }

        String filename = sanitizeFilename(note.getTitle()) + ".md";

        Path filePath = parentDir.resolve(filename);
        // Handle duplicate filenames
        int counter = 1;
        while (Files.exists(filePath)) {
            filename = sanitizeFilename(note.getTitle()) + " (" + counter + ").md";
            filePath = parentDir.resolve(filename);
            counter++;
        }

        if (note.getCreatedDate() == null) {
            note.setCreatedDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        }

        // Set the ID to relative path
        String relativePath = rootPath.relativize(filePath).toString();
        note.setId(relativePath);

        try {
            String fileContent = FrontmatterHandler.generate(note);
            Files.writeString(filePath, fileContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            idToPathMap.put(relativePath, filePath);
            return relativePath;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write note file: " + filePath, e);
            return null;
        }
    }

    @Override
    public Note getNoteById(String id) {
        if (id == null)
            return null;

        Path path = idToPathMap.get(id);
        if (path == null) {
            // Maybe it's a new file not in cache yet
            Path potential = rootPath.resolve(id);
            if (Files.exists(potential)) {
                path = potential;
                idToPathMap.put(id, path);
            } else {
                return null;
            }
        }

        try {
            String content = Files.readString(path);
            Note note = FrontmatterHandler.parse(content);
            // Override ID with our path-based ID
            note.setId(id);

            // Sync Title with Filename
            String filename = path.getFileName().toString();
            if (filename.endsWith(".md"))
                filename = filename.substring(0, filename.length() - 3);
            note.setTitle(filename);

            return note;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read note file: " + path, e);
            return null;
        }
    }

    @Override
    public void updateNote(Note note) {
        if (note == null || note.getId() == null)
            throw new InvalidParameterException("Invalid note");

        Path path = idToPathMap.get(note.getId());
        if (path == null) {
            // Check if file exists at ID location
            path = rootPath.resolve(note.getId());
            if (!Files.exists(path)) {
                logger.warning("Attempted to update non-existent note: " + note.getId());
                return;
            }
        }

        // Rename logic if Title changed
        String currentFilename = path.getFileName().toString();
        String expectedFilename = sanitizeFilename(note.getTitle()) + ".md";

        if (!currentFilename.equals(expectedFilename)) {
            Path newPath = path.resolveSibling(expectedFilename);
            if (!Files.exists(newPath)) {
                try {
                    Files.move(path, newPath);
                    // Update ID map
                    idToPathMap.remove(note.getId());
                    String newId = rootPath.relativize(newPath).toString();
                    idToPathMap.put(newId, newPath);
                    note.setId(newId); // Update object ID
                    path = newPath;
                } catch (IOException e) {
                    logger.warning("Failed to rename note file during update: " + e.getMessage());
                }
            }
        }

        note.setModifiedDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        try {
            String fileContent = FrontmatterHandler.generate(note);
            Files.writeString(path, fileContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to update note file: " + path, e);
        }
    }

    @Override
    public void deleteNote(String id) {
        Path path = idToPathMap.get(id);
        if (path != null && Files.exists(path)) {
            try {
                Note note = getNoteById(id);
                if (note != null) {
                    note.setDeleted(true);
                    note.setDeletedDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
                    String fileContent = FrontmatterHandler.generate(note);
                    Files.writeString(path, fileContent, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to delete (soft) note file: " + path, e);
            }
        }
    }

    @Override
    public void permanentlyDeleteNote(String id) {
        Path path = idToPathMap.get(id);
        if (path != null) {
            try {
                Files.delete(path);
                idToPathMap.remove(id);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to delete note file: " + path, e);
            }
        }
    }

    @Override
    public void restoreNote(String id) {
        Note note = getNoteById(id);
        if (note != null && note.isDeleted()) {
            note.setDeleted(false);
            note.setDeletedDate(null);
            updateNote(note);
        }
    }

    @Override
    public List<Note> fetchTrashNotes() {
        return fetchAllNotes().stream().filter(Note::isDeleted).collect(Collectors.toList());
    }

    @Override
    public List<Note> fetchNotesByFolderId(String folderId) {
        List<Note> notes = new ArrayList<>();
        Path folderPath;

        if (folderId == null || folderId.equals("ROOT") || folderId.isEmpty()) {
            folderPath = rootPath;
        } else {
            // Folder ID is the relative path, so resolve against root
            folderPath = rootPath.resolve(folderId);
        }

        if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
            try (Stream<Path> stream = Files.list(folderPath)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.endsWith(".md") && !name.startsWith(".");
                        })
                        .forEach(p -> {
                            String relativePath = rootPath.relativize(p).toString();
                            // Update cache lazily if needed, or rely on disk source of truth
                            idToPathMap.put(relativePath, p);
                            notes.add(createLightweightNote(relativePath, p));
                        });
            } catch (IOException e) {
                logger.warning("Failed to list notes in folder: " + folderPath);
            }
        }
        return notes;
    }

    @Override
    public void fetchNotesByFolderId(Folder folder) {
        List<Note> notes = fetchNotesByFolderId(folder.getId());
        // Cast to Component list for addAll
        List<com.example.forevernote.data.models.interfaces.Component> components = new ArrayList<>(notes);
        folder.addAll(components);
    }

    @Override
    public List<Note> fetchAllNotes() {
        // Return lightweight notes!
        return idToPathMap.entrySet().stream()
                .map(e -> createLightweightNote(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public Folder getFolderOfNote(String noteId) {
        return null;
    }

    @Override
    public void addTag(String noteId, String tagId) {
    }

    @Override
    public void addTag(Note note, Tag tag) {
        note.addTag(tag);
        updateNote(note);
    }

    @Override
    public void removeTag(String noteId, String tagId) {
    }

    @Override
    public void removeTag(Note note, Tag tag) {
        note.removeTag(tag);
        updateNote(note);
    }

    @Override
    public List<Tag> fetchTags(String noteId) {
        Note note = getNoteById(noteId);
        if (note != null)
            return note.getTags();
        return new ArrayList<>();
    }

    @Override
    public void loadTags(Note note) {
    }

    @Override
    public List<Note> fetchNotesByTagId(String tagId) {
        // Optimization needed: Tag Index.
        // For now: slow scan.
        List<Note> all = new ArrayList<>();
        for (String id : idToPathMap.keySet()) {
            Note n = getNoteById(id); // Reads file!
            if (n != null && n.getTags().stream().anyMatch(t -> t.getTitle().equals(tagId))) {
                all.add(n);
            }
        }
        return all;
    }

    private String sanitizeFilename(String title) {
        return title.replaceAll("[^a-zA-Z0-9\\.\\-_ ]", "_");
    }
}
