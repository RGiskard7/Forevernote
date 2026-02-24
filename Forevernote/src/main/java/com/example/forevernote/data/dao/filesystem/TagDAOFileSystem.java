package com.example.forevernote.data.dao.filesystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.forevernote.data.dao.interfaces.TagDAO;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;

/**
 * File System implementation of TagDAO.
 * Derives tags from existing notes.
 */
public class TagDAOFileSystem implements TagDAO {

    private final NoteDAOFileSystem noteDAO;

    public TagDAOFileSystem(NoteDAOFileSystem noteDAO) {
        this.noteDAO = noteDAO;
    }

    @Override
    public String createTag(Tag tag) {
        // Tags are created implicitly when added to notes in this simple FS model
        // So just return ID (Title)
        if (tag.getId() == null) {
            tag.setId(tag.getTitle());
        }
        return tag.getId();
    }

    @Override
    public Tag getTagById(String id) {
        // Tag ID in this system is usually determining the tag object properties
        // We can't fetch metadata for tag unless we store it separately.
        // For now, assume ID = Title
        return new Tag(id, id);
    }

    @Override
    public List<Tag> fetchAllTags() {
        Set<Tag> tags = new HashSet<>();
        List<Note> notes = noteDAO.fetchAllNotes();
        for (Note note : notes) {
            tags.addAll(note.getTags());
        }
        return new ArrayList<>(tags);
    }

    @Override
    public void updateTag(Tag tag) {
        // Renaming a tag implies updating all notes that have this tag.
        // Expensive operation.
        // TODO: Implement rename
    }

    @Override
    public void deleteTag(String id) {
        // Remove tag from all notes
        List<Note> notes = noteDAO.fetchAllNotes();
        for (Note note : notes) {
            List<Tag> tags = note.getTags();
            boolean changed = false;
            // Remove by ID or Title?
            // Assuming ID matches for now
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).getId() != null && tags.get(i).getId().equals(id)) {
                    note.removeTag(tags.get(i));
                    changed = true;
                    i--;
                } else if (tags.get(i).getTitle().equals(id)) { // Fallback if ID is title
                    note.removeTag(tags.get(i));
                    changed = true;
                    i--;
                }
            }
            if (changed) {
                noteDAO.updateNote(note);
            }
        }
    }

    @Override
    public List<Note> fetchAllNotesWithTag(String tagId) {
        // Tag ID is title for now? Or UUID. Note objects store Tags.
        // We iterate all notes and check if they contain the tag.
        List<Note> result = new ArrayList<>();
        List<Note> allNotes = noteDAO.fetchAllNotes();
        for (Note note : allNotes) {
            for (Tag t : note.getTags()) {
                if ((t.getId() != null && t.getId().equals(tagId)) || t.getTitle().equals(tagId)) {
                    result.add(note);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public boolean existsByTitle(String title) {
        List<Tag> allTags = fetchAllTags();
        for (Tag t : allTags) {
            if (t.getTitle().equalsIgnoreCase(title)) {
                return true;
            }
        }
        return false;
    }
}
