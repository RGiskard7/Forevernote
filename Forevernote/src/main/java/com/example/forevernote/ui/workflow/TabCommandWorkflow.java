package com.example.forevernote.ui.workflow;

import com.example.forevernote.service.tabs.TabSessionService;
import com.example.forevernote.service.tabs.TabSessionService.TabState;

import java.util.Optional;

/**
 * Small orchestration helpers for tab commands.
 */
public class TabCommandWorkflow {

    public Optional<TabState> next(TabSessionService tabSessionService) {
        if (tabSessionService == null) {
            return Optional.empty();
        }
        return tabSessionService.nextTab();
    }

    public Optional<TabState> previous(TabSessionService tabSessionService) {
        if (tabSessionService == null) {
            return Optional.empty();
        }
        return tabSessionService.previousTab();
    }

    public boolean closeCurrent(TabSessionService tabSessionService) {
        if (tabSessionService == null) {
            return false;
        }
        return tabSessionService.getActiveTab().map(active -> tabSessionService.closeTab(active.tabId())).orElse(false);
    }

    public void closeOthers(TabSessionService tabSessionService) {
        if (tabSessionService == null) {
            return;
        }
        tabSessionService.getActiveTab().ifPresent(active -> tabSessionService.closeAllExcept(active.tabId()));
    }

    public void closeAll(TabSessionService tabSessionService) {
        if (tabSessionService == null) {
            return;
        }
        tabSessionService.closeAll();
    }
}
