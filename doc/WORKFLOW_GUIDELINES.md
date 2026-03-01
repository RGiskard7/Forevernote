# Workflow Guidelines (UI)

Objetivo: mantener `MainController` como composition root y evitar sobreingeniería.

## Cuándo crear un workflow

Crea un workflow solo si se cumple al menos una de estas condiciones:

1. El bloque de lógica ocupa ~40-60+ líneas y mezcla UI con orquestación.
2. El mismo patrón aparece en varios handlers/comandos.
3. Quieres proteger un contrato con guard tests de delegación.
4. El bloque tiene riesgo de regresión y se beneficia de pruebas unitarias aisladas.

## Cuándo NO crear un workflow

1. Lógica trivial de 1-5 líneas (simple forwarding).
2. Código estrictamente de binding FXML o wiring visual local.
3. Extracciones que solo cambian ubicación, sin reducir complejidad real.

## Reglas prácticas

1. `MainController` conserva: `@FXML`, estado de vista, inicialización y delegación.
2. Workflow conserva: orquestación de caso de uso (sin conocer IDs FXML concretos).
3. No introducir frameworks extra para DI o arquitectura.
4. Un workflow por responsabilidad clara (`FileCommand`, `EditorCommand`, `Navigation`, etc.).
5. Mantener APIs aditivas y backward-compatible.

## Checklist antes de fusionar

1. ¿Baja complejidad/ciclocomática de `MainController` de forma medible?
2. ¿Se mantuvo comportamiento observable (UI/eventos/plugins/storage)?
3. ¿Hay tests/guards que aseguren la delegación?
4. ¿`mvn -f Forevernote/pom.xml test` en verde?

