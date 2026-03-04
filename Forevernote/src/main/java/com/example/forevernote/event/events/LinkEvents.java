package com.example.forevernote.event.events;

import com.example.forevernote.event.AppEvent;

public final class LinkEvents {

    private LinkEvents() {
    }

    public static class NoteLinksChangedEvent extends AppEvent {
        private final String noteId;

        public NoteLinksChangedEvent(String noteId) {
            this.noteId = noteId;
        }

        public String getNoteId() {
            return noteId;
        }
    }
}
