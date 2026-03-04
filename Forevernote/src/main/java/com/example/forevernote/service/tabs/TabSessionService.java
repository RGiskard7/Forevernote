package com.example.forevernote.service.tabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the open-note tab session state in memory.
 */
public class TabSessionService {

    public enum OpenMode {
        ACTIVATE_OR_OPEN,
        FORCE_NEW
    }

    public record NoteRef(String noteId, String title) {
    }

    public record TabState(String tabId, NoteRef noteRef, boolean dirty, boolean pinned) {
    }

    private final Map<String, TabState> tabsById = new LinkedHashMap<>();
    private final List<String> orderedTabIds = new ArrayList<>();
    private String activeTabId;
    private TabState lastClosed;

    public synchronized TabState openNote(NoteRef ref, OpenMode mode) {
        Objects.requireNonNull(ref, "ref");
        String noteId = ref.noteId();
        if (mode != OpenMode.FORCE_NEW && noteId != null && !noteId.isBlank()) {
            Optional<TabState> existing = tabsById.values().stream()
                    .filter(t -> Objects.equals(noteId, t.noteRef().noteId()))
                    .findFirst();
            if (existing.isPresent()) {
                activeTabId = existing.get().tabId();
                return existing.get();
            }
        }

        String tabId = UUID.randomUUID().toString();
        TabState state = new TabState(tabId, ref, false, false);
        tabsById.put(tabId, state);
        orderedTabIds.add(tabId);
        activeTabId = tabId;
        return state;
    }

    public synchronized boolean closeTab(String tabId) {
        TabState removed = tabsById.remove(tabId);
        if (removed == null) {
            return false;
        }
        orderedTabIds.remove(tabId);
        lastClosed = removed;
        if (Objects.equals(activeTabId, tabId)) {
            activeTabId = orderedTabIds.isEmpty() ? null : orderedTabIds.get(Math.max(0, orderedTabIds.size() - 1));
        }
        return true;
    }

    public synchronized void closeAllExcept(String tabId) {
        List<String> ids = new ArrayList<>(orderedTabIds);
        for (String id : ids) {
            if (!Objects.equals(id, tabId)) {
                closeTab(id);
            }
        }
        activateTab(tabId);
    }

    public synchronized void closeAll() {
        List<String> ids = new ArrayList<>(orderedTabIds);
        for (String id : ids) {
            closeTab(id);
        }
    }

    public synchronized Optional<TabState> reopenLastClosed() {
        if (lastClosed == null) {
            return Optional.empty();
        }
        TabState restored = new TabState(UUID.randomUUID().toString(), lastClosed.noteRef(), false, lastClosed.pinned());
        tabsById.put(restored.tabId(), restored);
        orderedTabIds.add(restored.tabId());
        activeTabId = restored.tabId();
        lastClosed = null;
        return Optional.of(restored);
    }

    public synchronized boolean activateTab(String tabId) {
        if (!tabsById.containsKey(tabId)) {
            return false;
        }
        activeTabId = tabId;
        return true;
    }

    public synchronized void markDirty(String tabId, boolean dirty) {
        TabState current = tabsById.get(tabId);
        if (current == null) {
            return;
        }
        tabsById.put(tabId, new TabState(current.tabId(), current.noteRef(), dirty, current.pinned()));
    }

    public synchronized void updateTabTitleForNote(String noteId, String title) {
        for (Map.Entry<String, TabState> entry : tabsById.entrySet()) {
            TabState tab = entry.getValue();
            if (!Objects.equals(tab.noteRef().noteId(), noteId)) {
                continue;
            }
            entry.setValue(new TabState(tab.tabId(), new NoteRef(noteId, title), tab.dirty(), tab.pinned()));
        }
    }

    public synchronized void pin(String tabId, boolean pinned) {
        TabState tab = tabsById.get(tabId);
        if (tab == null) {
            return;
        }
        tabsById.put(tabId, new TabState(tab.tabId(), tab.noteRef(), tab.dirty(), pinned));
    }

    public synchronized List<TabState> listTabs() {
        List<TabState> ordered = new ArrayList<>();
        for (String id : orderedTabIds) {
            TabState tab = tabsById.get(id);
            if (tab != null) {
                ordered.add(tab);
            }
        }
        return Collections.unmodifiableList(ordered);
    }

    public synchronized Optional<TabState> getActiveTab() {
        if (activeTabId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tabsById.get(activeTabId));
    }

    public synchronized Optional<TabState> findByNoteId(String noteId) {
        return tabsById.values().stream().filter(t -> Objects.equals(t.noteRef().noteId(), noteId)).findFirst();
    }

    public synchronized Optional<TabState> findByTabId(String tabId) {
        return Optional.ofNullable(tabsById.get(tabId));
    }

    public synchronized Optional<TabState> nextTab() {
        if (orderedTabIds.isEmpty()) {
            return Optional.empty();
        }
        int idx = Math.max(0, orderedTabIds.indexOf(activeTabId));
        int next = (idx + 1) % orderedTabIds.size();
        activeTabId = orderedTabIds.get(next);
        return Optional.ofNullable(tabsById.get(activeTabId));
    }

    public synchronized Optional<TabState> previousTab() {
        if (orderedTabIds.isEmpty()) {
            return Optional.empty();
        }
        int idx = Math.max(0, orderedTabIds.indexOf(activeTabId));
        int prev = (idx - 1 + orderedTabIds.size()) % orderedTabIds.size();
        activeTabId = orderedTabIds.get(prev);
        return Optional.ofNullable(tabsById.get(activeTabId));
    }
}
