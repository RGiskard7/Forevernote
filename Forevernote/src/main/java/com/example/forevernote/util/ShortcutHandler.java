package com.example.forevernote.util;

/**
 * Callback interface for keyboard shortcut actions.
 */
public interface ShortcutHandler {
    void newNote();
    void save();
    void open();
    void close();
    void quit();

    void undo();
    void redo();
    void cut();
    void copy();
    void paste();
    void selectAll();
    void find();
    void replace();

    void toggleFullscreen();
    void viewFolders();
    void viewTags();
    void viewRecent();
    void viewFavorites();

    void back();
    void forward();
    void previousNote();
    void nextNote();

    void toggleBold();
    void toggleItalic();
    void toggleUnderline();
    void insertLink();
    void deleteNote();
    void toggleFavorite();

    void focusSearch();
    void findNext();
    void findPrevious();

    void toggleMarkdownMode();
    void togglePreview();
    void toggleEditMode();

    void newFolder();
    void rename();
    void toggleSidebar();
    void quickAdd();
}