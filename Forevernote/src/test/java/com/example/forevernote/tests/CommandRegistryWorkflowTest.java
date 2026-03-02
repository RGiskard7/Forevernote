package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.CommandRegistryWorkflow;
import com.example.forevernote.ui.workflow.CommandRoutingWorkflow;
import com.example.forevernote.ui.workflow.CommandRoutingWorkflow.DispatchResult;

class CommandRegistryWorkflowTest {

    @Test
    void registerDefaultRoutesShouldSupportStableIdsLegacyNamesAndCustomAlias() {
        CommandRegistryWorkflow registry = new CommandRegistryWorkflow();
        CommandRoutingWorkflow routing = new CommandRoutingWorkflow();
        AtomicReference<String> executed = new AtomicReference<>();

        registry.registerDefaultRoutes(
                routing::registerRoute,
                routing::registerAlias,
                commandId -> () -> executed.set(commandId));

        DispatchResult legacySave = routing.dispatch("Save", token -> false);
        assertTrue(legacySave.handled());
        assertEquals("cmd.save", legacySave.resolvedToken());
        assertEquals("cmd.save", executed.get());

        DispatchResult stableId = routing.dispatch("cmd.goto_all_notes", token -> false);
        assertTrue(stableId.handled());
        assertEquals("cmd.goto_all_notes", stableId.resolvedToken());
        assertEquals("cmd.goto_all_notes", executed.get());

        DispatchResult customAlias = routing.dispatch("Toggle Right Panel", token -> false);
        assertTrue(customAlias.handled());
        assertEquals("cmd.toggle_info_panel", customAlias.resolvedToken());
        assertEquals("cmd.toggle_info_panel", executed.get());
    }

    @Test
    void registerDefaultRoutesShouldSkipCommandsWithoutAction() {
        CommandRegistryWorkflow registry = new CommandRegistryWorkflow();
        CommandRoutingWorkflow routing = new CommandRoutingWorkflow();

        registry.registerDefaultRoutes(
                routing::registerRoute,
                routing::registerAlias,
                commandId -> "cmd.save".equals(commandId) ? () -> {
                } : null);

        DispatchResult save = routing.dispatch("cmd.save", token -> false);
        assertTrue(save.handled());

        DispatchResult export = routing.dispatch("cmd.export", token -> false);
        assertFalse(export.handled());
    }

    @Test
    void registerDefaultRoutesShouldDoNothingWhenRequiredCollaboratorsAreMissing() {
        CommandRegistryWorkflow registry = new CommandRegistryWorkflow();
        CommandRoutingWorkflow routing = new CommandRoutingWorkflow();

        registry.registerDefaultRoutes(null, routing::registerAlias, commandId -> () -> {
        });
        assertTrue(routing.isEmpty());

        registry.registerDefaultRoutes(routing::registerRoute, routing::registerAlias, null);
        assertTrue(routing.isEmpty());
    }
}
