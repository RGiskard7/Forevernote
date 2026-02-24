import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

public class test_move {
    public static void main(String[] args) throws IOException {
        Path rootPath = Paths.get("data");
        Files.createDirectories(rootPath);
        
        Path folderPath = rootPath.resolve("folder");
        Files.createDirectories(folderPath);
        
        Path filePath = folderPath.resolve("note.md");
        Files.writeString(filePath, "test content", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        // Cache the file path
        Path cachedPath = filePath;
        
        // Move the folder to trash
        Path trashPath = rootPath.resolve(".trash");
        Files.createDirectories(trashPath);
        Path trashFolder = trashPath.resolve("folder");
        Files.move(folderPath, trashFolder);
        
        System.out.println("Moved to trash");
        
        // Try reading from cache
        try {
            System.out.println("Reading before restore: " + Files.readString(cachedPath));
        } catch (IOException e) {
            System.out.println("Expected failure: " + e.getMessage());
        }
        
        // Restore folder
        Files.move(trashFolder, folderPath);
        System.out.println("Restored from trash");
        
        // Try reading from cache
        System.out.println("Reading after restore: " + Files.readString(cachedPath));
    }
}
