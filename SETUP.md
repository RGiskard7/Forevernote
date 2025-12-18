# Guía Rápida de Configuración - Forevernote

## Requisitos Previos

### Software Requerido

1. **Java JDK 17** (obligatorio)
   - **Importante**: Necesitas el JDK (Java Development Kit), no solo el JRE (Java Runtime Environment)
   - Descarga desde: https://adoptium.net/ o https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
   - Verifica la instalación:
     ```bash
     java -version
     # Debe mostrar: openjdk version "17" o java version "17"
     ```

2. **Apache Maven 3.6+** (obligatorio para compilar)
   - Descarga desde: https://maven.apache.org/download.cgi
   - O usa gestores de paquetes:
     - **Windows**: `choco install maven` o `winget install Apache.Maven`
     - **macOS**: `brew install maven`
     - **Linux (Ubuntu/Debian)**: `sudo apt-get install maven`
   - Verifica la instalación:
     ```bash
     mvn -version
     # Debe mostrar: Apache Maven 3.x.x
     ```

### Opcional: Extensiones de VS Code

Para desarrollo en VS Code, instala:
- **Extension Pack for Java** (incluye soporte de Java, depurador, ejecutor de tests, etc.)
- **Maven for Java**

## Instalación Rápida

### 1. Clonar el Repositorio

```bash
git clone https://github.com/RGiskard7/Forevernote.git
cd Forevernote
```

### 2. Compilar el Proyecto

**Windows (PowerShell):**
```powershell
.\scripts\build_all.ps1
```

**macOS/Linux (Bash):**
```bash
./scripts/build_all.sh
```

Esto crea un JAR ejecutable en `Forevernote/target/forevernote-1.0.0-uber.jar`.

**Nota**: Durante la compilación, puedes ver warnings como "Failed to build parent project for org.openjfx:javafx-*". Estos son **normales e inofensivos** - ocurren porque Maven intenta construir el proyecto padre de JavaFX, lo cual no es necesario. La compilación seguirá siendo exitosa.

### 3. Ejecutar la Aplicación

**Windows (PowerShell):**
```powershell
.\scripts\run_all.ps1
```

**macOS/Linux (Bash):**
```bash
./scripts/run_all.sh
```

Los scripts automáticamente:
- Detectan los módulos JavaFX en tu repositorio Maven
- Configuran el module-path de Java correctamente
- Lanzan la aplicación

## Compilar y Ejecutar con VS Code

### Prerrequisitos

1. **Instalar Java 17 JDK** (no solo JRE)
   - Asegúrate de que `java -version` muestre la versión 17
   - VS Code lo detectará automáticamente

2. **Instalar Extensiones de VS Code**:
   - Abre VS Code
   - Presiona `Ctrl+Shift+X` (o `Cmd+Shift+X` en macOS)
   - Busca e instala: **Extension Pack for Java**
   - Esto incluye todas las extensiones Java necesarias

3. **Configurar Java Runtime** (si es necesario):
   - Presiona `Ctrl+Shift+P` (o `Cmd+Shift+P` en macOS)
   - Escribe: `Java: Configure Java Runtime`
   - Selecciona Java 17 como runtime por defecto
   - Si Java 17 no aparece, añádelo manualmente apuntando a tu instalación de JDK 17

### Compilar en VS Code

**Método 1: Usando Tareas (Recomendado)**
1. Presiona `Ctrl+Shift+B` (o `Cmd+Shift+B` en macOS)
2. Selecciona **"maven-compile"** para compilar
3. O selecciona **"maven-package"** para construir el JAR

**Método 2: Usando Terminal**
1. Abre terminal integrado: `Ctrl+`` (backtick)
2. Ejecuta:
   ```bash
   cd Forevernote
   mvn clean package -DskipTests
   ```

### Ejecutar en VS Code

**Método 1: Usando Debug/Run (Recomendado)**
1. Presiona `F5` o ve a **Run and Debug** (Ctrl+Shift+D)
2. Selecciona **"Launch Forevernote (Maven JavaFX)"** del menú desplegable
3. Haz clic en el botón verde de play o presiona `F5`
4. La aplicación se lanzará con JavaFX correctamente configurado

**Método 2: Usando Tareas**
1. Presiona `Ctrl+Shift+P` (o `Cmd+Shift+P` en macOS)
2. Escribe: `Tasks: Run Task`
3. Selecciona **"maven-exec-java"** para ejecutar vía Maven

**Método 3: Usando Terminal**
1. Abre terminal integrado: `Ctrl+`` (backtick)
2. Ejecuta:
   ```bash
   cd Forevernote
   mvn javafx:run
   ```

### Solución de Problemas en VS Code

**Problema**: VS Code muestra errores para imports de JavaFX
- **Solución**: 
  1. Presiona `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
  2. Presiona `Ctrl+Shift+P` → `Java: Reload Projects`
  3. Espera 1-2 minutos a que Maven sincronice las dependencias

**Problema**: "JavaFX runtime components are missing" al ejecutar
- **Solución**: Usa la configuración "Launch Forevernote (Maven JavaFX)", que usa el plugin JavaFX de Maven

**Problema**: VS Code usa Java 21 en lugar de Java 17
- **Solución**: 
  1. Presiona `Ctrl+Shift+P` → `Java: Configure Java Runtime`
  2. Establece Java 17 como predeterminado
  3. Actualiza `.vscode/settings.json` si es necesario (ver sección de configuración)

## Alternativa: Compilación Manual

### Compilar

```bash
cd Forevernote
mvn clean package -DskipTests
```

### Ejecutar JAR (con scripts - recomendado)

Los scripts manejan el module-path de JavaFX automáticamente:

**Windows:**
```powershell
.\scripts\run_all.ps1
```

**macOS/Linux:**
```bash
./scripts/run_all.sh
```

### Ejecutar JAR (directamente - requiere module-path manual)

Si quieres ejecutar el JAR directamente, necesitas especificar los módulos JavaFX:

```bash
java --module-path "C:\Users\<tu_usuario>\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21.jar;C:\Users\<tu_usuario>\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21.jar;..." --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web -jar Forevernote/target/forevernote-1.0.0-uber.jar
```

**Nota**: Esto es complejo y propenso a errores. Usa los scripts en su lugar.

### Ejecutar desde Código Fuente (Desarrollo)

```bash
cd Forevernote
mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

O usando el plugin JavaFX de Maven:
```bash
mvn javafx:run
```

## Información sobre los Scripts

### Scripts de Compilación

Los scripts de compilación (`build_all.ps1` / `build_all.sh`):
1. Detectan si Maven está instalado
2. Compilan y empaquetan el proyecto en un JAR ejecutable
3. Intentan instalar Maven automáticamente si no lo encuentran (solo si lo permites)

**Nota**: Los scripts de compilación **NO** crean las carpetas `data/` y `logs/`. Estas se crean automáticamente cuando ejecutas la aplicación por primera vez.

### Scripts de Ejecución

Los scripts de ejecución (`run_all.ps1` / `run_all.sh`):
1. Detectan automáticamente los módulos JavaFX en tu repositorio Maven (`~/.m2/repository/org/openjfx/`)
2. Buscan los JARs compilados específicos (excluyendo `-sources.jar` y `-javadoc.jar`)
3. Configuran correctamente el module-path usando rutas de JARs específicos
4. Lanzan la aplicación

**Ventaja**: Los scripts manejan automáticamente la configuración del module-path, evitando errores comunes.

## Base de Datos y Logs

- **Base de datos**: Se crea automáticamente en `Forevernote/data/database.db` cuando ejecutas la aplicación
- **Logs**: Se generan en el directorio `Forevernote/logs/` cuando ejecutas la aplicación

**Importante**: 
- Los scripts de compilación (`build_all.ps1` / `build_all.sh`) **NO** crean estas carpetas
- Solo se crean automáticamente cuando ejecutas la aplicación por primera vez
- Los scripts de ejecución (`run_all.ps1` / `run_all.sh`) aseguran que la aplicación se ejecute desde el directorio `Forevernote/`, por lo que las carpetas se crean en la ubicación correcta

## Solución de Problemas

### Warnings de Compilación

**Warning**: "Failed to build parent project for org.openjfx:javafx-*"
- **Estado**: Normal e inofensivo
- **Explicación**: Maven intenta construir el proyecto padre de JavaFX, lo cual no es necesario
- **Acción**: Ignora estos warnings - no afectan la funcionalidad

**Warning**: "6 problems were encountered while building the effective model"
- **Estado**: Normal e inofensivo
- **Explicación**: Relacionado con los warnings del proyecto padre de JavaFX
- **Acción**: Ignora - la compilación seguirá siendo exitosa

### Errores de Ejecución

**Error**: "JavaFX runtime components are missing"

**Solución 1 (Recomendada)**: Usa los scripts proporcionados:
```powershell
# Windows
.\scripts\run_all.ps1
```

```bash
# macOS/Linux
./scripts/run_all.sh
```

**Solución 2**: Ejecuta vía Maven:
```bash
cd Forevernote
mvn javafx:run
```

O:
```bash
mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

**Error**: "Invalid module name: '21' is not a Java identifier"
- **Solución**: Este error fue corregido en los scripts. Asegúrate de usar la última versión de los scripts
- **Causa**: Los scripts apuntaban a directorios que contenían archivos `-sources.jar`
- **Corrección**: Los scripts ahora usan rutas de JARs específicos

### Problemas con Maven

**Maven no encontrado**

Los scripts intentarán instalarlo automáticamente. Si prefieres instalarlo manualmente:

- **Windows**: Descarga desde https://maven.apache.org/download.cgi
- **macOS**: `brew install maven`
- **Linux (Ubuntu/Debian)**: `sudo apt-get install maven`

**Maven no está en PATH**

Si Maven está instalado pero no en el PATH, puedes usar la ruta completa:

**Windows:**
```powershell
& 'C:\Users\<tu_usuario>\.maven\maven-3.9.11\bin\mvn.cmd' -f Forevernote/pom.xml clean package -DskipTests
```

**macOS/Linux:**
```bash
/usr/local/bin/mvn -f Forevernote/pom.xml clean package -DskipTests
```

O configura variables de entorno temporales:

**PowerShell (temporal para la sesión):**
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = 'C:\Users\<tu_usuario>\.maven\maven-3.9.11\bin;' + $env:Path
```

**Bash (temporal para la sesión):**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### Problemas con VS Code

**Problema**: VS Code muestra errores para imports de JavaFX
- **Solución**: 
  1. `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
  2. `Ctrl+Shift+P` → `Java: Reload Projects`
  3. Espera a que Maven sincronice (1-2 minutos)

**Problema**: VS Code usa la versión incorrecta de Java
- **Solución**: 
  1. `Ctrl+Shift+P` → `Java: Configure Java Runtime`
  2. Establece Java 17 como predeterminado
  3. Verifica `.vscode/settings.json` tiene `"java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-17"` (ajusta la ruta para tu sistema)

**Problema**: No puedo ejecutar desde VS Code
- **Solución**: Usa la configuración "Launch Forevernote (Maven JavaFX)" que maneja todo automáticamente

**Problema**: "Could not find or load main class"
- **Solución**: Asegúrate de que la compilación fue exitosa. Recompila con:
  ```bash
  cd Forevernote
  mvn clean compile
  ```

**Problema**: La aplicación se abre pero se cierra inmediatamente
- **Solución**: Verifica que todas las dependencias de JavaFX están descargadas:
  ```bash
  cd Forevernote
  mvn clean compile
  ```

## Configuración de VS Code

El proyecto incluye configuraciones predefinidas de VS Code:

- **`.vscode/settings.json`**: Configuración de Java 17, configuración de Maven, archivos excluidos
- **`.vscode/tasks.json`**: Tareas de compilación (compile, package, test, run)
- **`.vscode/launch.json`**: Configuraciones de depuración/ejecución con soporte JavaFX
- **`.vscode/extensions.json`**: Extensiones recomendadas

### Tareas de VS Code

Tareas disponibles (presiona `Ctrl+Shift+P` → `Tasks: Run Task`):

- **maven-compile**: Compila el proyecto
- **maven-package**: Construye el JAR
- **maven-test**: Ejecuta tests unitarios
- **maven-exec-java**: Ejecuta vía Maven (maneja JavaFX automáticamente)

### Configuraciones de Lanzamiento de VS Code

Configuraciones disponibles (presiona `F5` o ve a Run and Debug):

- **Launch Forevernote (Maven JavaFX)**: Usa el plugin JavaFX de Maven (recomendado)
- **Launch Forevernote (Debug)**: Configuración manual de module-path para depuración

## Información Adicional

- Ver `README.md` para documentación completa
- Ver `scripts/README.md` para detalles específicos de los scripts
- Ver `BUILD.md` para información de construcción avanzada
- Ver `AGENTS.md` para guía de desarrollo orientada a agentes

## Estructura del Proyecto

```
Forevernote/
├── Forevernote/              # Módulo principal del proyecto
│   ├── src/
│   │   ├── main/java/       # Código fuente principal
│   │   └── test/java/       # Casos de prueba (unitarios)
│   ├── target/               # Archivos compilados
│   ├── pom.xml              # Configuración Maven
│   ├── data/                # Directorio de datos (se crea al ejecutar)
│   └── logs/                # Directorio de logs (se crea al ejecutar)
├── scripts/                  # Scripts de compilación y ejecución
├── .vscode/                  # Configuración de VS Code
│   ├── settings.json        # Configuración de Java
│   ├── tasks.json           # Tareas de compilación
│   ├── launch.json          # Configuraciones de ejecución
│   └── extensions.json      # Extensiones recomendadas
├── README.md                 # Este archivo
├── SETUP.md                  # Guía rápida de configuración
├── BUILD.md                  # Documentación de compilación
└── AGENTS.md                 # Guía de desarrollo orientada a agentes
```

## Información de la Aplicación

**Aplicación**: Forevernote - Gestor de Notas JavaFX  
**Versión**: 1.0.0  
**Lenguaje**: Java 17 (requerido)  
**Marco**: JavaFX 21 + SQLite  
**Build Tool**: Maven 3.6+

### Características

- 📝 Crear, editar y eliminar notas
- 📁 Organizar en carpetas/notebooks jerárquicos
- 🏷️ Sistema de etiquetas completo
- 📝 Soporte Markdown con vista previa en vivo
- 🔍 Búsqueda global en todas las notas
- 💾 Persistencia en SQLite
- 🎨 Interfaz gráfica moderna con JavaFX
- ⌨️ Atajos de teclado completos
- 🔄 Actualización automática de listas

## Próximos Pasos

1. **Compilar**: `Ctrl+Shift+B` → Selecciona "maven-compile" o "maven-package"
2. **Ejecutar**: Presiona `F5` → Selecciona "Launch Forevernote (Maven JavaFX)"
3. **Desarrollar**: Edita los archivos en `src/main/java` y recompila

¡La aplicación está lista para usar! 🚀
