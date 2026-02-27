# Release & Rollback Playbook

## Objetivo
Desplegar cambios por fases sin dejar la app rota y con rollback claro.

## Pre-release gate (obligatorio)
1. `mvn -f Forevernote/pom.xml clean test`
2. `mvn -f Forevernote/pom.xml -DskipTests clean package`
3. Smoke manual mínimo:
   - Crear/editar/guardar nota
   - Papelera: mover/restaurar/eliminar
   - Crear carpeta/subcarpeta
   - Tags (crear/asignar/quitar)
   - Tema (light/dark/system)
   - Abrir Command Palette y Plugin Manager
4. Validación dual de storage: SQLite + FileSystem.

## Estrategia de release
1. Congelar cambios de fase en una rama de release.
2. Ejecutar gate completo y documentar resultados.
3. Publicar artefacto (`forevernote-1.0.0-uber.jar`) con changelog asociado.
4. Monitorear logs iniciales de arranque y acciones críticas.

## Criterios de rollback inmediato
- Fallo de arranque
- Corrupción o pérdida de notas
- Imposibilidad de guardar/restaurar
- Bloqueo funcional de navegación principal

## Procedimiento de rollback
1. Volver al último tag/commit estable publicado.
2. Recompilar artefacto estable.
3. Verificar gate mínimo (test + package + smoke reducido).
4. Comunicar incidente y causa raíz.
5. Abrir hotfix aislado con reproducción y prueba de regresión.

## Post-release
1. Registrar hallazgos en ADR/changelog.
2. Añadir test de regresión si aplica.
3. Actualizar estado de fases de hardening.
