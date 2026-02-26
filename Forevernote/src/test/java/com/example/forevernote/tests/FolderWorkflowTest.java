package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.ui.workflow.FolderWorkflow;

class FolderWorkflowTest {

    @Test
    void stackedLayoutRestoresCollapsedNotesPanelAndLoadsFolder() {
        FolderWorkflow workflow = new FolderWorkflow();
        RecordingPort port = new RecordingPort();
        port.notesPanelMaxWidth = 1;
        port.navDividerPositions = new double[] { 0.99 };

        Folder folder = new Folder("work", "Work");
        workflow.handleFolderSelection(folder, true, port);

        assertEquals("work", port.currentFolderId);
        assertEquals("folder", port.filterType);
        assertEquals(1, port.loadFolderCalls);
        assertEquals(180.0, port.lastMinWidth);
        assertEquals(Double.MAX_VALUE, port.lastMaxWidth);
        assertEquals(0.5, port.lastNavDivider);
        assertTrue(port.notesToggleSelected);
    }

    @Test
    void columnLayoutExpandsNotesPanelWhenHidden() {
        FolderWorkflow workflow = new FolderWorkflow();
        RecordingPort port = new RecordingPort();
        port.notesPanelMaxWidth = 0;

        workflow.handleFolderSelection(new Folder("docs", "Docs"), false, port);

        assertEquals(0.25, port.lastContentDivider);
        assertTrue(port.notesToggleSelected);
    }

    private static final class RecordingPort implements FolderWorkflow.FolderSelectionPort {
        private String currentFolderId;
        private String filterType;
        private int loadFolderCalls = 0;
        private double notesPanelMaxWidth = Double.MAX_VALUE;
        private double[] navDividerPositions = new double[] { 0.5 };
        private double lastMinWidth = -1;
        private double lastMaxWidth = -1;
        private double lastNavDivider = -1;
        private double lastContentDivider = -1;
        private boolean notesToggleSelected = false;

        @Override
        public void setCurrentFolder(Folder folder) {
            this.currentFolderId = folder.getId();
        }

        @Override
        public void clearCurrentTag() {
        }

        @Override
        public void setCurrentFilterType(String filterType) {
            this.filterType = filterType;
        }

        @Override
        public void loadNotesForFolder(Folder folder) {
            loadFolderCalls++;
        }

        @Override
        public boolean hasNotesPanel() {
            return true;
        }

        @Override
        public double getNotesPanelMaxWidth() {
            return notesPanelMaxWidth;
        }

        @Override
        public void setNotesPanelMinWidth(double width) {
            this.lastMinWidth = width;
        }

        @Override
        public void setNotesPanelMaxWidth(double width) {
            this.lastMaxWidth = width;
        }

        @Override
        public double[] getNavDividerPositions() {
            return navDividerPositions;
        }

        @Override
        public void setNavDividerPosition(double position) {
            this.lastNavDivider = position;
        }

        @Override
        public void setContentDividerPosition(double position) {
            this.lastContentDivider = position;
        }

        @Override
        public void setNotesPanelToggleSelected(boolean selected) {
            this.notesToggleSelected = selected;
        }
    }
}
