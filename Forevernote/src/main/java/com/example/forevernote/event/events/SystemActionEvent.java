package com.example.forevernote.event.events;

import com.example.forevernote.event.AppEvent;

/**
 * Event fired when a broad system action is requested from the UI (Menu or
 * Toolbar).
 */
public class SystemActionEvent extends AppEvent {
    public enum ActionType {
        NEW_NOTE,
        NEW_FOLDER,
        NEW_TAG,
        SAVE,
        SAVE_ALL,
        DELETE,
        IMPORT,
        EXPORT,
        EXIT,
        UNDO,
        REDO,
        CUT,
        COPY,
        PASTE,
        FIND,
        REPLACE,
        TOGGLE_SIDEBAR,
        TOGGLE_NOTES_LIST,
        SWITCH_LAYOUT,
        ZOOM_IN,
        ZOOM_OUT,
        RESET_ZOOM,
        ZOOM_EDITOR_IN,
        ZOOM_EDITOR_OUT,
        RESET_EDITOR_ZOOM,
        LIST_VIEW,
        GRID_VIEW,
        TAGS_MANAGER,
        PLUGIN_MANAGER,
        PREFERENCES,
        SWITCH_STORAGE,
        DOCUMENTATION,
        ABOUT,
        SORT_FOLDERS,
        EXPAND_ALL_FOLDERS,
        COLLAPSE_ALL_FOLDERS,
        SORT_TAGS,
        SORT_RECENT,
        SORT_FAVORITES,
        SORT_TRASH,
        EMPTY_TRASH,
        REFRESH_NOTES,
        TOGGLE_TAGS,
        EDITOR_ONLY_MODE,
        SPLIT_VIEW_MODE,
        PREVIEW_ONLY_MODE,
        TOGGLE_PIN,
        TOGGLE_FAVORITE,
        TOGGLE_RIGHT_PANEL,
        HEADING1,
        HEADING2,
        HEADING3,
        BOLD,
        ITALIC,
        STRIKE,
        UNDERLINE,
        HIGHLIGHT,
        LINK,
        IMAGE,
        TODO_LIST,
        BULLET_LIST,
        NUMBERED_LIST,
        QUOTE,
        CODE
    }

    private final ActionType actionType;

    public SystemActionEvent(ActionType actionType) {
        this.actionType = actionType;
    }

    public ActionType getActionType() {
        return actionType;
    }
}
