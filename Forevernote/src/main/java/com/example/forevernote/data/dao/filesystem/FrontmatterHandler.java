package com.example.forevernote.data.dao.filesystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.ToDoNote;

/**
 * Utility class for handling YAML Frontmatter in Markdown files.
 * Parses metadata from the header and generates file content with metadata.
 */
public class FrontmatterHandler {

    private static final String SEPARATOR = "---";
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+):\\s*(.*)$");

    public static Note parse(String fileContent) {
        if (fileContent == null || fileContent.isEmpty()) {
            return new Note("", "");
        }

        String[] parts = fileContent.split(SEPARATOR, 3);

        // Check if file starts with ---
        if (!fileContent.startsWith(SEPARATOR) || parts.length < 3) {
            // No frontmatter, treat whole file as content
            // Try to extract title from first line if it's a header
            String title = "Untitled";
            String content = fileContent;

            String[] lines = fileContent.split("\n", 2);
            if (lines.length > 0 && lines[0].startsWith("# ")) {
                title = lines[0].substring(2).trim();
                content = lines.length > 1 ? lines[1] : "";
            }

            return new Note(title, content);
        }

        // Parse Frontmatter
        String frontmatter = parts[1];
        String content = parts[2].trim();
        Map<String, String> metadata = parseYaml(frontmatter);

        String id = metadata.get("id");
        String title = metadata.get("title");
        String createdDate = metadata.get("created");
        String modifiedDate = metadata.get("modified");

        // Handle ToDo notes
        boolean isToDo = "true".equalsIgnoreCase(metadata.get("is_todo"));
        String todoDue = metadata.get("todo_due");
        String todoCompleted = metadata.get("todo_completed");

        Note note;
        if (isToDo) {
            note = new ToDoNote(id, title, content, createdDate, modifiedDate, todoDue, todoCompleted);
        } else {
            note = new Note(id, title, content, createdDate, modifiedDate);
        }

        // Other properties
        note.setFavorite("true".equalsIgnoreCase(metadata.get("favorite")));
        note.setPinned("true".equalsIgnoreCase(metadata.get("pinned")));
        note.setDeleted("true".equalsIgnoreCase(metadata.get("deleted")));
        note.setDeletedDate(metadata.get("deleted_date"));
        // Handle tags
        if (metadata.containsKey("tags")) {
            String tagsString = metadata.get("tags");
            // Remove brackets []
            if (tagsString.startsWith("[") && tagsString.endsWith("]")) {
                tagsString = tagsString.substring(1, tagsString.length() - 1);
            }
            if (!tagsString.trim().isEmpty()) {
                String[] tagNames = tagsString.split(",");
                for (String tagName : tagNames) {
                    note.addTag(new Tag(tagName.trim()));
                }
            }
        }

        // Also extract inline tags from content like #my_tag
        if (content != null && !content.isEmpty()) {
            Pattern inlineTagPattern = Pattern.compile("(?<=\\s|^)#([a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9_\\-\\/]+)(?=\\s|$)");
            Matcher matcher = inlineTagPattern.matcher(content);
            while (matcher.find()) {
                String tagName = matcher.group(1);
                // Avoid adding duplicate tags
                boolean exists = note.getTags().stream().anyMatch(t -> t.getTitle().equalsIgnoreCase(tagName));
                if (!exists) {
                    note.addTag(new Tag(tagName));
                }
            }
        }

        return note;
    }

    public static String generate(Note note) {
        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");

        appendLine(sb, "id", note.getId());
        appendLine(sb, "title", note.getTitle());
        appendLine(sb, "created", note.getCreatedDate());
        appendLine(sb, "modified", note.getModifiedDate());

        appendLine(sb, "favorite", String.valueOf(note.isFavorite()));
        appendLine(sb, "pinned", String.valueOf(note.isPinned()));
        appendLine(sb, "deleted", String.valueOf(note.isDeleted()));

        if (note.getDeletedDate() != null)
            appendLine(sb, "deleted_date", note.getDeletedDate());
        if (note.getAuthor() != null)
            appendLine(sb, "author", note.getAuthor());
        if (note.getSourceUrl() != null)
            appendLine(sb, "source_url", note.getSourceUrl());

        // Tags
        List<Tag> tags = note.getTags();
        if (tags != null && !tags.isEmpty()) {
            sb.append("tags: [");
            for (int i = 0; i < tags.size(); i++) {
                sb.append(tags.get(i).getTitle());
                if (i < tags.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]\n");
        }

        if (note instanceof ToDoNote) {
            ToDoNote todo = (ToDoNote) note;
            appendLine(sb, "is_todo", "true");
            if (todo.getToDoDue() != null)
                appendLine(sb, "todo_due", todo.getToDoDue());
            if (todo.getToDoCompleted() != null)
                appendLine(sb, "todo_completed", todo.getToDoCompleted());
        }

        sb.append(SEPARATOR).append("\n\n");
        sb.append(note.getContent());

        return sb.toString();
    }

    private static void appendLine(StringBuilder sb, String key, String value) {
        if (value != null) {
            // Basic escaping could be added here
            sb.append(key).append(": ").append(value).append("\n");
        }
    }

    private static Map<String, String> parseYaml(String yamlContent) {
        Map<String, String> result = new HashMap<>();
        String[] lines = yamlContent.split("\n");

        String currentArrayKey = null;
        StringBuilder currentArrayValues = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty())
                continue;

            if (trimmedLine.startsWith("-") && currentArrayKey != null) {
                // It's an array element
                String value = trimmedLine.substring(1).trim();
                // Remove quotes
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                }

                if (currentArrayValues.length() > 0) {
                    currentArrayValues.append(", ");
                }
                currentArrayValues.append(value);
                result.put(currentArrayKey, currentArrayValues.toString());
                continue;
            }

            Matcher matcher = KEY_VALUE_PATTERN.matcher(trimmedLine);
            if (matcher.matches()) {
                String key = matcher.group(1).trim();
                String value = matcher.group(2).trim();

                // Start of an array
                if (value.isEmpty() || value.equals("-")) {
                    currentArrayKey = key;
                    currentArrayValues = new StringBuilder();
                    continue; // Might have elements in following lines
                } else {
                    currentArrayKey = null; // No longer collecting an array
                }

                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                }

                result.put(key, value);
            } else {
                currentArrayKey = null; // Reset if unrecognizable line
            }
        }
        return result;
    }
}
