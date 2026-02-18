package com.example.forevernote.data.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.exceptions.DataAccessException;
import com.example.forevernote.exceptions.InvalidParameterException;

/**
 * File System implementation of FolderDAO.
 * Maps application folders to filesystem directories.
 * Stores metadata in .folder.yaml inside each directory.
 */
public class FolderDAOFileSystem implements FolderDAO {

    private static final Logger logger = LoggerConfig.getLogger(FolderDAOFileSystem.class);
    private final Path rootPath;

    // Cache is less critical if we rely on paths, but useful for performance
    // Map ID (Relative Path) -> Absolute Path
    private final Map<String, Path> idToPathMap = new ConcurrentHashMap<>();

    public FolderDAOFileSystem(String rootDirectory) {
        this.rootPath = Paths.get(rootDirectory);
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectories(rootPath);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create root directory for folders: " + rootDirectory, e);
                throw new DataAccessException("Could not initialize file storage", e);
            }
        }
        refreshCache();
    }

    private void refreshCache() {
        idToPathMap.clear();
        // ID "" (empty string) or "ROOT" maps to rootPath
        idToPathMap.put("ROOT", rootPath);

        try (Stream<Path> walk = Files.walk(rootPath)) {
            walk.filter(Files::isDirectory)
                    .filter(p -> !p.equals(rootPath))
                    .forEach(path -> {
                        String relativePath = rootPath.relativize(path).toString();
                        idToPathMap.put(relativePath, path);
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to walk directory for folder cache refresh", e);
        }
    }

    @Override
    public String createFolder(Folder folder) {
        if (folder == null)
            throw new InvalidParameterException("Folder cannot be null");

        // Logic: ID is path. If ID is provided, use it. If not, use Title as name in
        // root.
        String parentId = "ROOT";
        if (folder.getParent() != null) {
            parentId = folder.getParent().getId();
        }

        Path parentPath = idToPathMap.get(parentId);
        if (parentPath == null)
            parentPath = rootPath;

        String folderName = sanitizeFilename(folder.getTitle());
        Path dirPath = parentPath.resolve(folderName);

        // Handle duplication
        int counter = 1;
        while (Files.exists(dirPath)) {
            dirPath = parentPath.resolve(folderName + " (" + counter + ")");
            counter++;
        }

        try {
            Files.createDirectories(dirPath);
            String newId = rootPath.relativize(dirPath).toString();
            idToPathMap.put(newId, dirPath);
            folder.setId(newId);
            return newId;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create folder directory: " + dirPath, e);
            return null;
        }
    }

    @Override
    public void updateFolder(Folder folder) {
        // Rename logic
        Path currentPath = idToPathMap.get(folder.getId());
        if (currentPath == null || !Files.exists(currentPath))
            return;

        String newName = sanitizeFilename(folder.getTitle());
        if (!currentPath.getFileName().toString().equals(newName)) {
            Path newPath = currentPath.resolveSibling(newName);
            if (!Files.exists(newPath)) {
                try {
                    Files.move(currentPath, newPath);
                    // Update Cache: Removing old ID and adding new one is tricky for recursive
                    // children headers
                    // Easiest is to refresh cache fully or update recursively.
                    // For now, refreshing cache is safer though invalidates other IDs if we held
                    // them
                    refreshCache();
                    folder.setId(rootPath.relativize(newPath).toString());
                } catch (IOException e) {
                    logger.warning("Failed to rename folder: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void deleteFolder(String id) {
        Path path = idToPathMap.get(id);
        if (path != null && Files.exists(path)) {
            try {
                deleteDirectoryRecursively(path);
                idToPathMap.remove(id);
                // Also remove subfolders from cache
                idToPathMap.keySet().removeIf(k -> k.startsWith(id + File.separator) || k.equals(id));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to delete folder: " + path, e);
            }
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                entries.forEach(entry -> {
                    try {
                        deleteDirectoryRecursively(entry);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        Files.delete(path);
    }

    @Override
    public Folder getFolderById(String id) {
        Path path = idToPathMap.get(id);
        if (path != null) {
            return new Folder(id, path.getFileName().toString());
        }
        return null;
    }

    @Override
    public Folder getFolderByNoteId(String noteId) {
        // NoteID is relative path "Folder/Note.md"
        // Return folder "Folder"
        Path notePath = Paths.get(noteId);
        Path parent = notePath.getParent();
        if (parent == null)
            return getFolderById("ROOT"); // Root folder

        return getFolderById(parent.toString());
    }

    @Override
    public List<Folder> fetchAllFoldersAsList() {
        return idToPathMap.entrySet().stream()
                .map(e -> new Folder(e.getKey(), e.getValue().getFileName().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public Folder fetchAllFoldersAsTree() {
        Folder root = new Folder("ROOT", "All Notes");
        loadSubFolders(root);
        return root;
    }

    @Override
    public void addNote(Folder folder, Note note) {
        // Move note file
        // Note logic handles this usually
    }

    @Override
    public void removeNote(Folder folder, Note note) {
    }

    @Override
    public void loadNotes(Folder folder) {
        // This is now redundant if NoteDAO handles it, but UI might call it
        // We rely on MainController using NoteDAO for notes
    }

    @Override
    public void addSubFolder(Folder parent, Folder subFolder) {
        // Logic handled in createFolder or updateFolder typically
    }

    @Override
    public void removeSubFolder(Folder parentFolder, Folder subFolder) {
        deleteFolder(subFolder.getId());
    }

    @Override
    public void loadSubFolders(Folder folder) {
        loadSubFolders(folder, Integer.MAX_VALUE);
    }

    @Override
    public void loadSubFolders(Folder folder, int maxDepth) {
        if (maxDepth <= 0)
            return;

        Path path = (folder.getId().equals("ROOT")) ? rootPath : idToPathMap.get(folder.getId());
        if (path == null || !Files.exists(path))
            return;

        try (Stream<Path> stream = Files.list(path)) {
            stream.filter(Files::isDirectory)
                    .forEach(p -> {
                        String startPath = rootPath.relativize(p).toString();
                        Folder sub = new Folder(startPath, p.getFileName().toString());
                        sub.setParent(folder);
                        folder.add(sub);
                        loadSubFolders(sub, maxDepth - 1);
                    });
        } catch (IOException e) {
            logger.warning("Error loading subfolders for " + folder.getTitle());
        }
    }

    @Override
    public void loadParentFolders(Folder folder) {
        // Not implemented strictly
    }

    @Override
    public void loadParentFolders(Folder folder, int maxDepth) {
        // Not implemented strictly
    }

    @Override
    public void loadParentFolder(Folder folder) {
        // Not implemented strictly
    }

    @Override
    public Folder getParentFolder(String folderId) {
        Path path = idToPathMap.get(folderId);
        if (path != null) {
            Path parent = path.getParent();
            if (parent != null && parent.startsWith(rootPath)) {
                if (parent.equals(rootPath))
                    return getFolderById("ROOT");
                return getFolderById(rootPath.relativize(parent).toString());
            }
        }
        return null;
    }

    @Override
    public Folder getParentFolder(Folder folder) {
        return getParentFolder(folder.getId());
    }

    @Override
    public String getPathFolder(String idFolder) {
        Path path = idToPathMap.get(idFolder);
        return path != null ? path.toAbsolutePath().toString() : null;
    }

    @Override
    public boolean existsByTitle(String title) {
        return Files.exists(rootPath.resolve(sanitizeFilename(title)));
    }

    private String sanitizeFilename(String title) {
        return title.replaceAll("[^a-zA-Z0-9\\.\\-_ ]", "_");
    }
}
