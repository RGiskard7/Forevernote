# ADR-0002: Command Routing by Stable IDs + UI Event Wiring for Command Palette

## Estado
Aceptado - 2026-02-27

## Contexto
El Command Palette dependía de nombres visibles de comandos (strings en inglés) y de un switch monolítico en `MainController`. Además, el botón de toolbar publicaba `UIEvents.ShowCommandPaletteEvent`, pero `MainController` no estaba suscrito a ese evento, causando que la paleta no apareciera.

## Decisión
1. Mantener IDs estables de comandos (`cmd.*`) en `CommandPalette.Command`.
2. Despachar comandos en `MainController` usando tabla de rutas (`Map<String, Runnable>`) y alias backward-compatible.
3. Suscribir `MainController` a `UIEvents.ShowCommandPaletteEvent` y `UIEvents.ShowQuickSwitcherEvent`.
4. Inicialización lazy/robusta de Command Palette y Quick Switcher al mostrarse.

## Consecuencias
- Menor acoplamiento a textos visibles e idioma.
- Menor complejidad ciclomática en ejecución de comandos.
- Compatibilidad con comandos legacy y plugins externos conservada.
- Se elimina el fallo funcional de apertura del Command Palette desde toolbar/eventos.

## Verificación
- `mvn -f Forevernote/pom.xml clean test`
- `mvn -f Forevernote/pom.xml -DskipTests clean package`
- Apertura de Command Palette por toolbar, `Ctrl+P` y `Ctrl+Shift+P`.
