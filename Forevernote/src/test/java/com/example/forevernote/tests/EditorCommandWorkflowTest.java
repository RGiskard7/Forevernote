package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.EditorCommandWorkflow;

class EditorCommandWorkflowTest {

    @Test
    void handleRedoShouldPublishLocalizedStatus() {
        EditorCommandWorkflow workflow = new EditorCommandWorkflow();
        AtomicReference<String> status = new AtomicReference<>();

        workflow.handleRedo(
                key -> "status.redo_not_available".equals(key) ? "Redo no disponible" : key,
                status::set);

        assertEquals("Redo no disponible", status.get());
    }

    @Test
    void handleReplaceWithNoOpenNoteShouldPublishLocalizedStatus() {
        EditorCommandWorkflow workflow = new EditorCommandWorkflow();
        AtomicReference<String> status = new AtomicReference<>();

        workflow.handleReplace(
                null,
                key -> "status.no_note_open".equals(key) ? "No hay nota abierta" : key,
                status::set);

        assertEquals("No hay nota abierta", status.get());
    }

    @Test
    void handleAttachmentNotSupportedShouldPublishLocalizedStatus() {
        EditorCommandWorkflow workflow = new EditorCommandWorkflow();
        AtomicReference<String> status = new AtomicReference<>();

        workflow.handleAttachmentNotSupported(
                key -> "status.attachments_not_supported".equals(key) ? "Adjuntos no soportados" : key,
                status::set);

        assertEquals("Adjuntos no soportados", status.get());
    }
}
