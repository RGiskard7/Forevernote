# Guía Rápida de Configuración - Forevernote

## Requisitos Previos

- **Java JDK 17** o superior
- **Apache Maven 3.6+** (opcional: los scripts pueden instalarlo automáticamente)

Verifica tu instalación:
```bash
java -version
mvn -version
```

## Instalación Rápida

### 1. Clonar el Repositorio

```bash
git clone https://github.com/RGiskard7/Forevernote.git
cd Forevernote
```

### 2. Compilar y Ejecutar

**Windows (PowerShell):**
```powershell
.\scripts\build_all.ps1
.\scripts\run_all.ps1
```

**macOS/Linux:**
```bash
./scripts/build_all.sh
./scripts/run_all.sh
```

## Alternativa: Compilación Manual

```bash
# Compilar
mvn -f Forevernote/pom.xml clean package -DskipTests

# Ejecutar JAR
java -jar Forevernote/target/forevernote-1.0.0-uber.jar
```

## Información sobre los Scripts

Los scripts de compilación (`build_all.ps1` / `build_all.sh`) intentan:
1. Detectar si Maven está instalado
2. Compilar y empaquetar el proyecto en un JAR ejecutable
3. Instalar Maven automáticamente si no lo encuentra (solo si lo permites)

Los scripts de ejecución (`run_all.ps1` / `run_all.sh`):
1. Detectan automáticamente los módulos JavaFX en tu repositorio Maven
2. Configuran correctamente el module-path
3. Lanzan la aplicación

## Base de Datos y Logs

- **Base de datos**: Se crea automáticamente en `Forevernote/data/database.db`
- **Logs**: Se generan en el directorio `logs/`

Ambos directorios se crean automáticamente si no existen.

## Solución de Problemas

### Error: "JavaFX runtime components are missing"

Usa los scripts proporcionados, que configuran correctamente el module-path automáticamente. Si necesitas ejecutar manualmente:

```bash
mvn -f Forevernote/pom.xml exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

### Maven no encontrado

Los scripts intentarán instalarlo. Si prefieres instalarlo manualmente:

- **Windows**: Descarga desde https://maven.apache.org/download.cgi
- **macOS**: `brew install maven`
- **Linux (Ubuntu/Debian)**: `sudo apt-get install maven`

## Integración con VS Code

El proyecto incluye tareas configuradas en `.vscode/tasks.json`:

1. Presiona `Ctrl+Shift+B` para compilar
2. Ve a **Terminal → Ejecutar Tarea** para ejecutar
3. Selecciona "Run Forevernote (Script)" o "Run Forevernote (Direct)"

## Información Adicional

- Ver `README.md` para documentación completa
- Ver `scripts/README.md` para detalles específicos de los scripts
- Ver `BUILD.md` para información de construcción avanzada

```
Forevernote/
├── src/
│   ├── main/java/       # Código fuente principal
│   └── test/java/       # Casos de prueba (unitarios)
├── target/              # Archivos compilados
├── pom.xml             # Configuración Maven
└── .vscode/tasks.json  # Tareas de VS Code
```

## Información de la Aplicación

**Aplicación**: Forevernote - Gestor de Notas JavaFX  
**Versión**: 1.0.0  
**Lenguaje**: Java 17  
**Marco**: JavaFX 21 + SQLite

### Características

- 📝 Crear, editar y eliminar notas
- 📁 Organizar en carpetas/notebooks
- 🏷️ Sistema de etiquetas
- 💾 Persistencia en SQLite
- 🎨 Interfaz gráfica moderna con JavaFX

## Solución de Problemas

### Error: "mvn: El término 'mvn' no se reconoce"

**Solución**: Maven está instalado pero no en el PATH. Usa la ruta completa en los comandos.

### Error: "Could not find or load main class"

**Solución**: Asegúrate de que la compilación fue exitosa. Compila nuevamente con:
```bash
C:\Users\elija\.maven\maven-3.9.11\bin\mvn.cmd clean compile
```

### La aplicación se abre pero se cierraimmediatamente

**Solución**: Verifica que todas las dependencias de JavaFX están descargadas:
```bash
C:\Users\elija\.maven\maven-3.9.11\bin\mvn.cmd clean compile
```

## Próximos Pasos

1. **Compilar**: `Ctrl+Shift+B` → Selecciona "Compile Forevernote"
2. **Ejecutar**: `Ctrl+Shift+B` → Selecciona "Run Forevernote (with dependencies)"
3. **Desarrollar**: Edita los archivos en `src/main/java` y recompila

¡La aplicación está lista para usar! 🚀
