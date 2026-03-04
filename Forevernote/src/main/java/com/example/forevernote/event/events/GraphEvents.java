package com.example.forevernote.event.events;

import com.example.forevernote.event.AppEvent;

public final class GraphEvents {

    private GraphEvents() {
    }

    public static class GraphSelectionChangedEvent extends AppEvent {
        private final String noteId;

        public GraphSelectionChangedEvent(String noteId) {
            super("Graph");
            this.noteId = noteId;
        }

        public String getNoteId() {
            return noteId;
        }
    }
}
