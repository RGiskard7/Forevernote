package com.example.forevernote.service.links;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Obsidian-style wikilinks and embeds.
 */
public class ObsidianLinkParser {

    private static final Pattern LINK_PATTERN = Pattern.compile("(!)?\\[\\[([^\\]]+)\\]\\]");
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("!?\\[[^\\]]*\\]\\(([^)]+)\\)");

    public record ParsedLink(boolean embed, String rawTarget, String target, String alias) {
    }

    public record ParsedLinks(List<ParsedLink> links) {
    }

    public ParsedLinks parse(String markdown) {
        String text = markdown != null ? markdown : "";
        List<ParsedLink> links = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Matcher matcher = LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            boolean embed = "!".equals(matcher.group(1));
            String rawInner = matcher.group(2) != null ? matcher.group(2).trim() : "";
            if (rawInner.isBlank()) {
                continue;
            }
            String[] parts = rawInner.split("\\|", 2);
            String target = parts[0].trim();
            String alias = parts.length > 1 ? parts[1].trim() : "";
            if (target.isBlank()) {
                continue;
            }
            String normalized = normalizeTarget(target);
            String dedupeKey = "wiki|" + embed + "|" + normalized + "|" + alias;
            if (seen.add(dedupeKey)) {
                links.add(new ParsedLink(embed, rawInner, normalized, alias));
            }
        }

        Matcher mdMatcher = MARKDOWN_LINK_PATTERN.matcher(text);
        while (mdMatcher.find()) {
            String rawTarget = mdMatcher.group(1) != null ? mdMatcher.group(1).trim() : "";
            if (rawTarget.isBlank()) {
                continue;
            }
            String normalized = normalizeTarget(rawTarget);
            if (normalized.isBlank()) {
                continue;
            }
            String lower = normalized.toLowerCase();
            if (lower.startsWith("http://")
                    || lower.startsWith("https://")
                    || lower.startsWith("mailto:")
                    || lower.startsWith("obsidian://")
                    || lower.startsWith("#")) {
                continue;
            }
            // Keep local markdown-like targets only.
            if (lower.contains("://")) {
                continue;
            }
            String dedupeKey = "md|false|" + normalized;
            if (seen.add(dedupeKey)) {
                links.add(new ParsedLink(false, rawTarget, normalized, ""));
            }
        }
        return new ParsedLinks(links);
    }

    private String normalizeTarget(String target) {
        String normalized = target.trim().replace("\\", "/");
        int query = normalized.indexOf('?');
        if (query >= 0) {
            normalized = normalized.substring(0, query).trim();
        }
        int heading = normalized.indexOf('#');
        if (heading >= 0) {
            normalized = normalized.substring(0, heading).trim();
        }
        if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("'") && normalized.endsWith("'"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}
