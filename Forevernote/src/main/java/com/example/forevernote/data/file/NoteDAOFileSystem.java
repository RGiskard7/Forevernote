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
    // Cache to map Note ID -> Note object (Lightweight)
    private final Map<String, Note> cachedNotes = new ConcurrentHashMap<>();

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
        cachedNotes.clear();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            // Using parallel stream for faster initial load of thousands of headers
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md") && !p.getFileName().toString().startsWith("."))
                    .parallel()
                    .forEach(path -> {
                        String relativePath = rootPath.relativize(path).toString();
                        idToPathMap.put(relativePath, path);
                        // Accessing created note immediately creates race condition if not thread safe
                        // NoteDAOFileSystem methods are synchronized or use concurrent maps
                        Note note = createLightweightNote(relativePath, path);
                        cachedNotes.put(relativePath, note);
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to walk directory for cache refresh", e);
        }
    }

    private Note createLightweightNote(String id, Path path) {
        // Create Note object
        String filename = path.getFileName().toString();
        String title = filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;
        Note note = new Note(id, title, "");

        // Lightweight Header Reading: Read only frontmatter to get metadata
        if (Files.exists(path)) {
            try (java.io.BufferedReader reader = Files.newBufferedReader(path)) {
                String line = reader.readLine();
                if (line != null && line.trim().equals("---")) {
                    // Frontmatter detected
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("---") || line.equals("..."))
                            break; // End of frontmatter

                        if (line.startsWith("favorite:")) {
                            note.setFavorite("true".equalsIgnoreCase(getValue(line)));
                        } else if (line.startsWith("pinned:")) {
                            note.setPinned("true".equalsIgnoreCase(getValue(line)));
                        } else if (line.startsWith("deleted:")) {
                            note.setDeleted("true".equalsIgnoreCase(getValue(line)));
                        } else if (line.startsWith("tags:")) {
                            String tagsVal = getValue(line);
                            // Format: tags: [tag1, tag2]
                            if (tagsVal.startsWith("[") && tagsVal.endsWith("]")) {
                                tagsVal = tagsVal.substring(1, tagsVal.length() - 1);
                                String[] parts = tagsVal.split(",");
                                for (String part : parts) {
                                    if (!part.trim().isEmpty()) {
                                        com.example.forevernote.data.models.Tag t = new com.example.forevernote.data.models.Tag(
                                                part.trim());
                                        t.setId(part.trim());
                                        note.addTag(t);
                                    }
                                }
                            } else if (!tagsVal.isEmpty()) {
                                // Maybe comma separated without brackets
                                if (tagsVal.contains(",")) {
                                    String[] parts = tagsVal.split(",");
                                    for (String part : parts) {
                                        if (!part.trim().isEmpty()) {
                                            com.example.forevernote.data.models.Tag t = new com.example.forevernote.data.models.Tag(
                                                    part.trim());
                                            t.setId(part.trim());
                                            note.addTag(t);
                                        }
                                    }
                                } else {
                                    com.example.forevernote.data.models.Tag t = new com.example.forevernote.data.models.Tag(
                                            tagsVal.trim());
                                    t.setId(tagsVal.trim());
                                    note.addTag(t);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // Ignore read errors for lightweight load
            }
        }
        return note;
    }

    private String getValue(String line) {
        int idx = line.indexOf(':');
        if (idx > 0 && idx < line.length() - 1) {
            return line.substring(idx + 1).trim();
        }
        return "";
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
            cachedNotes.put(relativePath, note);
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
                    // Update ID map and Cache
                    String oldId = note.getId();
                    idToPathMap.remove(oldId);
                    cachedNotes.remove(oldId);

                    String newId = rootPath.relativize(newPath).toString();
                    idToPathMap.put(newId, newPath);
                    note.setId(newId); // Update object ID
                    path = newPath;
                    cachedNotes.put(newId, note);
                } catch (IOException e) {
                    logger.warning("Failed to rename note file during update: " + e.getMessage());
                }
            }
        }

        note.setModifiedDate(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));

        try {
            String fileContent = FrontmatterHandler.generate(note);
            Files.writeString(path, fileContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            cachedNotes.put(note.getId(), note);
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
                    cachedNotes.put(id, note);
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
                cachedNotes.remove(id);
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
        if (cachedNotes.isEmpty()) {
            refreshCache();
        }

        List<Note> notes = new ArrayList<>();
        String prefix = (folderId == null || folderId.equals("ROOT")) ? "" : folderId + File.separator;

        for (Note note : cachedNotes.values()) {
            String id = note.getId();

            if (folderId == null || folderId.equals("ROOT")) {
                // Root folder: direct children have no separator
                if (!id.contains(File.separator)) {
                    notes.add(note);
                }
            } else {
                // Subfolder: must start with folder path + separator, and have no further
                // separators
                if (id.startsWith(prefix)) {
                    String remaining = id.substring(prefix.length());
                    if (!remaining.contains(File.separator)) {
                        notes.add(note);
                    }
                }
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
        if (cachedNotes.isEmpty()) {
            refreshCache();
        }
        return new ArrayList<>(cachedNotes.values());
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
        if (cachedNotes.isEmpty()) {
            refreshCache();
        }
        List<Note> all = new ArrayList<>();
        for (Note n : cachedNotes.values()) {
            for (com.example.forevernote.data.models.Tag t : n.getTags()) {
                if (t.getTitle().equals(tagId)) {
                    all.add(n);
                    break;
                }
            }
        }
        return all;
    }

    private String sanitizeFilename(String title) {
        return title.replaceAll("[^a-zA-Z0-9\\.\\-_ ]", "_");
    }
}
