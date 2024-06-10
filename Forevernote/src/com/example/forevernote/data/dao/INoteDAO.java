package com.example.forevernote.data.dao;

import java.util.List;

import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Notebook;

/**
 * This interface provides methods to interact with notes in a data access layer.
 */
public interface INoteDAO {

    /**
     * Creates a new note with the specified title and content.
     *
     * @param title   The title of the note.
     * @param content The content of the note.
     * @return The ID of the newly created note, or -1 if creation fails.
     */
    public int createNote(String title, String content);

    /**
     * Retrieves a note by its unique identifier.
     *
     * @param id The ID of the note to retrieve.
     * @return The note with the specified ID, or null if not found.
     */
    public Note getNoteById(int id);

    /**
     * Edits an existing note with the specified ID, updating its title and content.
     *
     * @param id      The ID of the note to edit.
     * @param title   The new title for the note.
     * @param content The new content for the note.
     */
    public void editNote(int id, String title, String content);

    /**
     * Deletes a note with the specified ID.
     *
     * @param id The ID of the note to delete.
     */
    public void deleteNoteById(int id);

    /**
     * Retrieves all notes and populates the provided list.
     *
     * @param list The list to populate with notes.
     */
    public void getAllNotes(List<Note> list);

    /**
     * Retrieves the notebook associated with a note.
     *
     * @param noteId The ID of the note.
     * @return The notebook associated with the note, or null if not found.
     */
    public Notebook getNotebookByNoteId(int noteId);

    /**
     * Removes a note from a notebook.
     *
     * @param noteId     The ID of the note to remove.
     * @param notebookId The ID of the notebook from which to remove the note.
     */
    public void removeNoteFromNotebook(int noteId, int notebookId);

    /**
     * Adds tags to a note.
     *
     * @param noteId The ID of the note.
     * @param tags   The tags to add to the note.
     */
    public void addTagsToNote(int noteId, List<Tag> tags);

    /**
     * Retrieves all tags associated with a note and populates the provided list.
     *
     * @param noteId The ID of the note.
     * @param list   The list to populate with tags.
     */
    public void getAllTagsFromNote(int noteId, List<Tag> list);

    /**
     * Removes tags from a note.
     *
     * @param noteId The ID of the note.
     * @param tags   The tags to remove from the note.
     */
    public void removeTagsFromNote(int noteId, List<Tag> tags);
}

