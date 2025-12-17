package com.example.forevernote.util;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility class for managing keyboard shortcuts in the application.
 * Provides a centralized way to define and handle keyboard shortcuts.
 */
public class KeyboardShortcuts {
    
    private final Map<KeyCombination, Consumer<KeyEvent>> shortcuts = new HashMap<>();
    private Scene scene;
    private ShortcutHandler handler;
    
    /**
     * Creates a new KeyboardShortcuts instance.
     */
    public KeyboardShortcuts() {
        initializeDefaultShortcuts();
    }

    public KeyboardShortcuts(ShortcutHandler handler) {
        this.handler = handler;
        initializeDefaultShortcuts();
    }
    
    /**
     * Sets the scene for which shortcuts will be active.
     */
    public void setScene(Scene scene) {
        this.scene = scene;
        setupEventHandlers();
    }
    
    /**
     * Initializes default keyboard shortcuts for the application.
     */
    private void initializeDefaultShortcuts() {
        // File operations
        addShortcut(KeyCode.N, KeyCombination.CONTROL_DOWN, this::handleNewNote);
        addShortcut(KeyCode.S, KeyCombination.CONTROL_DOWN, this::handleSave);
        addShortcut(KeyCode.O, KeyCombination.CONTROL_DOWN, this::handleOpen);
        addShortcut(KeyCode.W, KeyCombination.CONTROL_DOWN, this::handleClose);
        addShortcut(KeyCode.Q, KeyCombination.CONTROL_DOWN, this::handleQuit);
        
        // Edit operations
        addShortcut(KeyCode.Z, KeyCombination.CONTROL_DOWN, this::handleUndo);
        addShortcut(KeyCode.Y, KeyCombination.CONTROL_DOWN, this::handleRedo);
        addShortcut(KeyCode.X, KeyCombination.CONTROL_DOWN, this::handleCut);
        addShortcut(KeyCode.C, KeyCombination.CONTROL_DOWN, this::handleCopy);
        addShortcut(KeyCode.V, KeyCombination.CONTROL_DOWN, this::handlePaste);
        addShortcut(KeyCode.A, KeyCombination.CONTROL_DOWN, this::handleSelectAll);
        addShortcut(KeyCode.F, KeyCombination.CONTROL_DOWN, this::handleFind);
        addShortcut(KeyCode.H, KeyCombination.CONTROL_DOWN, this::handleReplace);
        
        // View operations
        addShortcut(KeyCode.F11, KeyCombination.SHIFT_DOWN, this::handleToggleFullscreen);
        addShortcut(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN, this::handleViewFolders);
        addShortcut(KeyCode.DIGIT2, KeyCombination.CONTROL_DOWN, this::handleViewTags);
        addShortcut(KeyCode.DIGIT3, KeyCombination.CONTROL_DOWN, this::handleViewRecent);
        addShortcut(KeyCode.DIGIT4, KeyCombination.CONTROL_DOWN, this::handleViewFavorites);
        
        // Navigation
        addShortcut(KeyCode.LEFT, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, this::handleBack);
        addShortcut(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN, this::handleForward);
        addShortcut(KeyCode.UP, KeyCombination.CONTROL_DOWN, this::handlePreviousNote);
        addShortcut(KeyCode.DOWN, KeyCombination.CONTROL_DOWN, this::handleNextNote);
        
        // Note operations
        addShortcut(KeyCode.B, KeyCombination.CONTROL_DOWN, this::handleToggleBold);
        addShortcut(KeyCode.I, KeyCombination.CONTROL_DOWN, this::handleToggleItalic);
        addShortcut(KeyCode.U, KeyCombination.CONTROL_DOWN, this::handleToggleUnderline);
        addShortcut(KeyCode.K, KeyCombination.CONTROL_DOWN, this::handleInsertLink);
        addShortcut(KeyCode.D, KeyCombination.CONTROL_DOWN, this::handleDeleteNote);
        addShortcut(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN, this::handleToggleFavorite);
        
        // Search
        addShortcut(KeyCode.F, KeyCombination.CONTROL_DOWN, this::handleSearch);
        addShortcut(KeyCode.F3, this::handleFindNext);
        addShortcut(KeyCode.F3, KeyCombination.SHIFT_DOWN, this::handleFindPrevious);
        
        // Markdown specific
        addShortcut(KeyCode.M, KeyCombination.CONTROL_DOWN, this::handleToggleMarkdownMode);
        addShortcut(KeyCode.P, KeyCombination.CONTROL_DOWN, this::handleTogglePreview);
        addShortcut(KeyCode.E, KeyCombination.CONTROL_DOWN, this::handleToggleEditMode);
        
        // Quick actions
        addShortcut(KeyCode.T, KeyCombination.CONTROL_DOWN, this::handleNewFolder);
        addShortcut(KeyCode.R, KeyCombination.CONTROL_DOWN, this::handleRename);
        addShortcut(KeyCode.L, KeyCombination.CONTROL_DOWN, this::handleToggleSidebar);
        addShortcut(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, this::handleQuickAdd);
    }
    
    /**
     * Adds a keyboard shortcut.
     */
    public void addShortcut(KeyCode keyCode, KeyCombination.Modifier modifiers, Consumer<KeyEvent> handler) {
        KeyCombination combination = new KeyCodeCombination(keyCode, modifiers);
        shortcuts.put(combination, handler);
    }

    public void addShortcut(KeyCode keyCode, KeyCombination.Modifier m1, KeyCombination.Modifier m2, Consumer<KeyEvent> handler) {
        KeyCombination combination = new KeyCodeCombination(keyCode, m1, m2);
        shortcuts.put(combination, handler);
    }
    
    /**
     * Adds a keyboard shortcut without modifiers.
     */
    public void addShortcut(KeyCode keyCode, Consumer<KeyEvent> handler) {
        KeyCombination combination = new KeyCodeCombination(keyCode);
        shortcuts.put(combination, handler);
    }
    
    /**
     * Sets up event handlers for the scene.
     */
    private void setupEventHandlers() {
        if (scene == null) return;

        scene.setOnKeyPressed(event -> {
            for (Map.Entry<KeyCombination, Consumer<KeyEvent>> e : shortcuts.entrySet()) {
                KeyCombination kc = e.getKey();
                if (kc.match(event)) {
                    event.consume();
                    e.getValue().accept(event);
                    break;
                }
            }
        });
    }
    
    // Default shortcut handlers (these would be connected to actual controller methods)
    
    private void handleNewNote(KeyEvent event) {
        if (handler != null) handler.newNote();
    }
    
    private void handleSave(KeyEvent event) {
        if (handler != null) handler.save();
    }
    
    private void handleOpen(KeyEvent event) {
        if (handler != null) handler.open();
    }
    
    private void handleClose(KeyEvent event) {
        if (handler != null) handler.close();
    }
    
    private void handleQuit(KeyEvent event) {
        if (handler != null) handler.quit();
    }
    
    private void handleUndo(KeyEvent event) {
        if (handler != null) handler.undo();
    }
    
    private void handleRedo(KeyEvent event) {
        if (handler != null) handler.redo();
    }
    
    private void handleCut(KeyEvent event) {
        if (handler != null) handler.cut();
    }
    
    private void handleCopy(KeyEvent event) {
        if (handler != null) handler.copy();
    }
    
    private void handlePaste(KeyEvent event) {
        if (handler != null) handler.paste();
    }
    
    private void handleSelectAll(KeyEvent event) {
        if (handler != null) handler.selectAll();
    }
    
    private void handleFind(KeyEvent event) {
        if (handler != null) handler.find();
    }
    
    private void handleReplace(KeyEvent event) {
        if (handler != null) handler.replace();
    }
    
    private void handleToggleFullscreen(KeyEvent event) {
        if (handler != null) handler.toggleFullscreen();
    }
    
    private void handleViewFolders(KeyEvent event) {
        if (handler != null) handler.viewFolders();
    }
    
    private void handleViewTags(KeyEvent event) {
        if (handler != null) handler.viewTags();
    }
    
    private void handleViewRecent(KeyEvent event) {
        if (handler != null) handler.viewRecent();
    }
    
    private void handleViewFavorites(KeyEvent event) {
        if (handler != null) handler.viewFavorites();
    }
    
    private void handleBack(KeyEvent event) {
        if (handler != null) handler.back();
    }
    
    private void handleForward(KeyEvent event) {
        if (handler != null) handler.forward();
    }
    
    private void handlePreviousNote(KeyEvent event) {
        if (handler != null) handler.previousNote();
    }
    
    private void handleNextNote(KeyEvent event) {
        if (handler != null) handler.nextNote();
    }
    
    private void handleToggleBold(KeyEvent event) {
        if (handler != null) handler.toggleBold();
    }
    
    private void handleToggleItalic(KeyEvent event) {
        if (handler != null) handler.toggleItalic();
    }
    
    private void handleToggleUnderline(KeyEvent event) {
        if (handler != null) handler.toggleUnderline();
    }
    
    private void handleInsertLink(KeyEvent event) {
        if (handler != null) handler.insertLink();
    }
    
    private void handleDeleteNote(KeyEvent event) {
        if (handler != null) handler.deleteNote();
    }
    
    private void handleToggleFavorite(KeyEvent event) {
        if (handler != null) handler.toggleFavorite();
    }
    
    private void handleSearch(KeyEvent event) {
        if (handler != null) handler.focusSearch();
    }
    
    private void handleFindNext(KeyEvent event) {
        if (handler != null) handler.findNext();
    }
    
    private void handleFindPrevious(KeyEvent event) {
        if (handler != null) handler.findPrevious();
    }
    
    private void handleToggleMarkdownMode(KeyEvent event) {
        if (handler != null) handler.toggleMarkdownMode();
    }
    
    private void handleTogglePreview(KeyEvent event) {
        if (handler != null) handler.togglePreview();
    }
    
    private void handleToggleEditMode(KeyEvent event) {
        if (handler != null) handler.toggleEditMode();
    }
    
    private void handleNewFolder(KeyEvent event) {
        if (handler != null) handler.newFolder();
    }
    
    private void handleRename(KeyEvent event) {
        if (handler != null) handler.rename();
    }
    
    private void handleToggleSidebar(KeyEvent event) {
        if (handler != null) handler.toggleSidebar();
    }
    
    private void handleQuickAdd(KeyEvent event) {
        if (handler != null) handler.quickAdd();
    }
    
    /**
     * Gets a map of all shortcuts and their descriptions.
     */
    public Map<String, String> getShortcutDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        
        descriptions.put("Ctrl+N", "Nueva nota");
        descriptions.put("Ctrl+S", "Guardar");
        descriptions.put("Ctrl+O", "Abrir");
        descriptions.put("Ctrl+W", "Cerrar");
        descriptions.put("Ctrl+Q", "Salir");
        
        descriptions.put("Ctrl+Z", "Deshacer");
        descriptions.put("Ctrl+Y", "Rehacer");
        descriptions.put("Ctrl+X", "Cortar");
        descriptions.put("Ctrl+C", "Copiar");
        descriptions.put("Ctrl+V", "Pegar");
        descriptions.put("Ctrl+A", "Seleccionar todo");
        descriptions.put("Ctrl+F", "Buscar");
        descriptions.put("Ctrl+H", "Reemplazar");
        
        descriptions.put("Ctrl+1", "Ver carpetas");
        descriptions.put("Ctrl+2", "Ver etiquetas");
        descriptions.put("Ctrl+3", "Ver recientes");
        descriptions.put("Ctrl+4", "Ver favoritos");
        
        descriptions.put("Ctrl+Alt+←", "Atrás");
        descriptions.put("Ctrl+Alt+→", "Adelante");
        descriptions.put("Ctrl+↑", "Nota anterior");
        descriptions.put("Ctrl+↓", "Siguiente nota");
        
        descriptions.put("Ctrl+B", "Negrita");
        descriptions.put("Ctrl+I", "Cursiva");
        descriptions.put("Ctrl+U", "Subrayado");
        descriptions.put("Ctrl+K", "Insertar enlace");
        descriptions.put("Ctrl+D", "Eliminar nota");
        descriptions.put("Ctrl+Shift+F", "Alternar favorito");
        
        descriptions.put("F3", "Buscar siguiente");
        descriptions.put("Shift+F3", "Buscar anterior");
        
        descriptions.put("Ctrl+M", "Modo Markdown");
        descriptions.put("Ctrl+P", "Vista previa");
        descriptions.put("Ctrl+E", "Modo edición");
        
        descriptions.put("Ctrl+T", "Nueva carpeta");
        descriptions.put("Ctrl+R", "Renombrar");
        descriptions.put("Ctrl+L", "Alternar barra lateral");
        descriptions.put("Ctrl+Enter", "Añadir rápido");
        
        return descriptions;
    }
}