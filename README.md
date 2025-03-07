# Forevernote - Notes Application

## Description

The Notes Application is a Java-based desktop application designed to help users manage their notes effectively. It provides a user-friendly interface for creating, updating, and organizing notes into notebooks and tags. The application is ideal for both personal and professional use, offering robust features to enhance productivity and organization. The application also supports comprehensive logging to monitor activities and troubleshoot issues efficiently.

## Features

- **Create Notes**: Easily create new notes with titles and content. Example: Create a note titled "Meeting Notes" with key points from a meeting.
- **Edit Notes**: Update existing notes with new information. Example: Add additional details to a note after a brainstorming session.
- **Delete Notes**: Remove notes that are no longer needed to keep your workspace organized.
- **Notebooks/Folders Management**: Create and delete notebooks/folders, providing a hierarchical organization for their notes. Notes can be added to or removed from notebooks/folders.
- **Labels Management**: Create and delete labels, and assign them to notes for better categorization and searchability.
- **Logging**: Comprehensive logging setup to track application behavior and errors, aiding in troubleshooting and monitoring.

## Technology Stack

- **Java**: The core programming language used for the application, providing a robust and platform-independent environment.
- **SQLite**: A lightweight database engine used for storing notes, notebooks, and tags, ensuring data persistence and reliability.
- **Logging**: Java's built-in `java.util.logging` for capturing and recording log messages, facilitating debugging and analysis.

## Installation

1. Clone the repository:
    ```bash
    git clone https://github.com/RGiskard7/Forevernote.git
    cd Forevernote
    ```

2. Set up your environment:
    - Ensure you have Java installed (JDK 8 or later).
    - Include the necessary libraries in your project (e.g., `sqlite-jdbc`, `slf4j-api`, `slf4j-jdk14`, logging libraries).

3. Create the `logging.properties` file:
    ```properties
    handlers= java.util.logging.ConsoleHandler, java.util.logging.FileHandler

    # ConsoleHandler Configuration
    java.util.logging.ConsoleHandler.level = SEVERE
    java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter

    # Other consoleHandler Configuration for level INFO
    java.util.logging.ConsoleHandler.level = INFO
    java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter

    # FileHandler Configuration
    java.util.logging.FileHandler.level = ALL
    java.util.logging.FileHandler.pattern = logs/app.log
    java.util.logging.FileHandler.append = true
    java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter

    # Root logger configuration
    .level = ALL
    ```

4. Run the application:
    ```bash
    java -cp .:path/to/sqlite-jdbc.jar com.example.forevernote.Main
    ```

## Usage

1. Run the application:
    ```bash
    java -jar target/Forevernote.jar
    ```

2. Use the GUI to create, edit, delete, and organize your notes.

## Configuration

The application uses a custom logger configuration to manage logging output. The configuration file `logging.properties` should be placed in the `resources` directory. Ensure the directory is included in the classpath.

## Project Structure

```
Forevernote/
│
├── data/                    # Directory for data files and resources
│   ├── ...
├── lib/                     # External libraries and dependencies
│   ├── ...
├── logs/                    # Directory for log files
│   ├── ...
├── resources/               # Additional resources such as configuration files
│   ├── ...
├── src/                     # Source code directory
│   ├── com/
│       ├── example/
│           ├── forevernote/
│               ├── config/  # Configuration classes
│               │   ├── LoggerConfig.java
│               ├── data/    # Data access and model classes
│               │   ├── SQLiteDB.java
│               │   ├── dao/ # Data Access Object classes
│               │   │   ├── TagDAOSQLite.java
│               │   │   ├── NoteDAOSQLite.java
│               │   │   ├── FolderDAOSQLite.java
│               │   │   ├── FactoryDAOSQLite.java
│               │   │   ├── interfaces/ # DAO interfaces
│               │   │   │   ├── TagDAO.java
│               │   │   │   ├── NoteDAO.java
│               │   │   │   ├── FolderDAO.java
│               │   │   ├── abstractLayers/ # Abstract DAO layers
│               │   │   │   ├── FactoryDAO.java
│               │   ├── models/ # Model classes
│               │   │   ├── ToDoNote.java
│               │   │   ├── Tag.java
│               │   │   ├── Note.java
│               │   │   ├── Folder.java
│               │   │   ├── interfaces/ # Model interfaces
│               │   │   │   ├── Component.java
│               │   │   ├── abstractLayers/ # Abstract model layers
│               │   │   │   ├── LeafModel.java
│               │   │   │   ├── CompositeModel.java
│               │   │   │   ├── BaseModel.java
│               ├── exceptions/ # Custom exception classes
│               │   ├── NoteNotFoundException.java
│               │   ├── NoteException.java
│               │   ├── DataAccessException.java
│               │   ├── InvalidParameterException.java
│               ├── tests/ # Test classes
│               │   ├── Test.java
│               │   ├── NoteDAOSQLiteTest.java
│               ├── ui/    # User interface components
│               │   ├── ...
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request for any enhancements or bug fixes.

---

# Aplicación de Notas

## Descripción

Esta es una aplicación de toma de notas que permite a los usuarios crear, gestionar y organizar sus notas de manera eficiente. La aplicación soporta la creación y gestión de cuadernos/carpetas, notas y etiquetas, ofreciendo una solución completa para las necesidades de toma de notas personales y profesionales. La aplicación también soporta un registro completo para monitorear actividades y solucionar problemas de manera eficiente.

## Funcionalidades

- **Crear Notas**: Crear nuevas notas fácilmente con títulos y contenido.
- **Editar Notas**: Actualizar notas existentes con nueva información.
- **Eliminar Notas**: Eliminar notas que ya no se necesitan.
- **Gestión de Cuadernos/Carpetas**: Los usuarios pueden crear y eliminar cuadernos/carpetas, proporcionando una organización jerárquica para sus notas. Las notas pueden ser añadidas o eliminadas de los cuadernos/carpetas.
- **Gestión de Etiquetas**: Los usuarios pueden crear y eliminar etiquetas, y asignarlas a notas para una mejor categorización y facilidad de búsqueda.
- **Registro**: Configuración de registro completa para rastrear el comportamiento y los errores de la aplicación.

## Tecnología

- **Java**: El lenguaje de programación principal utilizado para la aplicación.
- **SQLite**: Un motor de base de datos ligero utilizado para almacenar notas, cuadernos y etiquetas.
- **Logging**: `java.util.logging` de Java para capturar y registrar mensajes de registro.

## Instalación

1. Clona el repositorio:
    ```bash
    git clone https://github.com/RGiskard7/Forevernote.git
    cd Forevernote
    ```

2. Configura tu entorno:
    - Asegúrate de tener Java instalado (JDK 8 o posterior).
    - Incluye las bibliotecas necesarias en tu proyecto (por ejemplo, `sqlite-jdbc`, bibliotecas de logging).

3. Crea el archivo `logging.properties`:
    ```properties
    handlers= java.util.logging.ConsoleHandler, java.util.logging.FileHandler

    # Configuración del ConsoleHandler
    java.util.logging.ConsoleHandler.level = SEVERE
    java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter

    # Configuración del FileHandler
    java.util.logging.FileHandler.level = ALL
    java.util.logging.FileHandler.pattern = logs/app.log
    java.util.logging.FileHandler.append = true
    java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter

    # Configuración del logger root
    .level = ALL
    ```

4. Ejecuta la aplicación:
    ```bash
    java -cp .:path/to/sqlite-jdbc.jar com.example.forevernote.Main
    ```

## Uso

1. Ejecuta la aplicación:
    ```bash
    java -jar target/Forevernote.jar
    ```

2. Usa la interfaz gráfica para crear, editar, eliminar y organizar tus notas.

## Configuración

La aplicación utiliza una configuración personalizada del logger para gestionar la salida de los logs. El archivo de configuración logging.properties debe colocarse en el directorio resources. Asegúrate de que el directorio esté incluido en el classpath.

## Estructura del Proyecto

```
Forevernote/
│
├── data/                    # Directorio para archivos de datos y recursos
│   ├── ...
├── lib/                     # Bibliotecas externas y dependencias
│   ├── ...
├── logs/                    # Directorio para archivos de registro
│   ├── ...
├── resources/               # Recursos adicionales como archivos de configuración
│   ├── ...
├── src/                     # Directorio del código fuente
│   ├── com/
│       ├── example/
│           ├── forevernote/
│               ├── config/  # Clases de configuración
│               │   ├── LoggerConfig.java
│               ├── data/    # Clases de acceso a datos y modelos
│               │   ├── SQLiteDB.java
│               │   ├── dao/ # Clases de acceso a datos (DAO)
│               │   │   ├── TagDAOSQLite.java
│               │   │   ├── NoteDAOSQLite.java
│               │   │   ├── FolderDAOSQLite.java
│               │   │   ├── FactoryDAOSQLite.java
│               │   │   ├── interfaces/ # Interfaces de DAO
│               │   │   │   ├── TagDAO.java
│               │   │   │   ├── NoteDAO.java
│               │   │   │   ├── FolderDAO.java
│               │   │   ├── abstractLayers/ # Capas abstractas de DAO
│               │   │   │   ├── FactoryDAO.java
│               │   ├── models/ # Clases de modelos
│               │   │   ├── ToDoNote.java
│               │   │   ├── Tag.java
│               │   │   ├── Note.java
│               │   │   ├── Folder.java
│               │   │   ├── interfaces/ # Interfaces de modelos
│               │   │   │   ├── Component.java
│               │   │   ├── abstractLayers/ # Capas abstractas de modelos
│               │   │   │   ├── LeafModel.java
│               │   │   │   ├── CompositeModel.java
│               │   │   │   ├── BaseModel.java
│               ├── exceptions/ # Clases de excepciones personalizadas
│               │   ├── NoteNotFoundException.java
│               │   ├── NoteException.java
│               │   ├── DataAccessException.java
│               │   ├── InvalidParameterException.java
│               ├── tests/ # Clases de pruebas
│               │   ├── Test.java
│               │   ├── NoteDAOSQLiteTest.java
│               ├── ui/    # Componentes de la interfaz de usuario
│               │   ├── ...
```

## Licencia

Este proyecto está licenciado bajo la Licencia MIT - consulta el archivo [LICENSE](LICENSE) para más detalles.

## Contribuciones

¡Las contribuciones son bienvenidas! Por favor, bifurca el repositorio y envía una solicitud de extracción para cualquier mejora o corrección de errores.