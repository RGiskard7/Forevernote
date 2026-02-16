# Forevernote Design System

Este documento describe la guía de estilo, componentes y coherencia visual de Forevernote para asegurar que futuras modificaciones respeten la estética "Obsidian/Modern" de la aplicación en sus versiones clara y oscura.

## 1. Sistema de Colores (CSS Variables)

Forevernote utiliza variables de color para facilitar la gestión de temas. Todas las definiciones de color deben usar estas variables.

### Variables Globales
| Variable | Descripción | Light Theme | Dark Theme |
| :--- | :--- | :--- | :--- |
| `-fx-base-bg` | Fondo principal de la aplicación | `#ffffff` | `#1e1e1e` |
| `-fx-sidebar-bg` | Fondo de barras laterales y tabs | `#f5f5f5` | `#252525` |
| `-fx-header-bg` | Fondo de headers y barras de herramientas | `#f0f0f0` | `#2a2a2a` |
| `-fx-accent` | Color destacado principal | `#7c3aed` | `#7c3aed` |
| `-fx-accent-hover` | Color acento al pasar el ratón | `#6d28d9` | `#6d28d9` |
| `-fx-text-main` | Color de texto principal | `#222222` | `#e0e0e0` |
| `-fx-text-muted` | Texto secundario o iconos inactivos | `#666666` | `#a0a0a0` |
| `-fx-text-faint` | Texto muy suave o placeholders | `#999999` | `#777777` |
| `-fx-fn-border-color` | Color de bordes y separadores | `#d4d4d4` | `#3a3a3a` |

### Colores Semánticos
- **Success:** `-fx-success` (`#10b981` light / `#34d399` dark)
- **Warning:** `-fx-warning` (`#f59e0b` light / `#fbbf24` dark)
- **Danger:** `-fx-danger` (`#ef4444` light / `#f87171` dark)

### Folder Specific Colors
- **All Notes [=]:** `-fx-folder-all` (`#9f7aea`)
- **Expanded [/]:** `-fx-folder-open` (`#48bb78`)
- **Collapsed [+]:** `-fx-folder-closed` (`#ed8936`)

## 2. Iconografía
- **Librería:** [Feather Icons](https://feathericons.com/) (vía Ikonli `fth-`).
- **Estados:**
    - **Inactivo:** `-fx-text-muted`.
    - **Seleccionado:** `-fx-accent-contrast` sobre fondo `-fx-accent`.
    - **Pin Activo:** `-fx-accent` directamente.
- **Iconos Clave:**
    - **Fijar (Pin):** `fth-map-pin`.
    - **Favorito (Favorite):** `fth-star`.
    - **Panel Info:** `fth-menu` o `fth-sidebar`.
    - **Borrar:** `fth-trash-2`.

## 3. Componentes UI

### Botones y Toggles
- **Clase:** `.modern-toggle-btn` o `.toolbar-btn`.
- **Feedback:** Todo elemento clicable debe tener un estado `:hover` con un cambio visible de color de fondo (`-fx-hover-bg`).
- **Contrast:** En temas oscuros, asegurar que el texto/icono tenga suficiente contraste contra el fondo (usar `-fx-text-main` o `-fx-accent-contrast`).

### Note List Cells
- **Fondo:** Transparente por defecto, `-fx-accent` cuando está seleccionada.
- **Texto:** El título, previa y fecha deben cambiar a `-fx-accent-contrast` al ser seleccionados para máxima legibilidad.

## 4. Reglas de Implementación
1. **No usar colores hardcoded:** Prohibido usar hex/rgb directamente en los selectores. Usar siempre las variables definidas en `.root`.
2. **Soporte Multi-tema:** Cualquier cambio visual debe probarse tanto en `modern-theme.css` como en `dark-theme.css`.
3. **Accesibilidad:** Mantener contrastes altos (> 4.5:1 para texto normal). En estados `:selected:hover`, usar siempre `-fx-accent-hover` de fondo y `-fx-accent-contrast` para el texto.
4. **Ikonli Icons:** Usar `feather-icon` como clase base para iconos de la librería Feather.
5. **Iconos de Carpeta:** Los signos `[=]`, `[/]` y `[+]` deben usar sus variables específicas y cambiar a `-fx-accent-contrast` al ser seleccionados.
