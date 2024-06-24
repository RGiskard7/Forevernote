# Forevernote - Notes Application

## Description

The Notes Application is a Java-based desktop application designed to help users manage their notes effectively. Users can create, update, and delete notes, and organize them into notebooks and tags. The application uses SQLite for data storage and supports logging to monitor activities and errors.

## Features

- **Create Notes**: Easily create new notes with titles and content.
- **Edit Notes**: Update existing notes with new information.
- **Delete Notes**: Remove notes that are no longer needed.
- **Notebooks Management**: Create and delete notebooks, providing a hierarchical organization for their notes. Notes can be added to or removed from notebooks.
- **Labels Management**: Create and delete labels, and assign them to notes for better categorization and searchability.
- **Search Functionality**: Quickly find notes based on keywords, tags, or notebook names.
- **Logging**: Comprehensive logging setup to track application behavior and errors.

## Technology Stack

- **Java**: The core programming language used for the application.
- **SQLite**: A lightweight database engine used for storing notes, notebooks, and tags.
- **Logging**: Java's built-in `java.util.logging` for capturing and recording log messages.

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
    java -jar target/Evernote.jar
    ```

2. Use the GUI to create, edit, delete, and organize your notes.

## Configuration

The application uses a custom logger configuration to manage logging output. The configuration file `logging.properties` should be placed in the `resources` directory. Ensure the directory is included in the classpath.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

# Aplicación de Notas

## Descripción

Esta es una aplicación de toma de notas que permite a los usuarios crear, gestionar y organizar sus notas de manera eficiente. La aplicación soporta la creación y gestión de cuadernos, notas y etiquetas, ofreciendo una solución completa para las necesidades de toma de notas personales y profesionales.

## Funcionalidades

- **Crear Notas**: Crear nuevas notas fácilmente con títulos y contenido.
- **Editar Notas**: Actualizar notas existentes con nueva información.
- **Eliminar Notas**: Eliminar notas que ya no se necesitan.
- **Gestión de Cuadernos**: Los usuarios pueden crear y eliminar cuadernos, proporcionando una organización jerárquica para sus notas. Las notas pueden ser añadidas o eliminadas de los cuadernos.
- **Gestión de Etiquetas**: Los usuarios pueden crear y eliminar etiquetas, y asignarlas a notas para una mejor categorización y facilidad de búsqueda.
- **Funcionalidad de Búsqueda**: Encontrar rápidamente notas basadas en palabras clave, etiquetas o nombres de cuadernos.
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

## Licencia

Este proyecto está licenciado bajo la Licencia MIT - consulta el archivo [LICENSE](LICENSE) para más detalles.
