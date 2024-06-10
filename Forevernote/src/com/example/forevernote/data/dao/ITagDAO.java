package com.example.forevernote.data.dao;

import java.util.List;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;

/**
 * This interface provides methods to interact with tags in a data access layer.
 */
public interface ITagDAO {

    /**
     * Creates a new tag with the specified title.
     *
     * @param title The title of the tag.
     */
    public void createTag(String title);

    /**
     * Retrieves a tag by its unique identifier.
     *
     * @param id The ID of the tag to retrieve.
     * @return The tag with the specified ID, or null if not found.
     */
    public Tag getTagById(int id);

    /**
     * Edits an existing tag with the specified ID, updating its title.
     *
     * @param id    The ID of the tag to edit.
     * @param title The new title for the tag.
     */
    public void editTag(int id, String title);

    /**
     * Deletes a tag with the specified ID.
     *
     * @param id The ID of the tag to delete.
     */
    public void deleteTagById(int id);

    /**
     * Retrieves all tags and populates the provided list.
     *
     * @param list The list to populate with tags.
     */
    public void getAllTags(List<Tag> list);

    /**
     * Retrieves all notes associated with a tag and populates the provided list.
     *
     * @param tagId The ID of the tag.
     * @param list  The list to populate with notes.
     */
    public void getAllNotesWithTag(int tagId, List<Note> list);

    /**
     * Checks if a tag with the given title exists.
     *
     * @param title The title to check for existence.
     * @return True if a tag with the title exists, otherwise false.
     */
    public boolean existsByTitle(String title);
}


