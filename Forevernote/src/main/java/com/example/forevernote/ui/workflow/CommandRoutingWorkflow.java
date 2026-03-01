package com.example.forevernote.ui.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Centralizes command routing with stable IDs and backward-compatible aliases.
 */
public class CommandRoutingWorkflow {

    public record DispatchResult(boolean handled, String resolvedToken) {
    }

    private final Map<String, Runnable> routes = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();

    public boolean isEmpty() {
        return routes.isEmpty();
    }

    public void registerRoute(String id, String legacyName, Runnable action) {
        routes.put(id, action);
        aliases.put(id, id);
        if (legacyName != null && !legacyName.isEmpty()) {
            aliases.put(legacyName, id);
        }
    }

    public void registerAlias(String alias, String commandId) {
        if (alias == null || alias.isEmpty() || commandId == null || commandId.isEmpty()) {
            return;
        }
        aliases.put(alias, commandId);
    }

    public String resolveToken(String commandToken) {
        if (commandToken == null) {
            return "";
        }
        return aliases.getOrDefault(commandToken, commandToken);
    }

    public DispatchResult dispatch(String commandToken, Predicate<String> fallbackExecutor) {
        String resolved = resolveToken(commandToken);
        Runnable route = routes.get(resolved);
        if (route != null) {
            route.run();
            return new DispatchResult(true, resolved);
        }

        boolean handledByFallback = false;
        if (fallbackExecutor != null) {
            handledByFallback = fallbackExecutor.test(commandToken)
                    || (!resolved.equals(commandToken) && fallbackExecutor.test(resolved));
        }
        return new DispatchResult(handledByFallback, resolved);
    }
}
