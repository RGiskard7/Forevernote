package com.example.forevernote.ui.workflow;

import java.util.List;
import java.util.function.Consumer;

import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.NoteEvents;
import com.example.forevernote.plugin.Plugin;
import com.example.forevernote.plugin.PluginLoader;
import com.example.forevernote.plugin.PluginManager;

/**
 * Encapsulates plugin loading and registration orchestration.
 */
public class PluginLifecycleWorkflow {

    public record LoadResult(int registeredCount, List<String> loadFailures) {
    }

    public LoadResult registerCoreAndExternalPlugins(PluginManager pluginManager, Consumer<String> warningLogger) {
        if (pluginManager == null) {
            return new LoadResult(0, List.of("PluginManager is null"));
        }

        pluginManager.registerPlugin(new com.example.forevernote.plugin.mermaid.MermaidPlugin());

        PluginLoader.PluginLoadReport pluginLoadReport = PluginLoader.loadExternalPluginsWithReport();
        int registeredCount = 0;

        for (Plugin plugin : pluginLoadReport.getPlugins()) {
            if (pluginManager.registerPlugin(plugin)) {
                registeredCount++;
            } else if (warningLogger != null) {
                warningLogger.accept("Failed to register external plugin: " + plugin.getName());
            }
        }

        return new LoadResult(registeredCount, pluginLoadReport.getFailures());
    }

    public void subscribePluginUiEvents(
            EventBus eventBus,
            Runnable refreshListsAction,
            Consumer<String> infoLogger) {
        if (eventBus == null) {
            return;
        }

        eventBus.subscribe(NoteEvents.NotesRefreshRequestedEvent.class, event -> {
            if (refreshListsAction != null) {
                refreshListsAction.run();
            }
            if (infoLogger != null) {
                infoLogger.accept("Refreshed notes from plugin request");
            }
        });
    }
}
