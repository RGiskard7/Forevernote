package com.example.forevernote.ui.workflow;

import com.example.forevernote.data.models.Tag;

/**
 * Orchestrates tag selection/filter behavior.
 */
public class TagWorkflow {

    public interface TagSelectionPort {
        void setCurrentFolderToNull();

        void setCurrentTag(Tag tag);

        void setCurrentFilterType(String filterType);

        void loadNotesForTag(String tagTitle);
    }

    public void selectTag(Tag tag, TagSelectionPort port) {
        if (tag == null || tag.getTitle() == null || tag.getTitle().isBlank() || port == null) {
            return;
        }
        port.setCurrentFolderToNull();
        port.setCurrentTag(tag);
        port.setCurrentFilterType("tag");
        port.loadNotesForTag(tag.getTitle());
    }

    public void selectTagByTitle(String tagTitle, TagSelectionPort port) {
        if (tagTitle == null || tagTitle.isBlank() || port == null) {
            return;
        }
        selectTag(new Tag(tagTitle), port);
    }
}
