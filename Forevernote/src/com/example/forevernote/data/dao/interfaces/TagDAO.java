package com.example.forevernote.data.dao.interfaces;

import java.util.List;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;

/**
 * This interface provides methods to interact with tags in a data access layer.
 */
public interface TagDAO {

    public int createTag(Tag tag);

    /**
     * Retrieves a tag by its unique identifier.
     *
     * @param id The ID of the tag to retrieve.
     * @return The tag with the specified ID, or null if not found.
     */
    
    public void updateTag(Tag tag);

    public void deleteTag(int id);
    
    public Tag getTagById(int id);

    public List<Tag> fetchAllTags();

    public List<Note> fetchAllNotesWithTag(int tagId);

    /**
     * Checks if a tag with the given title exists.
     *
     * @param title The title to check for existence.
     * @return True if a tag with the title exists, otherwise false.
     */
    public boolean existsByTitle(String title);
}


