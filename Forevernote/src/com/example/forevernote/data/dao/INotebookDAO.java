package com.example.forevernote.data.dao;

import java.util.List;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Notebook;

/**
 * This interface provides methods to interact with notebooks in a data access layer.
 */
public interface INotebookDAO {

    /**
     * Creates a new notebook with the specified title.
     *
     * @param title The title of the notebook.
     * @return The ID of the newly created notebook, or -1 if creation fails.
     */
    public int createNotebook(String title);

    /**
     * Retrieves a notebook by its unique identifier.
     *
     * @param id The ID of the notebook to retrieve.
     * @return The notebook with the specified ID, or null if not found.
     */
    public Notebook getNotebookById(int id);
    
    /**
     * Retrieves the notebook associated with a note.
     *
     * @param noteId The ID of the note.
     * @return The notebook associated with the note, or null if not found.
     */
    public Notebook getNotebookByNoteId(int noteId);

    /**
     * Edits an existing notebook with the specified ID, updating its title.
     *
     * @param id    The ID of the notebook to edit.
     * @param title The new title for the notebook.
     */
    public void updateNotebook(int id, String title);

    /**
     * Deletes a notebook with the specified ID.
     *
     * @param id The ID of the notebook to delete.
     */
    public void deleteNoteboo(int id);

    /**
     * Retrieves all notebooks and populates the provided list.
     *
     * @param list The list to populate with notebooks.
     */
    public void getAllNotebooks(List<Notebook> list);

    /**
     * Checks if a notebook with the given title exists.
     *
     * @param title The title to check for existence.
     * @return True if a notebook with the title exists, otherwise false.
     */
    public boolean existsByTitle(String title);

    /**
     * Adds a note to a notebook.
     *
     * @param notebookId The ID of the notebook to add the note to.
     * @param noteId     The ID of the note to add.
     */
    public void addNoteToNotebook(int notebookId, int noteId);

    /**
     * Retrieves all notes associated with a notebook and populates the provided list.
     *
     * @param notebookId The ID of the notebook.
     * @param list       The list to populate with notes.
     */
    public void getAllNotesFromNotebook(int notebookId, List<Note> list);

    /**
     * Removes a note from a notebook.
     *
     * @param notebookId The ID of the notebook from which to remove the note.
     * @param noteId     The ID of the note to remove.
     */
    public void removeNoteFromNotebook(int notebookId, int noteId);
}


