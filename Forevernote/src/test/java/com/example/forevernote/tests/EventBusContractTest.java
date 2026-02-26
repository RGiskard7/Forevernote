package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.example.forevernote.event.AppEvent;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.plugin.PluginContext;

class EventBusContractTest {

    private static final class DummyEvent extends AppEvent {
    }

    @Test
    void pluginContextSubscribeReturnsNoOpWhenEventBusIsMissing() {
        PluginContext context = new PluginContext(
                "test-plugin",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        EventBus.Subscription subscription = context.subscribe(DummyEvent.class, event -> {
        });
        assertNotNull(subscription);
        assertFalse(subscription.isCancelled());

        subscription.cancel();
        assertFalse(subscription.isCancelled());
    }

    @Test
    void noOpSubscriptionIsSafeAndIdempotent() {
        EventBus.Subscription subscription = EventBus.Subscription.NO_OP;
        assertNotNull(subscription);
        assertFalse(subscription.isCancelled());

        subscription.cancel();
        subscription.cancel();

        assertFalse(subscription.isCancelled());
    }
}
