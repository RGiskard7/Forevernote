package com.example.forevernote.data.dao.interfaces;

import java.util.List;

import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Folder;

/**
 * This interface provides methods to interact with notes in a data access layer.
 */
public interface NoteDAO {

    public int createNote(Note note);

    public Note getNoteById(int id);

    public void updateNote(Note note);

    public void deleteNote(int id);

    public List<Note> fetchAllNotes();
    
    public List<Note> fetchNotesByFolderId(int folderId);
    
    public void fetchNotesByFolderId(Folder folder);

    public Folder getFolderOfNote(int noteId);

    public void addTag(int noteId, int tagId);
    
    public void addTag(Note note, Tag tag);
    
    public void removeTag(int noteId, int tagId);	
    
    public void removeTag(Note note, Tag tag);

    public List<Tag> fetchTags(int noteId);
    
    public void loadTags(Note note);    
}

