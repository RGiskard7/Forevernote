# Event Bus Contract

## Principles

- Events represent state changes or commands with explicit semantics.
- Mutable command events (e.g., save/delete) must not be republished in loops.
- Subscriptions must always return a safe handle (no null subscription contract).

## Required Practices

1. Publish typed events only.
2. Avoid side effects in subscribers that trigger the same command path recursively.
3. Unsubscribe/cleanup on controller or plugin teardown.
4. Log exceptions via structured logger; avoid `printStackTrace()`.

## Prohibited Patterns

- Recursive publication of `SAVE`/`DELETE` command events.
- Returning null subscriptions from plugin or UI contexts.
- Swallowing exceptions in event handlers without logging.
