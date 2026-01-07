# Changelog - Forevernote

## 📅 2026-01-08 — Corrección de warnings CSS y verificación final

### Resumen
Corrección de todos los warnings de CSS relacionados con variables de radius y verificación exhaustiva de funcionalidades.

### Correcciones Técnicas

1. **Warnings de CSS eliminados**
   - ❌ **Antes**: `ClassCastException` al usar variables CSS (`-fx-radius-md`, `-fx-radius-lg`) en `-fx-background-radius` y `-fx-border-radius`
   - ✅ **Ahora**: Todas las variables reemplazadas por valores directos en píxeles (`6px`, `8px`, `12px`)
   - ✅ Aplicado a ambos temas (light y dark)

2. **Verificación de funcionalidades**
   - ✅ Todas las funcionalidades core están completamente implementadas
   - ✅ 20 funcionalidades principales funcionando correctamente
   - ✅ Build sin errores ni warnings críticos

### Estado del Proyecto

**Funcionalidades completas (20):**
- ✅ Gestión de notas (crear, editar, eliminar, favoritos, recientes)
- ✅ Gestión de carpetas y subcarpetas
- ✅ Sistema de tags completo
- ✅ Editor Markdown con preview en tiempo real
- ✅ Vista Obsidian-style (split horizontal, modos de vista)
- ✅ Temas (claro, oscuro, sistema)
- ✅ Búsqueda y ordenamiento
- ✅ Import/Export de archivos
- ✅ Find/Replace, Undo/Redo, Zoom

**Funcionalidades futuras (no críticas):**
- 🔶 **Adjuntos de archivos** - Requiere sistema de almacenamiento de archivos (arquitectura adicional)
- 🔶 **Vista grilla** - Botón presente pero sin handler (funcionalidad estética)

### Archivos Modificados
- `modern-theme.css` - Reemplazo de variables CSS por valores directos
- `dark-theme.css` - Reemplazo de variables CSS por valores directos

---

## 📅 2026-01-07 — Corrección de bugs: Tags y Notas Recientes

### Resumen
Corrección de bugs críticos donde las notas eliminadas seguían apareciendo en "Recientes" y los tags no podían ser eliminados de las notas.

### Problemas Corregidos

1. **Notas eliminadas aparecían en Recientes y Favoritos**
   - ❌ **Antes**: Al eliminar una nota, seguía mostrándose en las listas de Recientes y Favoritos
   - ✅ **Ahora**: `handleDelete()` llama a `loadRecentNotes()` y `loadFavorites()` después de eliminar

2. **Tags no se podían eliminar de las notas**
   - ❌ **Antes**: El doble-click no funcionaba correctamente debido a referencia incorrecta del objeto Tag
   - ✅ **Ahora**: Tags tienen botón "×" visible para eliminar + el doble-click funciona correctamente
   - ✅ Se guarda `tagId` y `tagTitle` en variables finales para asegurar referencia correcta

3. **Notas recientes no se actualizaban al crear nota**
   - ❌ **Antes**: Al crear una nueva nota, no aparecía en Recientes
   - ✅ **Ahora**: `handleNewNote()` llama a `loadRecentNotes()` después de crear

### Mejoras UX (estilo Evernote/Obsidian)

- ✅ **Botón "×" en tags**: Cada tag tiene un botón visible para eliminar (más intuitivo)
- ✅ **Diálogo de confirmación**: Al eliminar un tag se muestra confirmación
- ✅ **Botón "+ Add Tag" con estilo**: Borde punteado, cambia color al hover

### CSS Actualizado

- ✅ **`.tag-container`**: Contenedor para tag + botón de eliminar
- ✅ **`.tag-remove-btn`**: Botón "×" minimalista que cambia a rojo en hover
- ✅ **`.add-tag-button`**: Estilo para botón de agregar tag
- ✅ Estilos aplicados a ambos temas (light y dark)

### Archivos Modificados
- `MainController.java` - Correcciones de refresh y nuevo UI de tags
- `modern-theme.css` - Estilos para tag-container y tag-remove-btn
- `dark-theme.css` - Estilos dark para tag-container y tag-remove-btn

---

## 📅 2026-01-07 — Rediseño Obsidian-style del editor y preview

### Resumen
Rediseño completo de la experiencia de edición para que sea similar a Obsidian: editor y preview lado a lado, fácilmente redimensionable, con botones de toggle para cambiar entre modos de vista.

### Cambios Principales

1. **Nueva estructura de Editor/Preview (estilo Obsidian)**
   - ✅ Editor y preview ahora están lado a lado (horizontal) en lugar de arriba/abajo
   - ✅ SplitPane redimensionable arrastrando el divisor
   - ✅ Divisor visual que cambia de color al hover (indica que es arrastrable)

2. **Botones de Toggle para modos de vista**
   - ✅ `✏️` - Modo solo editor (oculta preview)
   - ✅ `◧` - Modo split view (editor + preview, por defecto)
   - ✅ `👁` - Modo solo preview (oculta editor)
   - ✅ Los modos se aplican instantáneamente

3. **Panel de Info deslizable**
   - ✅ Nuevo botón `ℹ` para mostrar/ocultar panel de información
   - ✅ Panel lateral con estadísticas: palabras, caracteres, fechas
   - ✅ Información de ubicación (latitud/longitud)
   - ✅ Información de fuente (autor, URL)
   - ✅ Lista de adjuntos compacta

4. **Header unificado y minimalista**
   - ✅ Título de nota con estilo limpio
   - ✅ Barra de tags debajo del título
   - ✅ Fecha de modificación sutil
   - ✅ Iconos de acción (favorito, info, eliminar)

5. **Toolbar de formato minimalista**
   - ✅ Botones más pequeños y compactos
   - ✅ Tooltips con atajos de teclado
   - ✅ Contador de palabras discreto

6. **CSS actualizado para ambos temas**
   - ✅ `modern-theme.css` - Estilos Obsidian-style para tema light
   - ✅ `dark-theme.css` - Estilos Obsidian-style para tema dark
   - ✅ Cursores correctos (`col-resize` para divisores)
   - ✅ Estilos para todos los nuevos componentes

### Archivos Modificados
- `MainView.fxml` - Estructura completamente rediseñada
- `MainController.java` - Nuevos métodos de toggle y manejo de vista
- `modern-theme.css` - Estilos Obsidian-style agregados
- `dark-theme.css` - Estilos Obsidian-style agregados

### Mejoras UX
- ✅ Más fácil redimensionar el panel de preview
- ✅ Cambio rápido entre modos de vista
- ✅ Interfaz más limpia y menos saturada
- ✅ Panel de info accesible sin ocupar espacio permanente
- ✅ Experiencia similar a editores modernos como Obsidian/Typora

---

## 📅 2026-01-07 — Mejoras profesionales de UI y corrección de bugs

### Resumen
Análisis exhaustivo del proyecto con mejoras significativas en la interfaz, corrección de bugs críticos, e implementación de funcionalidades faltantes.

### Análisis Realizado
- 12 funcionalidades completamente implementadas
- 11 funcionalidades faltantes o parciales identificadas
- 5 bugs corregidos
- 6 problemas de UI/UX resueltos

### Cambios

1. **CSS Profesional - Reescritura Completa**
   - ✅ `modern-theme.css` - Tema light completamente rediseñado
   - ✅ `dark-theme.css` - Tema dark profesional
   - ✅ Sistema de variables CSS para colores, tipografía, espaciado
   - ✅ Sombras y bordes profesionales
   - ✅ Scrollbars minimalistas y elegantes
   - ✅ Estilos mejorados para botones, inputs, tabs

2. **Corrección de Bugs**
   - ✅ **Listeners duplicados**: `loadRecentNotes()` y `loadFavorites()` ya no recrean listeners
   - ✅ **Null pointer exceptions**: Comparaciones null-safe en `sortNotes()`
   - ✅ **Persistencia de tema**: El tema se guarda con `java.util.prefs.Preferences`
   - ✅ **Código muerto eliminado**: Métodos no usados removidos

3. **Nuevas Funcionalidades**
   - ✅ **Import**: Importación funcional de archivos .md, .txt, .markdown
   - ✅ **Export**: Exportación funcional con FileChooser a .md o .txt
   - ✅ **About dialog**: Diálogo profesional con versión, descripción, y créditos

4. **Calidad de Código**
   - ✅ Todos los warnings de lint resueltos
   - ✅ Imports no usados eliminados
   - ✅ Manejo correcto de nulls

### Archivos Modificados
- `modern-theme.css` - Reescritura completa
- `dark-theme.css` - Reescritura completa
- `MainController.java` - Bugs corregidos, nuevas funcionalidades
- `AGENTS.md` - Documentación actualizada

---

## 📅 2025-12-18 — Badges y mejoras visuales en README

### Resumen
Añadidos badges estándar de tecnologías y licencia al README. Banner actualizado para ocupar todo el ancho.

### Cambios

1. **`README.md`**
   - ✅ Banner actualizado para ocupar 100% del ancho con `style="width: 100%; max-width: 100%;"`
   - ✅ Añadidos badges estándar:
     - Licencia MIT
     - Java 17+
     - JavaFX 21
     - SQLite 3
     - Maven 3.6+
     - Plataformas (Windows, macOS, Linux)
   - ✅ Badges colocados en posición estándar (después del banner, antes de la descripción)
   - ✅ Todos los badges son clicables y enlazan a páginas oficiales

---

## 📅 2025-12-18 — Centralización de metadata: app.properties para rebranding fácil

### Resumen
Implementado sistema centralizado de configuración para facilitar el cambio de nombre, icono y metadata de la aplicación. Similar a un "manifest" de Android, todo está en un solo archivo. El icono de la ventana también se lee desde `app.properties`.

### Cambios

1. **`Forevernote/src/main/resources/app.properties`** (NUEVO)
   - ✅ Archivo centralizado con toda la metadata de la aplicación
   - ✅ Nombre, versión, vendor, descripción, copyright
   - ✅ Título de ventana
   - ✅ Rutas de iconos por plataforma (Windows, macOS, Linux)
   - ✅ Categorías de paquetes por plataforma

2. **`Forevernote/src/main/java/com/example/forevernote/AppConfig.java`** (NUEVO)
   - ✅ Clase helper para leer `app.properties`
   - ✅ Métodos estáticos para acceder a toda la metadata
   - ✅ Valores por defecto si el archivo no existe

3. **`Forevernote/src/main/java/com/example/forevernote/Main.java`**
   - ✅ Usa `AppConfig.getWindowTitle()` en lugar de string hardcodeado
   - ✅ Usa `AppConfig.getWindowIconPath()` para el icono de la ventana (barra de tareas)

4. **`Forevernote/src/main/java/com/example/forevernote/AppDataDirectory.java`**
   - ✅ Usa `AppConfig.getAppName()` en lugar de constante hardcodeada

5. **Scripts de packaging** (3 archivos)
   - ✅ `scripts/package-windows.ps1`: Lee `app.properties` y usa variables
   - ✅ `scripts/package-macos.sh`: Lee `app.properties` y usa variables
   - ✅ `scripts/package-linux.sh`: Lee `app.properties` y usa variables
   - ✅ Soporte para iconos: añade `--icon` si el archivo existe
   - ✅ Todos los valores (nombre, versión, vendor, etc.) vienen de `app.properties`

6. **`Forevernote/src/main/resources/icons/`** (NUEVO)
   - ✅ Carpeta para iconos de la aplicación
   - ✅ `README.md` con instrucciones de formatos requeridos

### Cómo cambiar nombre e icono

**Para cambiar el nombre de la aplicación:**
1. Edita `Forevernote/src/main/resources/app.properties`
2. Cambia `app.name=TuNuevoNombre`
3. Recompila y empaqueta

**Para cambiar el icono del ejecutable (jpackage):**
1. Coloca tus iconos en `Forevernote/src/main/resources/icons/`:
   - Windows: `app-icon.ico`
   - macOS: `app-icon.icns`
   - Linux: `app-icon.png`
2. Los scripts detectarán automáticamente los iconos

**Para cambiar el icono de la ventana (barra de tareas):**
1. Coloca tu icono PNG en `Forevernote/src/main/resources/com/example/forevernote/ui/images/app-icon.png`
2. O modifica `app.icon.window` en `app.properties` para usar otra ruta
3. El icono se cargará automáticamente al iniciar la aplicación

### Ventajas

- ✅ **Un solo archivo para cambiar todo**: `app.properties`
- ✅ **Estándar y profesional**: Similar a manifest de Android
- ✅ **Sin hardcodeos**: Todo viene de configuración
- ✅ **Fácil rebranding**: Cambia un archivo y recompila

---

## 📅 2025-12-18 — Limpieza y simplificación: AppDataDirectory, LoggerConfig, Main

### Resumen
Simplificación del código para manejo de directorios de datos. Eliminada complejidad innecesaria.

### Cambios

1. **`AppDataDirectory.java`** - Simplificado
   - ✅ Eliminado método `isMacOSAppBundle()` innecesario
   - ✅ Lógica clara: primero relativo, si falla usa directorio del SO
   - ✅ Windows fallback: `%APPDATA%\Forevernote\`
   - ✅ macOS fallback: `~/Library/Application Support/Forevernote/`
   - ✅ Linux fallback: `~/.config/Forevernote/` (XDG Base Directory)

2. **`LoggerConfig.java`** - Simplificado
   - ✅ Eliminada lógica especial de macOS
   - ✅ Siempre usa ruta absoluta de `AppDataDirectory.getLogsDirectory()`
   - ✅ NO crea directorios (eso lo hace Main)

3. **`Main.java`** - Simplificado
   - ✅ Bloque estático crea directorios antes de cargar logger
   - ✅ Código más limpio y legible

4. **`.vscode/launch.json`**
   - ✅ Añadido `"cwd": "${workspaceFolder}/Forevernote"` para correcto directorio de trabajo

### Responsabilidades claras

| Clase | Responsabilidad |
|-------|-----------------|
| `AppDataDirectory` | Determina DÓNDE van data/ y logs/ |
| `Main` | Crea los directorios al arrancar |
| `LoggerConfig` | Configura logging (NO crea directorios) |

### Comportamiento

- **Desarrollo (VSCode)**: `data/` y `logs/` en `Forevernote/`
- **.exe empaquetado**: `data/` y `logs/` junto al .exe
- **Si no puede escribir**: usa directorio estándar del SO

---

## 📅 2025-12-18 — Scripts de packaging multiplataforma con jpackage

### Resumen
Implementación completa de scripts de packaging para Windows, macOS y Linux usando `jpackage`. Solución estándar para JavaFX con clase `Launcher`.

### Cambios

1. **`Launcher.java`** (NUEVO)
   - ✅ Clase estándar para `jpackage` con JavaFX
   - ✅ NO extiende `Application`, simplemente llama a `Main.main(args)`
   - ✅ Patrón recomendado por Oracle para aplicaciones JavaFX empaquetadas

2. **`scripts/package-windows.ps1`**
   - ✅ Genera instalador MSI o app-image (portable)
   - ✅ Usa `--main-class com.example.forevernote.Launcher`
   - ✅ Detecta WiX Toolset para MSI, fallback a app-image
   - ✅ Usa directorio temporal para evitar recursión en estructura
   - ✅ Maneja rutas largas de Windows

3. **`scripts/package-macos.sh`**
   - ✅ Genera instalador DMG con aplicación .app nativa
   - ✅ Detecta arquitectura (Intel vs Apple Silicon)
   - ✅ Busca JARs específicos de plataforma de JavaFX
   - ✅ Opciones Java recomendadas para macOS

4. **`scripts/package-linux.sh`**
   - ✅ Genera instalador DEB/RPM según distribución
   - ✅ Detecta arquitectura y JARs específicos de JavaFX

5. **`Forevernote/pom.xml`**
   - ✅ Agregadas dependencias faltantes: `javafx-base` y `javafx-media`
   - ✅ Configuración de `maven-resources-plugin` para macOS (permisos POSIX)

### Problemas resueltos

- **Estructura recursiva**: `jpackage` copiaba directorio de salida dentro de sí mismo → Solucionado con directorio temporal
- **.exe no ejecutaba**: Faltaban dependencias JavaFX → Agregadas al pom.xml
- **Module javafx.base not found**: JARs de Maven no son módulos JPMS → Solucionado con `Launcher` class
- **Permisos en macOS**: Maven fallaba al copiar recursos → Solucionado con `filtering=false`

---

## 📅 2025-12-18 — Scripts de ejecución multiplataforma

### Resumen
Reescritos scripts de lanzamiento para soportar correctamente todas las plataformas, incluyendo detección de arquitectura y JARs específicos de JavaFX.

### Cambios

1. **`scripts/run_all.sh`** - Reescrito completamente
   - ✅ Detección automática de plataforma (mac, mac-aarch64, linux, linux-aarch64)
   - ✅ Búsqueda de JARs específicos de plataforma
   - ✅ Fallback a JAR genérico si no hay específico
   - ✅ Compatible con bash (no usar con `sh`)

2. **`scripts/launch-forevernote.sh`** - Reescrito completamente
   - ✅ Detección de plataforma para Apple Silicon (arm64 -> mac-aarch64)
   - ✅ Mensajes compatibles con POSIX (usando `printf` en lugar de `echo -e`)
   - ✅ Muestra qué JARs se encuentran para debugging

3. **`scripts/launch-forevernote.bat`** - Corregido
   - ✅ Eliminados códigos ANSI que no funcionan en CMD
   - ✅ Traducido completamente al inglés
   - ✅ Funciona en CMD y PowerShell

4. **`scripts/launch-forevernote.ps1`** - Corregido
   - ✅ Usa archivos JAR específicos en lugar de directorios
   - ✅ Filtrado de archivos `-sources.jar` y `-javadoc.jar`

5. **Scripts obsoletos eliminados**
   - ✅ Eliminados `launch.bat` y `launch.sh` de la raíz del proyecto

### Problemas resueltos

- **Error `-e Java found:`**: `echo -e` no funciona con `sh` → Solucionado usando `printf`
- **Error `Module javafx.base not found`**: JavaFX tiene JARs específicos de plataforma → Detecta y usa el correcto
- **Error `Invalid module name: '21'`**: Scripts incluían `-sources.jar` → Filtrados correctamente

### Nota técnica

Los JARs de JavaFX en Maven son específicos de plataforma:
- `javafx-base-21.jar` - JAR genérico (sin código nativo)
- `javafx-base-21-mac.jar` - macOS Intel
- `javafx-base-21-mac-aarch64.jar` - macOS Apple Silicon (M1/M2/M3)
- `javafx-base-21-linux.jar` - Linux x86_64
- `javafx-base-21-linux-aarch64.jar` - Linux ARM64

---

## 📅 2025-12-18 — Configuración VS Code multiplataforma

### Resumen
Corregidos archivos de configuración de VS Code para funcionar en todas las plataformas. Eliminada sobreingeniería y hardcodeos.

### Cambios

1. **`.vscode/launch.json`**
   - ✅ Simplificada configuración (Maven maneja JavaFX automáticamente)
   - ✅ Añadido `"cwd": "${workspaceFolder}/Forevernote"` para correcto directorio de trabajo
   - ✅ Eliminadas secciones multiplataforma redundantes

2. **`.vscode/settings.json`**
   - ✅ Eliminadas rutas hardcodeadas de Java
   - ✅ VS Code auto-detecta JDK de cada plataforma
   - ✅ Eliminados hardcodeos de Windows

3. **`.vscode/tasks.json`**
   - ✅ Eliminado `JAVA_HOME` hardcodeado
   - ✅ Mantenidas secciones específicas de plataforma para scripts

4. **Archivos Eclipse eliminados**
   - ✅ Eliminados `.classpath`, `.project` que interferían con Maven
   - ✅ Contenían rutas absolutas incorrectas de otro usuario

### Problema resuelto

Los archivos de Eclipse contenían rutas absolutas incorrectas y estructura incorrecta (`path="src"` en lugar de `path="src/main/java"`), causando que VS Code no detectara correctamente la estructura Maven.

---

## 📅 2025-12-18 — Corrección crítica: .gitignore estaba ignorando código fuente

### Resumen
Corregido problema crítico donde `.gitignore` tenía `data/` que ignoraba TODAS las carpetas `data/`, incluyendo el código fuente en `Forevernote/src/main/java/com/example/forevernote/data/`.

### Solución

1. **`.gitignore`**
   - ✅ Eliminado `data/` genérico
   - ✅ Mantenido solo `Forevernote/data/` (carpeta de runtime con base de datos)
   - ✅ Ahora solo ignora la carpeta de runtime, NO el código fuente

2. **Archivos añadidos a Git**
   - ✅ Todos los archivos de `Forevernote/src/main/java/com/example/forevernote/data/` ahora están en staging
   - ✅ 17 archivos Java de la capa de datos listos para commit

### Archivos que ahora se suben
- `SQLiteDB.java`
- Todos los DAOs (`FolderDAOSQLite`, `NoteDAOSQLite`, `TagDAOSQLite`, etc.)
- Todos los modelos (`Folder`, `Note`, `Tag`, `ToDoNote`, etc.)
- Interfaces y capas abstractas

---

## 📅 2025-12-18 — Corrección de bugs en favoritos, notas recientes y botón recargar

### Resumen
Corrección de tres problemas críticos: error IndexOutOfBoundsException al hacer clic en favoritos, notas recientes no se visualizaban, y botón recargar no respetaba el contexto actual.

### Archivos modificados

1. **`Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java`**
   - ✅ Agregadas variables de estado para rastrear contexto: `currentFilterType`, `currentTag`
   - ✅ Corregido error IndexOutOfBoundsException en favoritos: ahora carga nota directamente sin intentar seleccionar en lista
   - ✅ Agregado listener para `recentNotesListView` para que las notas recientes se visualicen al hacer clic
   - ✅ Implementado `handleRefresh()` que respeta el contexto actual (carpeta/tag/favoritos/búsqueda)
   - ✅ Modificado `loadFavorites()` para limpiar selección antes de actualizar lista
   - ✅ Cambiado de `setAll()` a `clear()` + `addAll()` para evitar problemas de selección
   - ✅ Usado `Platform.runLater()` para asegurar que la actualización se complete antes de cargar nota

### Problemas corregidos

1. **Error IndexOutOfBoundsException en favoritos**
   - ❌ **Antes**: Intentaba seleccionar nota en `notesListView` aunque no estuviera en la lista actual
   - ✅ **Ahora**: Carga la nota directamente en el editor sin intentar seleccionarla en la lista

2. **Notas recientes no se visualizaban**
   - ❌ **Antes**: No había listener para cuando se hacía clic en una nota reciente
   - ✅ **Ahora**: Agregado listener que carga la nota en el editor al hacer clic

3. **Botón recargar siempre mostraba todas las notas**
   - ❌ **Antes**: `handleRefresh()` siempre llamaba a `loadAllNotes()`
   - ✅ **Ahora**: Respeta el contexto actual (carpeta/tag/favoritos/búsqueda)

---

## 📅 2025-12-17 — Implementación de funcionalidad de favoritos (is_favorite)

### Resumen
Implementación completa del campo `is_favorite` en las notas, permitiendo marcar y desmarcar notas como favoritas.

### Archivos modificados

1. **`Forevernote/src/main/java/com/example/forevernote/data/models/Note.java`**
   - ✅ Agregado campo `isFavorite` (boolean) con getter y setter

2. **`Forevernote/src/main/java/com/example/forevernote/data/SQLiteDB.java`**
   - ✅ Agregada columna `is_favorite` al esquema de la tabla `notes`
   - ✅ Implementada migración automática para bases de datos existentes

3. **`Forevernote/src/main/java/com/example/forevernote/data/dao/NoteDAOSQLite.java`**
   - ✅ Actualizado para incluir `is_favorite` en INSERT y UPDATE
   - ✅ Actualizado `mapResultSetToNote()` para leer `is_favorite`

4. **`Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java`**
   - ✅ Implementado método `loadFavorites()` para cargar notas favoritas
   - ✅ Actualizado método `handleToggleFavorite()` para alternar estado de favorito

### Funcionalidades implementadas

- ✅ Marcar/desmarcar notas como favoritas desde el menú
- ✅ Persistencia del estado de favorito en la base de datos
- ✅ Lista de favoritos en la barra lateral que se actualiza automáticamente
- ✅ Clic en favorito carga la nota en el editor
- ✅ Migración automática de bases de datos existentes

---

## 📅 2025-12-17 — Corrección completa de errores y funcionalidades

### Resumen
Corrección exhaustiva de todos los errores identificados en el proyecto e implementación de funcionalidades faltantes para que la aplicación sea completamente funcional.

### Errores críticos corregidos

1. **`Note.java`**
   - ✅ Corregido tipo de datos: `Integer` → `Double` para `latitude` y `longitude`
   - ✅ Corregido typo: `logitude` → `longitude`
   - ✅ Mejorado `equals()` y `hashCode()` para usar ID cuando está disponible

2. **`SQLiteDB.java`**
   - ✅ Corregido manejo de conexiones en `initDatabase()`
   - ✅ Agregado rollback en caso de error
   - ✅ Eliminado constraint `UNIQUE` en título de notas

3. **`NoteDAOSQLite.java`**
   - ✅ Corregido manejo de tipos: `getInt()` → `getDouble()` para coordenadas
   - ✅ Agregado rollback en todos los métodos de modificación
   - ✅ Cambiado `mapResultSetToNote` de `protected` a `public`

4. **`FolderDAOSQLite.java` y `TagDAOSQLite.java`**
   - ✅ Corregidos loggers y typos (`cratedDate` → `createdDate`)
   - ✅ Agregado rollback en todos los métodos de modificación

5. **`MainController.java` - Corrección de carpetas**
   - ✅ Cambiado `TreeView<String>` a `TreeView<Folder>` para identificar correctamente por ID
   - ✅ Corregido `loadFolders()` para solo mostrar carpetas root
   - ✅ Corregido manejo de subcarpetas y operaciones CRUD

6. **`MainController.java` - Corrección de tags y Markdown**
   - ✅ Cambiado preview de `TextArea` a `WebView` para renderizar HTML del Markdown
   - ✅ Añadido estilos CSS completos para el preview de Markdown
   - ✅ Corregido `handleAddTagToNote()` para actualizar lista de tags

### Funcionalidades implementadas

- ✅ **Formato Markdown**: Bold, Italic, Underline, Link, Image - insertan sintaxis Markdown
- ✅ **Listas**: Todo lists (`- [ ]`) y Numbered lists (`1. `)
- ✅ **Zoom**: In, Out, Reset con control de tamaño de fuente (50%-300%)
- ✅ **Búsqueda global**: Busca en títulos y contenido de todas las notas
- ✅ **Tags Manager**: Diálogo completo con lista de tags y opción de eliminar
- ✅ **Preferences**: Diálogo de configuración con información de base de datos
- ✅ **Documentation**: Diálogo de guía de usuario con características y atajos
- ✅ **Keyboard Shortcuts**: Diálogo completo con todos los atajos disponibles
- ✅ **Replace**: Diálogo completo de buscar y reemplazar
- ✅ **Auto-refresh**: Listado de notas se actualiza automáticamente al guardar/eliminar
- ✅ **Nodo "All Notes" visible**: Cambiado a "📚 All Notes" y visible en árbol

### Scripts corregidos

- ✅ **`scripts/run_all.ps1` y `scripts/run_all.sh`**
   - ✅ Corregido módulo JavaFX: `javafx.media` → `javafx.web`
   - ✅ Agregado `javafx.base` a los módulos requeridos
   - ✅ Mejorada búsqueda de módulos JavaFX

### Mejoras técnicas

- ✅ **Manejo de transacciones**: Todos los métodos DAO incluyen rollback en caso de error
- ✅ **Manejo de nulls**: Agregado manejo robusto de valores null en toda la aplicación
- ✅ **Cierre de recursos**: Mejorado el cierre de conexiones de base de datos

---

## 📅 2025-12-17 — Correcciones de funcionalidades UI

### Resumen
Corrección de errores de UI y mejora de la experiencia de usuario para tags, carpetas y preview de Markdown.

### Archivos modificados

1. **`MainController.java`**
   - ✅ Implementado `handleTagSelection()` para filtrar notas cuando se clicka en un tag
   - ✅ Cambiado diálogo de añadir tag: de `ChoiceDialog` a `Dialog` con `ComboBox` editable
   - ✅ Permite seleccionar tags existentes O escribir un nuevo tag
   - ✅ El nodo "All Notes" en el árbol ahora funciona correctamente

2. **`MainView.fxml`**
   - ✅ Eliminado `ScrollPane` wrapper del `WebView` (causaba error de coerción)
   - ✅ Cambiado `TextArea` por `WebView` en la pestaña Preview

3. **`SQLiteDB.java`**
   - ✅ Añadida migración para eliminar constraint UNIQUE de `folders.title`
   - ✅ Permite crear carpetas con el mismo nombre en diferentes ubicaciones

### Mejoras de UX

- **Tags**: Al hacer clic en un tag se filtran las notas que lo tienen
- **Añadir Tags**: El diálogo muestra un ComboBox con las tags existentes + opción de escribir nueva
- **Carpetas**: Se puede crear carpetas con nombres duplicados (en diferentes ubicaciones)
- **All Notes**: Funciona correctamente como nodo raíz para mostrar todas las notas

---

## 📅 2025-12-17 — Corrección de module-path y scripts de ejecución

### Resumen
Corrección de scripts de ejecución para usar rutas de JARs específicos en lugar de directorios, evitando que Java intente cargar archivos `-sources.jar` como módulos.

### Archivos modificados

1. **`scripts/run_all.ps1` y `scripts/run_all.sh`**
   - ✅ Cambiado para usar rutas de JARs específicos en lugar de directorios
   - ✅ Evita que Java escanee directorios y encuentre archivos `-sources.jar`
   - ✅ Excluidos archivos `-sources.jar` y `-javadoc.jar` de la búsqueda

2. **`Forevernote/pom.xml`**
   - ✅ Eliminada declaración duplicada del plugin `javafx-maven-plugin`
   - ✅ Añadido `javafx-maven-plugin` para ejecutar la aplicación con JavaFX correctamente

### Problema resuelto

El error `Unable to derive module descriptor for javafx-base-21-sources.jar` se debía a que el script estaba usando directorios en el module-path. Cuando Java encuentra un directorio en el module-path, escanea todos los JARs dentro de ese directorio, incluyendo los `-sources.jar` y `-javadoc.jar`, que no son módulos válidos.

Al usar rutas de JARs específicos, Java solo carga ese JAR específico y no escanea el directorio.

---

## 📅 2025-12-17 — Configuración VS Code para JavaFX

### Resumen
Corrección del error "JavaFX runtime components are missing" al ejecutar desde VS Code, añadiendo soporte para Maven JavaFX plugin.

### Archivos modificados

1. **`.vscode/launch.json`**
   - ✅ Añadida configuración "Launch Forevernote (Maven JavaFX)" que usa `javafx:run`
   - ✅ Añadida configuración "Launch Forevernote (Debug)" con module-path manual

2. **`.vscode/tasks.json`**
   - ✅ Actualizada tarea `maven-exec-java` para usar `javafx:run` en lugar de `exec:java`

3. **`.vscode/settings.json`**
   - ✅ Configurado `java.jdt.ls.java.home` para usar Java 17 explícitamente
   - ✅ Excluidos archivos `-sources.jar` y `-javadoc.jar` del classpath

### Problema resuelto

El error "JavaFX runtime components are missing" se debía a que VS Code ejecutaba la aplicación directamente con Java sin configurar el module-path de JavaFX. Ahora se usa el plugin de Maven JavaFX que maneja automáticamente todas las dependencias y el module-path.

---

## 📅 2025-12-17 — Actualización de documentación

### Resumen
Actualización exhaustiva de la documentación del proyecto para reflejar el estado actual.

### Archivos modificados

1. **`README.md`**
   - ✅ Documentación completa de requisitos (Java 17 JDK obligatorio, Maven 3.6+)
   - ✅ Instrucciones detalladas de compilación y ejecución con scripts
   - ✅ Guía completa de uso de VS Code (compilar, ejecutar, depurar)
   - ✅ Solución de problemas detallada
   - ✅ Información sobre warnings normales de compilación

2. **`SETUP.md`**
   - ✅ Guía rápida en español con todos los requisitos
   - ✅ Instrucciones paso a paso para VS Code
   - ✅ Solución de problemas específica para cada error común

3. **`Forevernote/BUILD.md`**
   - ✅ Actualizado con información completa sobre requisitos
   - ✅ Agregadas instrucciones detalladas para VS Code
   - ✅ Documentación de warnings normales de JavaFX
   - ✅ Información sobre directorios de runtime (data/, logs/)

4. **`.vscode/README.md`**
   - ✅ Traducido completamente al inglés
   - ✅ Instrucciones para resolver problemas de imports y configuración
