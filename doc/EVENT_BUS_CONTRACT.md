# EventBus Contract

## Objetivo
Definir reglas claras para publicación/suscripción y evitar efectos secundarios ambiguos.

## Reglas de publicación
- Los eventos mutables (`SystemActionEvent.SAVE`, `SystemActionEvent.DELETE`) deben tener **un único dueño** de ejecución.
- Un handler **no debe** republicar el mismo comando mutable que está procesando.
- `publish(...)` se usa para flujo UI-safe y despacha en hilo JavaFX.
- `publishSync(...)` se reserva para casos controlados (tests, lógica no-UI que requiere sincronía).

## Reglas de suscripción
- `subscribe(...)` siempre devuelve una `EventBus.Subscription` válida.
- En contexto de plugin sin bus disponible se devuelve `Subscription.NO_OP` (nunca `null`).
- Cada suscriptor debe cancelar su suscripción en su ciclo de vida de cierre cuando corresponda.

## Casos prohibidos
- Re-publicar `SAVE`/`DELETE` desde el mismo handler que los procesa.
- Ignorar excepciones de handlers con `printStackTrace()`.
- Asumir orden cross-thread fuera del contrato de JavaFX `runLater`.

## Observabilidad
- Errores de handlers se registran con `Logger` y stacktrace (`Level.SEVERE`).
- No se permite salida por consola directa para fallos del bus.
