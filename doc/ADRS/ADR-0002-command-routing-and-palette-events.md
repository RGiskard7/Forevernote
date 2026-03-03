# ADR-0002: Command Routing and Palette Events

- Status: Accepted
- Date: 2026-03-03

## Context

The UI had growing command handling complexity in `MainController`, with risk of duplicated routing and command regressions in palette execution.

## Decision

- Keep command IDs and aliases backward compatible.
- Consolidate routing setup into dedicated workflow-level registration helpers.
- Ensure command palette and legacy routes use a unified command registry path.

## Consequences

- Reduced routing drift risk.
- Easier guard testing for command compatibility.
- MainController still large, but routing logic is less fragmented.
