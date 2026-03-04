package com.example.forevernote.tests;

import com.example.forevernote.service.tabs.TabSessionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabSessionServiceTest {

    @Test
    void openActivateCloseAndDirtyFlowWorks() {
        TabSessionService service = new TabSessionService();

        var t1 = service.openNote(new TabSessionService.NoteRef("n1", "Note 1"), TabSessionService.OpenMode.ACTIVATE_OR_OPEN);
        var t2 = service.openNote(new TabSessionService.NoteRef("n2", "Note 2"), TabSessionService.OpenMode.ACTIVATE_OR_OPEN);

        assertEquals(2, service.listTabs().size());
        assertEquals(t2.tabId(), service.getActiveTab().orElseThrow().tabId());

        service.activateTab(t1.tabId());
        service.markDirty(t1.tabId(), true);
        assertTrue(service.findByTabId(t1.tabId()).orElseThrow().dirty());

        service.closeTab(t1.tabId());
        assertEquals(1, service.listTabs().size());

        service.reopenLastClosed();
        assertEquals(2, service.listTabs().size());
    }
}
