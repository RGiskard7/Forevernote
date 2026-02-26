package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.forevernote.data.models.Tag;
import com.example.forevernote.ui.workflow.TagWorkflow;

class TagWorkflowTest {

    @Test
    void selectTagUpdatesContextAndLoadsNotes() {
        TagWorkflow workflow = new TagWorkflow();
        RecordingPort port = new RecordingPort();

        workflow.selectTag(new Tag("t1", "Backend"), port);

        assertEquals(1, port.clearFolderCalls);
        assertEquals("Backend", port.currentTagTitle);
        assertEquals("tag", port.filterType);
        assertEquals("Backend", port.loadedTagTitle);
    }

    @Test
    void selectTagByTitleCreatesTagContext() {
        TagWorkflow workflow = new TagWorkflow();
        RecordingPort port = new RecordingPort();

        workflow.selectTagByTitle("Roadmap", port);

        assertEquals("Roadmap", port.currentTagTitle);
        assertEquals("Roadmap", port.loadedTagTitle);
    }

    private static final class RecordingPort implements TagWorkflow.TagSelectionPort {
        private int clearFolderCalls = 0;
        private String currentTagTitle;
        private String filterType;
        private String loadedTagTitle;

        @Override
        public void setCurrentFolderToNull() {
            clearFolderCalls++;
        }

        @Override
        public void setCurrentTag(Tag tag) {
            this.currentTagTitle = tag.getTitle();
        }

        @Override
        public void setCurrentFilterType(String filterType) {
            this.filterType = filterType;
        }

        @Override
        public void loadNotesForTag(String tagTitle) {
            this.loadedTagTitle = tagTitle;
        }
    }
}
