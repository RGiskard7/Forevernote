package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.example.forevernote.data.dao.interfaces.FolderDAO;
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

    @Test
    void createFolderInRootShouldCallDaoCreateOnly() {
        FolderWorkflow workflow = new FolderWorkflow();
        AtomicReference<Folder> createdFolder = new AtomicReference<>();
        AtomicReference<Folder> addedSubfolder = new AtomicReference<>();

        FolderDAO dao = (FolderDAO) Proxy.newProxyInstance(
                FolderDAO.class.getClassLoader(),
                new Class<?>[] { FolderDAO.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "createFolder":
                            Folder f = (Folder) args[0];
                            createdFolder.set(f);
                            return "folder-id";
                        case "addSubFolder":
                            addedSubfolder.set((Folder) args[1]);
                            return null;
                        default:
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class) {
                                return false;
                            }
                            return null;
                    }
                });

        FolderWorkflow.FolderCreationResult result = workflow.createFolder(dao, "Inbox", null, true);
        assertTrue(result.success());
        assertEquals("folder-id", result.folder().getId());
        assertEquals("Inbox", result.folder().getTitle());
        assertTrue(createdFolder.get() != null);
        assertTrue(addedSubfolder.get() == null);
    }

    @Test
    void createSubfolderShouldCallDaoCreateAndAddSubfolder() {
        FolderWorkflow workflow = new FolderWorkflow();
        AtomicReference<Folder> addedParent = new AtomicReference<>();
        AtomicReference<Folder> addedChild = new AtomicReference<>();

        FolderDAO dao = (FolderDAO) Proxy.newProxyInstance(
                FolderDAO.class.getClassLoader(),
                new Class<?>[] { FolderDAO.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "createFolder":
                            return "child-id";
                        case "addSubFolder":
                            addedParent.set((Folder) args[0]);
                            addedChild.set((Folder) args[1]);
                            return null;
                        default:
                            Class<?> returnType = method.getReturnType();
                            if (returnType == boolean.class) {
                                return false;
                            }
                            return null;
                    }
                });

        Folder parent = new Folder("root/work", "Work");
        FolderWorkflow.FolderCreationResult result = workflow.createSubfolder(dao, "Projects", parent);

        assertTrue(result.success());
        assertEquals("child-id", result.folder().getId());
        assertEquals(parent, addedParent.get());
        assertEquals("Projects", addedChild.get().getTitle());
    }

    @Test
    void createFolderShouldFailWhenNameIsBlank() {
        FolderWorkflow workflow = new FolderWorkflow();
        FolderWorkflow.FolderCreationResult result = workflow.createFolder(null, " ", null, true);
        assertFalse(result.success());
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
