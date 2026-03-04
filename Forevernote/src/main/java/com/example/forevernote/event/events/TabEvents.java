package com.example.forevernote.event.events;

import com.example.forevernote.event.AppEvent;

public final class TabEvents {

    private TabEvents() {
    }

    public static class TabStateChangedEvent extends AppEvent {
        public TabStateChangedEvent() {
            super("Tabs");
        }
    }
}
