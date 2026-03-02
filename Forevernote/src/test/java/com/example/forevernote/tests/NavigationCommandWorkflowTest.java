package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.ui.workflow.NavigationCommandWorkflow;

class NavigationCommandWorkflowTest {

    @Test
    void refreshByContextShouldRouteToFolderTagSearchAndDefaultHandlers() {
        NavigationCommandWorkflow workflow = new NavigationCommandWorkflow();

        Folder folder = new Folder("Inbox");
        folder.setId("inbox");
        Tag tag = new Tag("work");
        tag.setTitle("work");

        AtomicInteger refreshCalls = new AtomicInteger(0);
        AtomicReference<String> selectedFolderId = new AtomicReference<>();
        AtomicReference<String> selectedTagTitle = new AtomicReference<>();
        AtomicReference<String> searchQuery = new AtomicReference<>();
        AtomicInteger errors = new AtomicInteger(0);

        workflow.refreshByContext(
                "folder",
                folder,
                null,
                null,
                null,
                null,
                null,
                refreshCalls::incrementAndGet,
                f -> selectedFolderId.set(f.getId()),
                selectedTagTitle::set,
                ignored -> "abc",
                searchQuery::set,
                key -> key,
                msg -> {
                },
                ex -> errors.incrementAndGet());
        assertEquals("inbox", selectedFolderId.get());
        assertEquals(0, refreshCalls.get());

        workflow.refreshByContext(
                "tag",
                null,
                tag,
                null,
                null,
                null,
                null,
                refreshCalls::incrementAndGet,
                f -> selectedFolderId.set(f.getId()),
                selectedTagTitle::set,
                ignored -> "abc",
                searchQuery::set,
                key -> key,
                msg -> {
                },
                ex -> errors.incrementAndGet());
        assertEquals("work", selectedTagTitle.get());

        workflow.refreshByContext(
                "search",
                null,
                null,
                null,
                null,
                null,
                null,
                refreshCalls::incrementAndGet,
                f -> selectedFolderId.set(f.getId()),
                selectedTagTitle::set,
                ignored -> "query",
                searchQuery::set,
                key -> key,
                msg -> {
                },
                ex -> errors.incrementAndGet());
        assertEquals("query", searchQuery.get());

        workflow.refreshByContext(
                "all",
                null,
                null,
                null,
                null,
                null,
                null,
                refreshCalls::incrementAndGet,
                f -> selectedFolderId.set(f.getId()),
                selectedTagTitle::set,
                ignored -> "",
                searchQuery::set,
                key -> key,
                msg -> {
                },
                ex -> errors.incrementAndGet());
        assertEquals(1, refreshCalls.get());
        assertEquals(0, errors.get());
    }
}
