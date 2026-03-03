package com.example.forevernote.ui.workflow;

import com.example.forevernote.AppDataDirectory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.plugin.PreviewEnhancer;
import com.example.forevernote.util.MarkdownProcessor;

/**
 * Builds preview HTML for notes with safe enhancer handling.
 */
public class PreviewWorkflow {
    private static final Logger logger = LoggerConfig.getLogger(PreviewWorkflow.class);
    private static final String HLJS_SCRIPT = loadResourceText(
            "/com/example/forevernote/ui/preview/highlightjs/highlight.min.js");
    private static final String HLJS_LIGHT_CSS = loadResourceText(
            "/com/example/forevernote/ui/preview/highlightjs/vs.min.css");
    private static final String HLJS_DARK_CSS = loadResourceText(
            "/com/example/forevernote/ui/preview/highlightjs/vs2015.min.css");

    private static final Pattern OBSIDIAN_IMAGE_EMBED_PATTERN = Pattern.compile("!\\[\\[([^\\]]+)\\]\\]");
    private static final Pattern HTML_IMG_SRC_PATTERN = Pattern.compile("<img([^>]*?)src=\"([^\"]+)\"([^>]*)>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SIZE_TOKEN_PATTERN = Pattern.compile("^(\\d+)(?:x(\\d+))?$");
    private static final Map<String, Path> PREVIEW_IMAGE_CACHE = new HashMap<>();

    public record PreviewContext(String storageType, String filesystemRootDirectory, String noteId) {
        public boolean isFileSystemStorage() {
            return "filesystem".equalsIgnoreCase(storageType);
        }
    }

    public String buildPreviewHtml(String markdownContent, boolean isDarkTheme, Collection<PreviewEnhancer> enhancers) {
        return buildPreviewHtml(markdownContent, isDarkTheme, enhancers, null);
    }

    public String buildPreviewHtml(String markdownContent, boolean isDarkTheme, Collection<PreviewEnhancer> enhancers,
            PreviewContext context) {
        ProcessedMarkdown processed = preprocessMarkdown(markdownContent != null ? markdownContent : "", context);
        String html = MarkdownProcessor.markdownToHtml(processed.markdown());
        html = applyTokenReplacements(html, processed.tokenToHtml());
        html = resolveRelativeImageSourcesInHtml(html, context);

        Injections injections = collectInjections(enhancers);
        String highlightCss = isDarkTheme ? HLJS_DARK_CSS : HLJS_LIGHT_CSS;

        String styleBlock = isDarkTheme ? darkStyles() : lightStyles();
        String highlightScript = highlightScriptBlock();

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                    %s
                    <style>
                        %s
                        %s
                    </style>
                </head>
                <body>
                %s
                <script>
                    %s
                    %s
                </script>
                %s
                </body>
                </html>
                """.formatted(injections.head(), highlightCss, styleBlock, html, HLJS_SCRIPT, highlightScript,
                injections.body());
    }

    public String buildEmptyHtml(boolean isDarkTheme) {
        String fg = isDarkTheme ? "#B3B3B3" : "#71717A";
        String bg = isDarkTheme ? "#1E1E1E" : "#FFFFFF";
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
                    <style>
                        html, body { color: %s; background-color: %s; margin: 0; padding: 0; width: 100%%; height: 100%%; }
                        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; }
                    </style>
                </head>
                <body></body>
                </html>
                """.formatted(fg, bg);
    }

    private ProcessedMarkdown preprocessMarkdown(String markdown, PreviewContext context) {
        ProcessedMarkdown withEmbeds = replaceObsidianImageEmbedsWithTokens(markdown, context);
        return withEmbeds;
    }

    private ProcessedMarkdown replaceObsidianImageEmbedsWithTokens(String markdown, PreviewContext context) {
        Matcher matcher = OBSIDIAN_IMAGE_EMBED_PATTERN.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        Map<String, String> tokenToHtml = new HashMap<>();
        int tokenIndex = 0;
        while (matcher.find()) {
            String rawInner = matcher.group(1) != null ? matcher.group(1).trim() : "";
            String replacementHtml = buildObsidianEmbedReplacement(rawInner, context);
            String token = "@@FN_OBS_EMBED_" + tokenIndex++ + "@@";
            tokenToHtml.put(token, replacementHtml);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(token));
        }
        matcher.appendTail(sb);
        return new ProcessedMarkdown(sb.toString(), tokenToHtml);
    }

    private String buildObsidianEmbedReplacement(String rawInner, PreviewContext context) {
        if (rawInner.isBlank()) {
            return "<span class=\"fn-missing-embed\">[missing embed]</span>";
        }

        String[] parts = rawInner.split("\\|");
        String target = parts[0].trim();
        String width = null;
        String height = null;

        for (int i = 1; i < parts.length; i++) {
            String candidate = parts[i].trim();
            Matcher sizeMatcher = SIZE_TOKEN_PATTERN.matcher(candidate);
            if (sizeMatcher.matches()) {
                width = sizeMatcher.group(1);
                height = sizeMatcher.group(2);
                break;
            }
        }

        Path resolved = resolveLocalImagePath(target, context);
        if (resolved == null) {
            return "<span class=\"fn-missing-embed\">[missing: " + escapeHtml(target) + "]</span>";
        }

        StringBuilder attrs = new StringBuilder();
        if (width != null && !width.isBlank()) {
            attrs.append(" width=\"").append(escapeHtml(width)).append("\"");
        }
        if (height != null && !height.isBlank()) {
            attrs.append(" height=\"").append(escapeHtml(height)).append("\"");
        }

        String src = resolved.toUri().toString();
        String alt = resolved.getFileName() != null ? resolved.getFileName().toString() : target;
        return "<img src=\"" + escapeHtml(src) + "\" alt=\"" + escapeHtml(alt) + "\"" + attrs + ">";
    }

    private String applyTokenReplacements(String html, Map<String, String> tokenToHtml) {
        if (tokenToHtml == null || tokenToHtml.isEmpty()) {
            return html;
        }
        String resolved = html;
        for (Map.Entry<String, String> entry : tokenToHtml.entrySet()) {
            resolved = resolved.replace(entry.getKey(), entry.getValue());
        }
        return resolved;
    }

    private String resolveRelativeImageSourcesInHtml(String html, PreviewContext context) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        Matcher matcher = HTML_IMG_SRC_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String beforeSrc = matcher.group(1);
            String src = matcher.group(2);
            String afterSrc = matcher.group(3);

            String resolvedSrc = resolveImageSrc(src, context);
            String rebuiltTag = "<img" + beforeSrc + "src=\"" + escapeHtml(resolvedSrc) + "\"" + afterSrc + ">";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(rebuiltTag));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String resolveImageSrc(String src, PreviewContext context) {
        if (src == null || src.isBlank()) {
            return src;
        }
        String normalized = src.trim();

        if (normalized.startsWith("http://") || normalized.startsWith("https://")
                || normalized.startsWith("data:") || normalized.startsWith("file:")) {
            return normalized;
        }

        Path resolved = resolveLocalImagePath(normalized, context);
        if (resolved != null) {
            return resolved.toUri().toString();
        }
        return normalized;
    }

    private Path resolveLocalImagePath(String rawPath, PreviewContext context) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        String cleaned = rawPath.trim();
        if (cleaned.startsWith("<") && cleaned.endsWith(">") && cleaned.length() > 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        if (cleaned.startsWith("file:")) {
            try {
                Path candidate = Paths.get(new URI(cleaned));
                if (Files.isRegularFile(candidate)) {
                    return candidate.toAbsolutePath().normalize();
                }
            } catch (URISyntaxException | IllegalArgumentException e) {
                logger.fine("Invalid file URI in markdown preview: " + cleaned);
            }
        }

        Path direct = Paths.get(cleaned);
        if (direct.isAbsolute() && Files.isRegularFile(direct)) {
            return toRenderableImagePath(direct.toAbsolutePath().normalize());
        }

        if (context == null || !context.isFileSystemStorage() || context.filesystemRootDirectory() == null
                || context.filesystemRootDirectory().isBlank()) {
            return null;
        }

        Path root = Paths.get(context.filesystemRootDirectory()).toAbsolutePath().normalize();
        List<Path> baseCandidates = new ArrayList<>();

        if (context.noteId() != null && !context.noteId().isBlank() && context.noteId().contains("/")) {
            String parentId = context.noteId().substring(0, context.noteId().lastIndexOf('/'));
            baseCandidates.add(root.resolve(parentId.replace("/", File.separator)).normalize());
        }
        baseCandidates.add(root);

        List<String> relativeCandidates = buildPathCandidates(cleaned);
        for (Path base : baseCandidates) {
            for (String rel : relativeCandidates) {
                Path candidate = base.resolve(rel.replace("/", File.separator)).normalize();
                if (Files.isRegularFile(candidate)) {
                    return toRenderableImagePath(candidate);
                }
            }
            Path discovered = resolveByBasenameScan(base, cleaned);
            if (discovered != null) {
                return toRenderableImagePath(discovered);
            }
        }

        return null;
    }

    private List<String> buildPathCandidates(String cleanedPath) {
        List<String> candidates = new ArrayList<>();
        candidates.add(cleanedPath);

        int lastSlash = cleanedPath.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? cleanedPath.substring(lastSlash + 1) : cleanedPath;
        boolean hasExtension = fileName.contains(".");

        if (!hasExtension) {
            String[] imageExtensions = { ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".svg", ".heic", ".heif",
                    ".avif", ".tif", ".tiff", ".ico", ".apng", ".jxl" };
            for (String ext : imageExtensions) {
                candidates.add(cleanedPath + ext);
            }
        }

        return candidates;
    }

    private Path resolveByBasenameScan(Path baseDirectory, String rawRelativePath) {
        try {
            String normalized = rawRelativePath.replace("\\", "/");
            int slash = normalized.lastIndexOf('/');
            String parent = slash >= 0 ? normalized.substring(0, slash) : "";
            String baseName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
            if (baseName.isBlank()) {
                return null;
            }

            Path parentDir = parent.isBlank() ? baseDirectory : baseDirectory.resolve(parent.replace("/", File.separator));
            if (!Files.isDirectory(parentDir)) {
                return null;
            }

            try (var stream = Files.list(parentDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            int dot = name.lastIndexOf('.');
                            if (dot <= 0) {
                                return false;
                            }
                            return name.substring(0, dot).equalsIgnoreCase(baseName);
                        })
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            logger.fine("Failed basename scan for preview image resolution: " + e.getMessage());
            return null;
        }
    }

    private Path toRenderableImagePath(Path sourcePath) {
        if (sourcePath == null) {
            return null;
        }
        String lower = sourcePath.getFileName() != null ? sourcePath.getFileName().toString().toLowerCase() : "";
        if (!lower.endsWith(".heic") && !lower.endsWith(".heif")) {
            return sourcePath;
        }
        return convertHeicToPngForPreview(sourcePath);
    }

    private Path convertHeicToPngForPreview(Path sourcePath) {
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (!osName.contains("mac")) {
                return sourcePath;
            }

            String keySeed = sourcePath.toAbsolutePath().normalize() + "|" + Files.getLastModifiedTime(sourcePath).toMillis()
                    + "|" + Files.size(sourcePath);
            String key = Integer.toHexString(keySeed.hashCode());

            synchronized (PREVIEW_IMAGE_CACHE) {
                Path cached = PREVIEW_IMAGE_CACHE.get(key);
                if (cached != null && Files.isRegularFile(cached)) {
                    return cached;
                }
            }

            Path cacheDir = Paths.get(AppDataDirectory.getBaseDirectory(), "cache", "preview-images");
            Files.createDirectories(cacheDir);
            Path targetPng = cacheDir.resolve(key + ".png");
            if (Files.isRegularFile(targetPng)) {
                synchronized (PREVIEW_IMAGE_CACHE) {
                    PREVIEW_IMAGE_CACHE.put(key, targetPng);
                }
                return targetPng;
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "sips",
                    "-s", "format", "png",
                    sourcePath.toString(),
                    "--out", targetPng.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit == 0 && Files.isRegularFile(targetPng)) {
                synchronized (PREVIEW_IMAGE_CACHE) {
                    PREVIEW_IMAGE_CACHE.put(key, targetPng);
                }
                return targetPng;
            }
            logger.warning("Failed to convert HEIC image for preview: " + sourcePath + " (exit=" + exit + ")");
            return sourcePath;
        } catch (Exception e) {
            logger.warning("HEIC preview conversion failed for " + sourcePath + ": " + e.getMessage());
            return sourcePath;
        }
    }

    private Injections collectInjections(Collection<PreviewEnhancer> enhancers) {
        if (enhancers == null || enhancers.isEmpty()) {
            return new Injections("", "");
        }
        StringBuilder head = new StringBuilder();
        StringBuilder body = new StringBuilder();

        for (PreviewEnhancer enhancer : enhancers) {
            if (enhancer == null) {
                continue;
            }
            try {
                String headInjection = enhancer.getHeadInjections();
                if (headInjection != null && !headInjection.isBlank()) {
                    head.append(headInjection).append("\n");
                }
            } catch (Exception e) {
                logger.warning("Preview enhancer head injection failed: " + e.getMessage());
            }
            try {
                String bodyInjection = enhancer.getBodyInjections();
                if (bodyInjection != null && !bodyInjection.isBlank()) {
                    body.append(bodyInjection).append("\n");
                }
            } catch (Exception e) {
                logger.warning("Preview enhancer body injection failed: " + e.getMessage());
            }
        }
        return new Injections(head.toString(), body.toString());
    }

    private String darkStyles() {
        return """
                html { background-color: #1E1E1E; margin: 0; padding: 0; width: 100%; height: 100%; }
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; padding: 20px; line-height: 1.6; color: #E0E0E0; background-color: #1E1E1E; }
                h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 600; color: #FFFFFF; }
                h1 { font-size: 2em; border-bottom: 2px solid #3a3a3a; padding-bottom: 0.3em; }
                h2 { font-size: 1.5em; border-bottom: 1px solid #3a3a3a; padding-bottom: 0.3em; }
                h3 { font-size: 1.25em; }
                code:not(pre code) { background-color: #2d2d2d; color: #ce9178; padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 0.9em; }
                pre { background-color: #1e1e1e; border: 1px solid #3a3a3a; border-radius: 6px; margin: 1em 0; overflow-x: auto; position: relative; }
                pre code { font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 13px; line-height: 1.5; padding: 16px !important; display: block; background: transparent !important; color: inherit; }
                .hljs { background: transparent !important; }
                blockquote { border-left: 4px solid #818CF8; margin: 0; padding-left: 20px; color: #B3B3B3; background-color: #252525; padding: 10px 20px; border-radius: 4px; }
                ul, ol { margin: 1em 0; padding-left: 2em; color: #E0E0E0; }
                li { margin: 0.5em 0; }
                table { border-collapse: collapse; width: 100%; margin: 1em 0; }
                table th, table td { border: 1px solid #3a3a3a; padding: 10px; text-align: left; }
                table th { background-color: #252525; font-weight: 600; color: #FFFFFF; }
                table td { background-color: #1E1E1E; color: #E0E0E0; }
                a { color: #818CF8; text-decoration: none; }
                a:hover { color: #A5B4FC; text-decoration: underline; }
                img { max-width: 100%; height: auto; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.3); }
                .fn-missing-embed { color: #d4a373; font-style: italic; }
                hr { border: none; border-top: 1px solid #3a3a3a; margin: 2em 0; }
                strong { color: #FFFFFF; font-weight: 600; }
                mark { background-color: #564a00; color: #ffd700; padding: 1px 3px; border-radius: 2px; }
                """;
    }

    private String lightStyles() {
        return """
                html { background-color: #FFFFFF; margin: 0; padding: 0; width: 100%; height: 100%; }
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; padding: 20px; line-height: 1.6; color: #24292e; background-color: #FFFFFF; }
                h1, h2, h3, h4, h5, h6 { margin-top: 1.5em; margin-bottom: 0.5em; font-weight: 600; color: #24292e; }
                h1 { font-size: 2em; border-bottom: 2px solid #eaecef; padding-bottom: 0.3em; }
                h2 { font-size: 1.5em; border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
                h3 { font-size: 1.25em; }
                code:not(pre code) { background-color: #f0f0f0; color: #d63384; padding: 2px 6px; border-radius: 4px; font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 0.9em; }
                pre { background-color: #f8f8f8; border: 1px solid #e1e4e8; border-radius: 6px; margin: 1em 0; overflow-x: auto; position: relative; }
                pre code { font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace; font-size: 13px; line-height: 1.5; padding: 16px !important; display: block; background: transparent !important; color: inherit; }
                .hljs { background: transparent !important; }
                blockquote { border-left: 4px solid #6366F1; margin: 0; padding-left: 20px; color: #57606a; background-color: #f6f8fa; padding: 10px 20px; border-radius: 4px; }
                ul, ol { margin: 1em 0; padding-left: 2em; color: #24292e; }
                li { margin: 0.5em 0; }
                table { border-collapse: collapse; width: 100%; margin: 1em 0; }
                table th, table td { border: 1px solid #e1e4e8; padding: 10px; text-align: left; }
                table th { background-color: #f6f8fa; font-weight: 600; color: #24292e; }
                table td { background-color: #FFFFFF; color: #24292e; }
                a { color: #0969da; text-decoration: none; }
                a:hover { color: #0550ae; text-decoration: underline; }
                img { max-width: 100%; height: auto; border-radius: 6px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                .fn-missing-embed { color: #8a5a44; font-style: italic; }
                hr { border: none; border-top: 1px solid #e1e4e8; margin: 2em 0; }
                strong { color: #24292e; font-weight: 600; }
                mark { background-color: #fff8c5; padding: 1px 3px; border-radius: 2px; }
                """;
    }

    private record Injections(String head, String body) {
    }

    private record ProcessedMarkdown(String markdown, Map<String, String> tokenToHtml) {
    }

    private static String loadResourceText(String resourcePath) {
        try (InputStream in = PreviewWorkflow.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                logger.warning("Preview asset not found: " + resourcePath);
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warning("Failed to read preview asset " + resourcePath + ": " + e.getMessage());
            return "";
        }
    }

    private String highlightScriptBlock() {
        return """
                if (typeof hljs !== 'undefined' && hljs && hljs.highlightElement) {
                    document.querySelectorAll('pre code').forEach(function(block) {
                        hljs.highlightElement(block);
                    });
                }
                """;
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
