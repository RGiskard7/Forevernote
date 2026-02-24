package com.example.forevernote.data.dao.filesystem;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
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
import java.util.stream.Stream;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.models.interfaces.Component;
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

    public void refreshCache() {
        idToPathMap.clear();
        cachedNotes.clear();
        try (Stream<Path> walk = Files.walk(rootPath)) {
            // Using parallel stream for faster initial load of thousands of headers
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.toString().contains(File.separator + ".")) // Exclude hidden files and folders
                                                                               // (.trash, .git)
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
            try (BufferedReader reader = Files.newBufferedReader(path)) {
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
                                        Tag t = new Tag(part.trim());
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
                                            Tag t = new Tag(part.trim());
                                            t.setId(part.trim());
                                            note.addTag(t);
                                        }
                                    }
                                } else {
                                    Tag t = new Tag(tagsVal.trim());
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
        Path sourcePath = idToPathMap.get(id);
        if (sourcePath != null && Files.exists(sourcePath)) {
            try {
                // Determine target path in trash while preserving relative structure
                Path trashDir = rootPath.resolve(".trash");
                Path targetPath = trashDir.resolve(id); // id is relative path

                // Ensure target parent directories exist in trash
                if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                    Files.createDirectories(targetPath.getParent());
                }

                // Avoid collision in trash
                if (Files.exists(targetPath)) {
                    String filename = targetPath.getFileName().toString();
                    String name = filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;
                    targetPath = targetPath.getParent().resolve(name + "_" + System.currentTimeMillis() + ".md");
                }

                Files.move(sourcePath, targetPath);

                // Remove from cache
                idToPathMap.remove(id);
                cachedNotes.remove(id);

            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to move note to trash: " + sourcePath, e);
            }
        }
    }

    @Override
    public List<Note> fetchTrashNotes() {
        List<Note> deletedNotes = new ArrayList<>();
        Path trashPath = rootPath.resolve(".trash");
        if (Files.exists(trashPath) && Files.isDirectory(trashPath)) {
            try (Stream<Path> stream = Files.walk(trashPath)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md"))
                        .filter(p -> !p.getFileName().toString().startsWith(".")) // Ignore hidden files
                        .filter(p -> {
                            // Only skip truly hidden system files like .DS_Store or .obsidian
                            // but allow trashed folders starting with dot
                            String filename = p.getFileName().toString();
                            return !filename.startsWith(".") || filename.endsWith(".md");
                        })
                        .forEach(p -> {
                            // Create note with ID relative to root (e.g. .trash/sub/note.md)
                            String trashId = rootPath.relativize(p).toString().replace("\\", "/");
                            Note note = createLightweightNote(trashId, p);
                            note.setDeleted(true);
                            deletedNotes.add(note);
                        });
            } catch (IOException e) {
                logger.warning("Error reading trash folder: " + e.getMessage());
            }
        }
        return deletedNotes;
    }

    @Override
    public void restoreNote(String id) {
        // ID is relative path like .trash/note.md or .trash/Folder/note.md
        try {
            Path source = rootPath.resolve(id);
            if (!Files.exists(source)) {
                // Try fallback to just filename if id is not found (for old trashed notes)
                String filename = Paths.get(id).getFileName().toString();
                source = rootPath.resolve(".trash").resolve(filename);
            }

            if (Files.exists(source)) {
                // Calculate original relative path
                String originalRelPath = id;
                if (id.startsWith(".trash" + File.separator)) {
                    originalRelPath = id.substring((".trash" + File.separator).length());
                } else if (id.startsWith(".trash")) {
                    originalRelPath = id.substring(6);
                    if (originalRelPath.startsWith("/") || originalRelPath.startsWith("\\")) {
                        originalRelPath = originalRelPath.substring(1);
                    }
                }

                Path target = rootPath.resolve(originalRelPath);

                // If parent folder was deleted (not restored), restore to root instead of
                // recreating the folder
                if (target.getParent() != null && !Files.exists(target.getParent())) {
                    String filenameOrig = target.getFileName().toString();
                    target = rootPath.resolve(filenameOrig);
                }

                // Handle conflicts
                if (Files.exists(target)) {
                    String filename = target.getFileName().toString();
                    String name = filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;
                    target = target.getParent().resolve(name + "_restored_" + System.currentTimeMillis() + ".md");
                }

                // Ensure parent exists (always true if it's rootPath, but safe to keep)
                if (target.getParent() != null && !Files.exists(target.getParent())) {
                    Files.createDirectories(target.getParent());
                }

                Files.move(source, target);

                // Update cache
                refreshCache();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to restore note: " + id, e);
        }
    }

    @Override
    public void permanentlyDeleteNote(String id) {
        try {
            Path path = rootPath.resolve(id); // id starts with .trash/ usually

            if (Files.exists(path)) {
                Files.delete(path);
                idToPathMap.remove(id);
                cachedNotes.remove(id);
            } else {
                // Try fallback to filename in trash root
                String filename = path.getFileName().toString();
                Path fallback = rootPath.resolve(".trash").resolve(filename);
                if (Files.exists(fallback)) {
                    Files.delete(fallback);
                }
            }
            // Also remove from cache
            cachedNotes.remove(id);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to permanently delete note: " + id, e);
        }
    }

    @Override
    public List<Note> fetchNotesByFolderId(String folderId) {
        if (cachedNotes.isEmpty()) {
            refreshCache();
        }

        List<Note> notes = new ArrayList<>();
        String prefix = (folderId == null || folderId.equals("ROOT")) ? "" : folderId + File.separator;

        for (Note note : cachedNotes.values()) {
            if (note.isDeleted())
                continue;

            String id = note.getId().replace("\\", "/");
            String normalizedPrefix = prefix.replace("\\", "/");

            if (folderId == null || folderId.equals("ROOT")) {
                // Root folder: direct children have no separator
                if (!id.contains("/")) {
                    notes.add(note);
                }
            } else {
                // Subfolder: must start with folder path + separator, and have no further
                // separators
                if (id.startsWith(normalizedPrefix)) {
                    String remaining = id.substring(normalizedPrefix.length());
                    if (!remaining.contains("/")) {
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
        List<Component> components = new ArrayList<>(notes);
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
            for (Tag t : n.getTags()) {
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
