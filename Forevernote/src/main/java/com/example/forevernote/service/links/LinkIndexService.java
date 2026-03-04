package com.example.forevernote.service.links;

import com.example.forevernote.data.models.Note;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Incremental index for outgoing/incoming links with unresolved tracking.
 */
public class LinkIndexService {

    public record LinkEdge(String sourceNoteId, String targetNoteId, String rawTarget, String alias, boolean embed,
            boolean unresolved, boolean ambiguous) {
    }

    public record Resolution(String targetNoteId, boolean unresolved, boolean ambiguous) {
    }

    private final ObsidianLinkParser parser;

    private final Map<String, Note> notesById = new HashMap<>();
    private final Map<String, String> noteIdByNormalizedPath = new HashMap<>();
    private final Map<String, Set<LinkEdge>> outgoingBySource = new HashMap<>();
    private final Map<String, Set<LinkEdge>> incomingByTarget = new HashMap<>();
    private final Set<LinkEdge> unresolvedEdges = new HashSet<>();

    public LinkIndexService() {
        this(new ObsidianLinkParser());
    }

    public LinkIndexService(ObsidianLinkParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public synchronized void rebuildIndex(Collection<Note> notes) {
        notesById.clear();
        noteIdByNormalizedPath.clear();
        outgoingBySource.clear();
        incomingByTarget.clear();
        unresolvedEdges.clear();

        if (notes == null) {
            return;
        }
        for (Note note : notes) {
            if (note == null || note.getId() == null || note.getId().isBlank()) {
                continue;
            }
            notesById.put(note.getId(), note);
            noteIdByNormalizedPath.put(normalizePathKey(note.getId()), note.getId());
            noteIdByNormalizedPath.put(normalizePathKey(note.getTitle()), note.getId());
            String noteIdNoExt = stripKnownExtension(note.getId());
            noteIdByNormalizedPath.put(normalizePathKey(noteIdNoExt), note.getId());
            String titleNoExt = stripKnownExtension(note.getTitle());
            noteIdByNormalizedPath.put(normalizePathKey(titleNoExt), note.getId());
        }

        for (Note note : notesById.values()) {
            reindexNote(note);
        }
    }

    public synchronized void reindexNote(Note note) {
        if (note == null || note.getId() == null || note.getId().isBlank()) {
            return;
        }
        notesById.put(note.getId(), note);
        noteIdByNormalizedPath.put(normalizePathKey(note.getId()), note.getId());
        noteIdByNormalizedPath.put(normalizePathKey(note.getTitle()), note.getId());

        Set<LinkEdge> previous = outgoingBySource.remove(note.getId());
        if (previous != null) {
            for (LinkEdge edge : previous) {
                if (edge.targetNoteId() != null) {
                    Set<LinkEdge> incoming = incomingByTarget.get(edge.targetNoteId());
                    if (incoming != null) {
                        incoming.remove(edge);
                        if (incoming.isEmpty()) {
                            incomingByTarget.remove(edge.targetNoteId());
                        }
                    }
                }
                unresolvedEdges.remove(edge);
            }
        }

        ObsidianLinkParser.ParsedLinks parsed = parser.parse(note.getContent());
        Set<LinkEdge> newEdges = new HashSet<>();
        for (ObsidianLinkParser.ParsedLink parsedLink : parsed.links()) {
            Resolution resolution = resolveTarget(parsedLink.target(), note.getId());
            LinkEdge edge = new LinkEdge(
                    note.getId(),
                    resolution.targetNoteId(),
                    parsedLink.target(),
                    parsedLink.alias(),
                    parsedLink.embed(),
                    resolution.unresolved(),
                    resolution.ambiguous());
            newEdges.add(edge);
            if (edge.targetNoteId() != null) {
                incomingByTarget.computeIfAbsent(edge.targetNoteId(), k -> new HashSet<>()).add(edge);
            } else {
                unresolvedEdges.add(edge);
            }
        }
        outgoingBySource.put(note.getId(), newEdges);
    }

    public synchronized List<LinkEdge> getOutgoing(String noteId) {
        return outgoingBySource.getOrDefault(noteId, Set.of()).stream().toList();
    }

    public synchronized List<LinkEdge> getIncoming(String noteId) {
        return incomingByTarget.getOrDefault(noteId, Set.of()).stream().toList();
    }

    public synchronized List<LinkEdge> getUnresolved() {
        return unresolvedEdges.stream().toList();
    }

    public synchronized Resolution resolveTarget(String rawTarget, String sourceNoteId) {
        if (rawTarget == null || rawTarget.isBlank()) {
            return new Resolution(null, true, false);
        }

        String normalized = normalizePathKey(rawTarget);
        String exact = noteIdByNormalizedPath.get(normalized);
        if (exact != null) {
            return new Resolution(exact, false, false);
        }

        String sourceDir = sourceNoteId != null && sourceNoteId.contains("/")
                ? sourceNoteId.substring(0, sourceNoteId.lastIndexOf('/'))
                : "";
        if (!sourceDir.isBlank()) {
            String relative = normalizePathKey(sourceDir + "/" + rawTarget);
            String relativeMatch = noteIdByNormalizedPath.get(relative);
            if (relativeMatch != null) {
                return new Resolution(relativeMatch, false, false);
            }
        }

        String targetNoExt = stripKnownExtension(rawTarget);
        List<String> candidates = noteIdByNormalizedPath.entrySet().stream()
                .filter(e -> stripKnownExtension(e.getKey()).equals(normalizePathKey(targetNoExt)))
                .map(Map.Entry::getValue)
                .distinct()
                .collect(Collectors.toList());
        if (candidates.size() == 1) {
            return new Resolution(candidates.get(0), false, false);
        }
        if (candidates.size() > 1) {
            return new Resolution(null, true, true);
        }
        return new Resolution(null, true, false);
    }

    public synchronized Map<String, List<LinkEdge>> outgoingIndexSnapshot() {
        Map<String, List<LinkEdge>> snapshot = new HashMap<>();
        for (Map.Entry<String, Set<LinkEdge>> entry : outgoingBySource.entrySet()) {
            snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return snapshot;
    }

    private String normalizePathKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replace("\\", "/");
        if (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String stripKnownExtension(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        String lower = v.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt")) {
            return v.substring(0, v.lastIndexOf('.'));
        }
        return v;
    }
}
