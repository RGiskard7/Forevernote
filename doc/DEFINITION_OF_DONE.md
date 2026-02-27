# Definition of Done (Hardening)

Este DoD aplica a cualquier cambio de arquitectura, persistencia, plugins, UI crítica o workflows.

## Criterios obligatorios
1. Build verde:
   - `mvn -f Forevernote/pom.xml clean test`
   - `mvn -f Forevernote/pom.xml -DskipTests clean package`
2. Compatibilidad preservada:
   - Sin breaking changes de storage (SQLite + FileSystem).
   - Sin breaking changes de API pública de plugins.
3. Regresión funcional cubierta:
   - Se añade o actualiza al menos un test cuando el cambio afecta lógica no trivial.
4. Smoke funcional validado:
   - Crear/editar/guardar nota.
   - Papelera (mover/restaurar).
   - Carpetas/subcarpetas.
   - Tags.
   - Tema.
   - Plugin Manager.
5. Calidad operacional:
   - Sin `catch` vacíos nuevos.
   - Sin `printStackTrace()` nuevo.
   - Logging accionable para errores.
6. Documentación mínima actualizada:
   - Changelog.
   - Documento técnico afectado (`ARCHITECTURE`, `PLUGINS`, ADR o equivalente).

## Criterios de rechazo
1. Cualquier loop de eventos en acciones mutables (`SAVE`, `DELETE`).
2. Dependencia de literales visibles para lógica de negocio (por ejemplo, `"All Notes"`).
3. Funcionalidad que solo funciona en un backend de storage.
4. Cambios que bloquean hilo UI en operaciones frecuentes.
