package com.example.forevernote.ui.workflow;

import java.util.logging.Logger;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.models.Folder;

/**
 * Orchestrates folder-selection behavior in UI while keeping controller thin.
 */
public class FolderWorkflow {
    private static final Logger logger = LoggerConfig.getLogger(FolderWorkflow.class);

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
}
