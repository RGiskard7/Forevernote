package com.example.forevernote.ui.workflow;

import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.models.Folder;

/**
 * Orchestrates folder-selection behavior in UI while keeping controller thin.
 */
public class FolderWorkflow {
    private static final Logger logger = LoggerConfig.getLogger(FolderWorkflow.class);

    public record FolderCreationResult(boolean success, Folder folder, String errorMessage) {
    }

    public interface FolderSelectionPort {
        void setCurrentFolder(Folder folder);

        void clearCurrentTag();

        void setCurrentFilterType(String filterType);

        void loadNotesForFolder(Folder folder);

        boolean hasNotesPanel();

        double getNotesPanelMaxWidth();

        void setNotesPanelMinWidth(double width);

        void setNotesPanelMaxWidth(double width);

        double[] getNavDividerPositions();

        void setNavDividerPosition(double position);

        void setContentDividerPosition(double position);

        void setNotesPanelToggleSelected(boolean selected);
    }

    public void handleFolderSelection(Folder folder, boolean isStackedLayout, FolderSelectionPort port) {
        try {
            if (folder == null || port == null) {
                return;
            }

            port.setCurrentFolder(folder);
            port.clearCurrentTag();
            port.setCurrentFilterType("folder");
            port.loadNotesForFolder(folder);

            if (!port.hasNotesPanel()) {
                return;
            }

            if (isStackedLayout) {
                double[] positions = port.getNavDividerPositions();
                boolean hasDivider = positions != null && positions.length > 0;
                boolean dividerCollapsed = hasDivider && positions[0] > 0.95;

                if (port.getNotesPanelMaxWidth() < 10 || dividerCollapsed) {
                    port.setNotesPanelMinWidth(180);
                    port.setNotesPanelMaxWidth(Double.MAX_VALUE);
                    port.setNavDividerPosition(0.5);
                }
            } else if (port.getNotesPanelMaxWidth() < 10) {
                port.setNotesPanelMinWidth(180);
                port.setNotesPanelMaxWidth(Double.MAX_VALUE);
                port.setContentDividerPosition(0.25);
            }

            port.setNotesPanelToggleSelected(true);
        } catch (Exception e) {
            logger.severe("Failed to handle folder selection " + (folder != null ? folder.getTitle() : "null") + ": "
                    + e.getMessage());
        }
    }

    /**
     * Creates a folder in root or inside current folder based on createInRoot flag.
     */
    public FolderCreationResult createFolder(
            FolderDAO folderDAO,
            String folderName,
            Folder currentFolder,
            boolean createInRoot) {
        if (folderDAO == null) {
            return new FolderCreationResult(false, null, "FolderDAO is null");
        }
        if (folderName == null || folderName.isBlank()) {
            return new FolderCreationResult(false, null, "Folder name is empty");
        }

        try {
            Folder newFolder = new Folder(folderName.trim());
            if (!createInRoot && currentFolder != null) {
                newFolder.setParent(currentFolder);
            }

            String folderId = folderDAO.createFolder(newFolder);
            if (folderId == null || folderId.isBlank()) {
                return new FolderCreationResult(false, null, "Folder ID is null/blank");
            }

            newFolder.setId(folderId);

            if (!createInRoot && currentFolder != null) {
                folderDAO.addSubFolder(currentFolder, newFolder);
            }

            return new FolderCreationResult(true, newFolder, null);
        } catch (Exception e) {
            logger.warning("Failed to create folder: " + e.getMessage());
            return new FolderCreationResult(false, null, e.getMessage());
        }
    }

    /**
     * Creates a subfolder under a specific parent folder.
     */
    public FolderCreationResult createSubfolder(FolderDAO folderDAO, String subfolderName, Folder parentFolder) {
        if (parentFolder == null) {
            return new FolderCreationResult(false, null, "Parent folder is null");
        }
        return createFolder(folderDAO, subfolderName, parentFolder, false);
    }
}
