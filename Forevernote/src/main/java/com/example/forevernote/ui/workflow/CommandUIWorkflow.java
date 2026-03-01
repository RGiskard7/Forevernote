package com.example.forevernote.ui.workflow;

import java.util.function.Consumer;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.ui.components.CommandPalette;
import com.example.forevernote.ui.components.QuickSwitcher;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Encapsulates command UI orchestration (palette/switcher and shortcuts).
 */
public class CommandUIWorkflow {

    public record CommandUiComponents(CommandPalette commandPalette, QuickSwitcher quickSwitcher) {
    }

    public CommandUiComponents ensureCommandUiComponents(
            Stage stage,
            CommandPalette existingPalette,
            QuickSwitcher existingSwitcher,
            Consumer<String> commandHandler,
            Consumer<Note> noteSelectionHandler) {
        if (stage == null) {
            return new CommandUiComponents(existingPalette, existingSwitcher);
        }

        CommandPalette palette = existingPalette;
        if (palette == null) {
            palette = new CommandPalette(stage);
            if (commandHandler != null) {
                palette.setCommandHandler(commandHandler);
            }
        }

        QuickSwitcher switcher = existingSwitcher;
        if (switcher == null) {
            switcher = new QuickSwitcher(stage);
            if (noteSelectionHandler != null) {
                switcher.setOnNoteSelected(noteSelectionHandler);
            }
        }

        return new CommandUiComponents(palette, switcher);
    }

    public void initializeKeyboardShortcuts(
            Scene scene,
            Runnable openPaletteAction,
            Runnable openQuickSwitcherAction,
            Consumer<String> infoLogger,
            Consumer<String> warningLogger) {
        if (scene == null) {
            if (warningLogger != null) {
                warningLogger.accept("Scene not available for keyboard shortcuts");
            }
            return;
        }

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case P:
                        if (openPaletteAction != null) {
                            openPaletteAction.run();
                            event.consume();
                        }
                        break;
                    case O:
                        if (openQuickSwitcherAction != null) {
                            openQuickSwitcherAction.run();
                            event.consume();
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        if (infoLogger != null) {
            infoLogger.accept("Keyboard shortcuts initialized (Ctrl+P: Command Palette, Ctrl+O: Quick Switcher)");
        }
    }
}
