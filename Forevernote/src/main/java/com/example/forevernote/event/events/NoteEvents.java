package com.example.forevernote.event.events;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.event.AppEvent;

/**
 * Note-related events for the application.
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.1.0
 */
public final class NoteEvents {
    
    private NoteEvents() {
        // Prevent instantiation
    }
    
    /**
     * Event fired when a note is selected.
     */
    public static class NoteSelectedEvent extends AppEvent {
        private final Note note;
        
        public NoteSelectedEvent(Note note) {
            super("NoteList");
            this.note = note;
        }
        
        public Note getNote() {
            return note;
        }
    }
    
    /**
     * Event fired when a note is created.
     */
    public static class NoteCreatedEvent extends AppEvent {
        private final Note note;
        
        public NoteCreatedEvent(Note note) {
            this.note = note;
        }
        
        public Note getNote() {
            return note;
        }
    }
    
    /**
     * Event fired when a note is saved.
     */
    public static class NoteSavedEvent extends AppEvent {
        private final Note note;
        
        public NoteSavedEvent(Note note) {
            this.note = note;
        }
        
        public Note getNote() {
            return note;
        }
    }
    
    /**
     * Event fired when a note is deleted.
     */
    public static class NoteDeletedEvent extends AppEvent {
        private final int noteId;
        private final String noteTitle;
        
        public NoteDeletedEvent(int noteId, String noteTitle) {
            this.noteId = noteId;
            this.noteTitle = noteTitle;
        }
        
        public int getNoteId() {
            return noteId;
        }
        
        public String getNoteTitle() {
            return noteTitle;
        }
    }
    
    /**
     * Event fired when a note's favorite status changes.
     */
    public static class NoteFavoriteChangedEvent extends AppEvent {
        private final Note note;
        private final boolean isFavorite;
        
        public NoteFavoriteChangedEvent(Note note, boolean isFavorite) {
            this.note = note;
            this.isFavorite = isFavorite;
        }
        
        public Note getNote() {
            return note;
        }
        
        public boolean isFavorite() {
            return isFavorite;
        }
    }
    
    /**
     * Event fired when notes list should be refreshed.
     */
    public static class NotesRefreshRequestedEvent extends AppEvent {
        public NotesRefreshRequestedEvent() {
            super();
        }
    }
    
    /**
     * Event fired when note content changes.
     */
    public static class NoteContentChangedEvent extends AppEvent {
        private final Note note;
        private final String content;
        
        public NoteContentChangedEvent(Note note, String content) {
            this.note = note;
            this.content = content;
        }
        
        public Note getNote() {
            return note;
        }
        
        public String getContent() {
            return content;
        }
    }
}
