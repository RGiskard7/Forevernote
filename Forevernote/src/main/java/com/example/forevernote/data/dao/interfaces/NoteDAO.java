package com.example.forevernote.data.dao.interfaces;

import java.util.List;

import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Folder;

/**
 * This interface defines the contract for data access operations related to notes.
 * It provides methods for creating, retrieving, updating, and deleting notes, 
 * as well as managing their relationships with folders and tags.
 */
public interface NoteDAO {

	// CRUD Operations
    /**
     * Creates a new note in the database.
     *
     * @param note The note to be created.
     * @return The generated ID of the created note.
     */
    public int createNote(Note note);

    /**
     * Retrieves a note by its unique ID.
     *
     * @param id The ID of the note.
     * @return The corresponding Note object, or null if not found.
     */
    public Note getNoteById(int id);

    /**
     * Updates an existing note.
     *
     * @param note The note containing updated data.
     */
    public void updateNote(Note note);

    /**
     * Deletes a note by its ID.
     *
     * @param id The ID of the note to be deleted.
     */
    public void deleteNote(int id);

    // Retrieval Methods
    /**
     * Fetches all notes from the database.
     *
     * @return A list of all notes.
     */
    public List<Note> fetchAllNotes();
    
    /**
     * Fetches all notes that belong to a specific folder.
     *
     * @param folderId The ID of the folder.
     * @return A list of notes inside the specified folder.
     */
    public List<Note> fetchNotesByFolderId(int folderId);
    
    /**
     * Loads all notes that belong to a specific folder into the folder object.
     *
     * @param folder The folder whose notes should be loaded.
     */
    public void fetchNotesByFolderId(Folder folder);

    /**
     * Retrieves the folder that contains a specific note.
     *
     * @param noteId The ID of the note.
     * @return The Folder object that contains the given note, or null if the note is not inside a folder.
     */
    public Folder getFolderOfNote(int noteId);

    // Tag Management
    /**
     * Assigns a tag to a note using their respective IDs.
     *
     * @param noteId The ID of the note.
     * @param tagId The ID of the tag to be assigned.
     */
    public void addTag(int noteId, int tagId);
    
    /**
     * Assigns a tag to a note.
     *
     * @param note The note to which the tag should be assigned.
     * @param tag The tag to be assigned.
     */
    public void addTag(Note note, Tag tag);
    
    /**
     * Removes a tag from a note using their respective IDs.
     *
     * @param noteId The ID of the note.
     * @param tagId The ID of the tag to be removed.
     */
    public void removeTag(int noteId, int tagId);	
    
    /**
     * Removes a tag from a note.
     *
     * @param note The note from which the tag should be removed.
     * @param tag The tag to be removed.
     */
    public void removeTag(Note note, Tag tag);

    /**
     * Fetches all tags associated with a given note.
     *
     * @param noteId The ID of the note.
     * @return A list of tags assigned to the note.
     */
    public List<Tag> fetchTags(int noteId);
    
    /**
     * Loads all tags assigned to a specific note into the note object.
     *
     * @param note The note whose tags should be loaded.
     */
    public void loadTags(Note note);
    
    /**
     * Fetches all notes that have a specific tag.
     *
     * @param tagId The ID of the tag.
     * @return A list of notes that have the specified tag.
     */
    public List<Note> fetchNotesByTagId(int tagId);
}