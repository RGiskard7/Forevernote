package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.example.forevernote.ui.workflow.CommandRoutingWorkflow;
import com.example.forevernote.ui.workflow.CommandRoutingWorkflow.DispatchResult;

class CommandRoutingWorkflowTest {

    @Test
    void shouldResolveLegacyAliasAndExecuteRoute() {
        CommandRoutingWorkflow workflow = new CommandRoutingWorkflow();
        AtomicInteger counter = new AtomicInteger(0);
        workflow.registerRoute("cmd.save", "Save", counter::incrementAndGet);

        DispatchResult result = workflow.dispatch("Save", token -> false);

        assertTrue(result.handled());
        assertEquals("cmd.save", result.resolvedToken());
        assertEquals(1, counter.get());
    }

    @Test
    void shouldUseFallbackForUnknownRoutes() {
        CommandRoutingWorkflow workflow = new CommandRoutingWorkflow();
        DispatchResult result = workflow.dispatch("plugin.custom.action", token -> token.startsWith("plugin."));

        assertTrue(result.handled());
        assertEquals("plugin.custom.action", result.resolvedToken());
    }

    @Test
    void shouldReportUnknownWhenNoRouteOrFallback() {
        CommandRoutingWorkflow workflow = new CommandRoutingWorkflow();
        DispatchResult result = workflow.dispatch("missing", token -> false);

        assertFalse(result.handled());
        assertEquals("missing", result.resolvedToken());
    }
}
