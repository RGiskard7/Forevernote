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
                    // Exclude any directory that is inside a hidden folder (e.g. .trash, .git)
                    .filter(p -> !p.toString().contains(File.separator + "."))
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
                Path trashRoot = rootPath.resolve(".trash");
                if (!Files.exists(trashRoot)) {
                    Files.createDirectories(trashRoot);
                }

                // Use the relative ID to preserve structure in trash
                Path targetPath = trashRoot.resolve(id);

                // Ensure target parent directories exist in trash
                if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                    Files.createDirectories(targetPath.getParent());
                }

                // Handle duplication
                if (Files.exists(targetPath)) {
                    String name = targetPath.getFileName().toString();
                    targetPath = targetPath.getParent().resolve(name + "_" + System.currentTimeMillis());
                }

                Files.move(path, targetPath);

                // Update cache
                idToPathMap.remove(id);
                // Also remove subfolders from cache
                String idPrefix = id + File.separator;
                idToPathMap.keySet().removeIf(k -> k.startsWith(idPrefix) || k.equals(id));

            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to move folder to trash: " + path, e);
                throw new DataAccessException("Failed to delete folder", e);
            }
        }
    }

    @Override
    public Folder fetchTrashFolders() {
        // Title should be a localized string or just "Trash", not ".trash"
        Folder trashRootFolder = new Folder(".trash", "Trash");
        Path trashPath = rootPath.resolve(".trash");

        if (Files.exists(trashPath)) {
            loadSubFoldersRec(trashRootFolder, trashPath);
        }
        return trashRootFolder;
    }

    private void loadSubFoldersRec(Folder parent, Path currentPath) {
        try (Stream<Path> stream = Files.list(currentPath)) {
            stream.filter(Files::isDirectory)
                    .forEach(p -> {
                        // ID must be relative to rootPath with forward slashes
                        String id = rootPath.relativize(p).toString().replace("\\", "/");

                        // Title MUST be just the directory name
                        String title = p.getFileName().toString();

                        Folder sub = new Folder(id, title);
                        parent.add(sub);
                        sub.setParent(parent);

                        // Do NOT add to idToPathMap - these are trash folders
                        // and must not pollute the main folder cache

                        // Recursively load subfolders
                        loadSubFoldersRec(sub, p);
                    });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading trash subfolders for " + currentPath, e);
        }
    }

    @Override
    public void restoreFolder(String id) {
        // ID is relative path like .trash/MyFolder or .trash/Sub/MyFolder
        Path srcPath = rootPath.resolve(id);
        if (!Files.exists(srcPath))
            throw new DataAccessException("Folder not found in trash: " + id, null);

        // Calculate original relative path by removing .trash/ prefix
        String originalRelativePath = id;
        if (id.startsWith(".trash" + File.separator)) {
            originalRelativePath = id.substring((".trash" + File.separator).length());
        } else if (id.equals(".trash")) {
            return; // Cannot restore the trash itself
        } else if (id.startsWith(".trash")) {
            // Case where separator might be different or it's just .trash/folder
            originalRelativePath = id.substring(6);
            if (originalRelativePath.startsWith("/") || originalRelativePath.startsWith("\\")) {
                originalRelativePath = originalRelativePath.substring(1);
            }
        }

        Path targetPath = rootPath.resolve(originalRelativePath);

        // If target already exists, append timestamp or similar to avoid conflict
        if (Files.exists(targetPath)) {
            String name = srcPath.getFileName().toString();
            targetPath = targetPath.getParent().resolve(name + "_restored_" + System.currentTimeMillis());
        }

        try {
            // Ensure parent directory exists
            if (targetPath.getParent() != null && !Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.move(srcPath, targetPath);
            refreshCache();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to restore folder: " + id, e);
            throw new DataAccessException("Failed to restore folder", e);
        }
    }

    @Override
    public void permanentlyDeleteFolder(String id) {
        Path path = rootPath.resolve(id); // Should be in .trash
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);

                idToPathMap.remove(id);
                // Also remove subfolders from cache
                String idPrefix = id.replace("\\", "/") + "/";
                idToPathMap.keySet().removeIf(k -> k.replace("\\", "/").startsWith(idPrefix) || k.equals(id));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to permanently delete folder: " + id, e);
            }
        }
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
                .filter(e -> !e.getKey().equals("ROOT") && !e.getKey().isEmpty()) // Exclude ROOT
                .filter(e -> !e.getValue().getFileName().toString().startsWith(".")) // Exclude hidden
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
        if (folder == null || note == null)
            return;

        Path sourcePath = idToPathMap.get(note.getId());
        if (sourcePath == null)
            return;

        Path targetDir;
        if ("ROOT".equals(folder.getId())) {
            targetDir = rootPath;
        } else {
            targetDir = idToPathMap.get(folder.getId());
        }

        if (targetDir == null || !Files.exists(targetDir))
            return;

        Path targetPath = targetDir.resolve(sourcePath.getFileName());

        try {
            if (!sourcePath.equals(targetPath)) {
                Files.move(sourcePath, targetPath);

                // Update maps and note object
                String oldId = note.getId();
                String newId = rootPath.relativize(targetPath).toString();

                idToPathMap.remove(oldId);
                idToPathMap.put(newId, targetPath);

                // We need to update the note in NoteDAO cache too
                // Since we don't have direct access easily without coupling,
                // but MainController refreshes both.
                // However, we MUST update the note object itself so subsequent calls use the
                // new ID
                note.setId(newId);
                note.setParent(folder);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to move note file to folder: " + folder.getId(), e);
        }
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
                    .filter(p -> !p.getFileName().toString().startsWith(".")) // Ignore hidden
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
