package com.example.forevernote.ui.workflow.graph;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.service.links.LinkIndexService;
import com.example.forevernote.service.links.LinkIndexService.LinkEdge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * Builds graph data models from notes/link index.
 */
public class GraphWorkflow {

    public record GraphFilter(Set<String> folderPrefixes, Set<String> tags, boolean includeUnresolved, int maxNodes) {
    }

    public record GraphNode(String id, String title, boolean unresolved, double x, double y) {
    }

    public record GraphEdge(String sourceId, String targetId, boolean unresolved, boolean embed) {
    }

    public record GraphData(List<GraphNode> nodes, List<GraphEdge> edges) {
    }

    public GraphData buildGlobalGraph(Collection<Note> notes, LinkIndexService linkIndexService, GraphFilter filter) {
        if (notes == null || linkIndexService == null) {
            return new GraphData(List.of(), List.of());
        }
        Map<String, Note> notesById = new HashMap<>();
        for (Note n : notes) {
            if (n != null && n.getId() != null) {
                if (acceptNote(n, filter)) {
                    notesById.put(n.getId(), n);
                }
            }
        }

        List<GraphNode> nodes = new ArrayList<>();
        Map<String, GraphNode> nodeById = new HashMap<>();
        List<GraphEdge> edges = new ArrayList<>();

        for (Note note : notesById.values()) {
            GraphNode node = new GraphNode(note.getId(), note.getTitle(), false, 0, 0);
            nodes.add(node);
            nodeById.put(node.id(), node);
        }

        for (Note note : notesById.values()) {
            List<LinkEdge> outgoing = linkIndexService.getOutgoing(note.getId());
            for (LinkEdge edge : outgoing) {
                if (edge.targetNoteId() == null) {
                    if (filter != null && !filter.includeUnresolved()) {
                        continue;
                    }
                    String unresolvedId = "unresolved::" + edge.rawTarget();
                    GraphNode unresolvedNode = nodeById.get(unresolvedId);
                    if (unresolvedNode == null) {
                        unresolvedNode = new GraphNode(unresolvedId, edge.rawTarget(), true, 0, 0);
                        nodes.add(unresolvedNode);
                        nodeById.put(unresolvedId, unresolvedNode);
                    }
                    edges.add(new GraphEdge(note.getId(), unresolvedNode.id(), true, edge.embed()));
                    continue;
                }
                if (!notesById.containsKey(edge.targetNoteId())) {
                    continue;
                }
                edges.add(new GraphEdge(edge.sourceNoteId(), edge.targetNoteId(), false, edge.embed()));
            }
        }

        applyLayout(nodes, edges);
        return trim(nodes, edges, filter != null ? filter.maxNodes() : 0);
    }

    public GraphData buildLocalGraph(String centerNoteId, int depth, Collection<Note> notes,
            LinkIndexService linkIndexService, GraphFilter filter) {
        if (centerNoteId == null || centerNoteId.isBlank() || notes == null || linkIndexService == null) {
            return new GraphData(List.of(), List.of());
        }
        Map<String, Note> notesById = notes.stream()
                .filter(Objects::nonNull)
                .filter(n -> n.getId() != null)
                .collect(HashMap::new, (m, n) -> m.put(n.getId(), n), HashMap::putAll);

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> level = new HashMap<>();
        queue.add(centerNoteId);
        level.put(centerNoteId, 0);
        visited.add(centerNoteId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentLevel = level.getOrDefault(current, 0);
            if (currentLevel >= Math.max(1, depth)) {
                continue;
            }
            for (LinkEdge e : linkIndexService.getOutgoing(current)) {
                if (e.targetNoteId() == null) {
                    continue;
                }
                if (visited.add(e.targetNoteId())) {
                    level.put(e.targetNoteId(), currentLevel + 1);
                    queue.add(e.targetNoteId());
                }
            }
            for (LinkEdge e : linkIndexService.getIncoming(current)) {
                if (e.sourceNoteId() == null) {
                    continue;
                }
                if (visited.add(e.sourceNoteId())) {
                    level.put(e.sourceNoteId(), currentLevel + 1);
                    queue.add(e.sourceNoteId());
                }
            }
        }

        List<Note> localNotes = visited.stream().map(notesById::get).filter(Objects::nonNull).toList();
        return buildGlobalGraph(localNotes, linkIndexService, filter);
    }

    private boolean acceptNote(Note note, GraphFilter filter) {
        if (note == null) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        if (filter.folderPrefixes() != null && !filter.folderPrefixes().isEmpty()) {
            String id = note.getId() != null ? note.getId() : "";
            boolean match = filter.folderPrefixes().stream().anyMatch(prefix -> id.startsWith(prefix));
            if (!match) {
                return false;
            }
        }
        if (filter.tags() != null && !filter.tags().isEmpty()) {
            boolean hasTag = note.getTags().stream().anyMatch(t -> filter.tags().contains(t.getTitle()));
            if (!hasTag) {
                return false;
            }
        }
        return true;
    }

    private GraphData trim(List<GraphNode> nodes, List<GraphEdge> edges, int maxNodes) {
        if (maxNodes <= 0 || maxNodes >= nodes.size()) {
            return new GraphData(nodes, edges);
        }
        List<GraphNode> reducedNodes = new ArrayList<>(nodes.subList(0, maxNodes));
        Set<String> allowed = reducedNodes.stream().map(GraphNode::id).collect(HashSet::new, Set::add, Set::addAll);
        List<GraphEdge> reducedEdges = edges.stream()
                .filter(e -> allowed.contains(e.sourceId()) && allowed.contains(e.targetId()))
                .toList();
        return new GraphData(reducedNodes, reducedEdges);
    }

    private void applyLayout(List<GraphNode> nodes, List<GraphEdge> edges) {
        if (nodes.isEmpty()) {
            return;
        }
        // For large vaults, use a deterministic O(n) layout inspired by Obsidian-like
        // overview behavior (dense, navigable, no n^2 force simulation lag).
        if (nodes.size() > 220) {
            applyFastLayout(nodes, edges);
            return;
        }

        Random random = new Random(42);
        Map<String, double[]> pos = new HashMap<>();
        for (GraphNode node : nodes) {
            pos.put(node.id(), new double[] { 80 + random.nextDouble() * 720, 80 + random.nextDouble() * 480 });
        }

        int iterations = Math.min(90, 24 + (nodes.size() / 2));
        double k = Math.sqrt((800.0 * 600.0) / Math.max(1, nodes.size()));

        for (int i = 0; i < iterations; i++) {
            Map<String, double[]> disp = new HashMap<>();
            for (GraphNode v : nodes) {
                disp.put(v.id(), new double[] { 0.0, 0.0 });
            }

            for (int a = 0; a < nodes.size(); a++) {
                GraphNode v = nodes.get(a);
                for (int b = a + 1; b < nodes.size(); b++) {
                    GraphNode u = nodes.get(b);
                    double[] pv = pos.get(v.id());
                    double[] pu = pos.get(u.id());
                    double dx = pv[0] - pu[0];
                    double dy = pv[1] - pu[1];
                    double dist = Math.max(1.0, Math.hypot(dx, dy));
                    double force = (k * k) / dist;
                    double fx = dx / dist * force;
                    double fy = dy / dist * force;
                    disp.get(v.id())[0] += fx;
                    disp.get(v.id())[1] += fy;
                    disp.get(u.id())[0] -= fx;
                    disp.get(u.id())[1] -= fy;
                }
            }

            for (GraphEdge e : edges) {
                if (!pos.containsKey(e.sourceId()) || !pos.containsKey(e.targetId())) {
                    continue;
                }
                double[] ps = pos.get(e.sourceId());
                double[] pt = pos.get(e.targetId());
                double dx = ps[0] - pt[0];
                double dy = ps[1] - pt[1];
                double dist = Math.max(1.0, Math.hypot(dx, dy));
                double force = (dist * dist) / k;
                double fx = dx / dist * force;
                double fy = dy / dist * force;
                disp.get(e.sourceId())[0] -= fx;
                disp.get(e.sourceId())[1] -= fy;
                disp.get(e.targetId())[0] += fx;
                disp.get(e.targetId())[1] += fy;
            }

            double temperature = Math.max(1.0, 20.0 - i * 0.12);
            for (GraphNode v : nodes) {
                double[] p = pos.get(v.id());
                double[] d = disp.get(v.id());
                double dist = Math.max(1.0, Math.hypot(d[0], d[1]));
                p[0] += (d[0] / dist) * Math.min(Math.abs(d[0]), temperature);
                p[1] += (d[1] / dist) * Math.min(Math.abs(d[1]), temperature);
                p[0] = clamp(p[0], 20, 980);
                p[1] = clamp(p[1], 20, 720);
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            GraphNode n = nodes.get(i);
            double[] p = pos.get(n.id());
            nodes.set(i, new GraphNode(n.id(), n.title(), n.unresolved(), p[0], p[1]));
        }
    }

    private void applyFastLayout(List<GraphNode> nodes, List<GraphEdge> edges) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (GraphNode n : nodes) {
            adjacency.put(n.id(), new HashSet<>());
        }
        for (GraphEdge e : edges) {
            if (!adjacency.containsKey(e.sourceId()) || !adjacency.containsKey(e.targetId())) {
                continue;
            }
            adjacency.get(e.sourceId()).add(e.targetId());
            adjacency.get(e.targetId()).add(e.sourceId());
        }

        List<String> isolated = new ArrayList<>();
        Set<String> nonIsolatedSet = new HashSet<>();
        for (GraphNode node : nodes) {
            if (adjacency.getOrDefault(node.id(), Set.of()).isEmpty()) {
                isolated.add(node.id());
            } else {
                nonIsolatedSet.add(node.id());
            }
        }

        Random rng = new Random(42);
        double centerX = 500.0;
        double centerY = 380.0;
        double radius = Math.max(260.0, 190.0 + Math.sqrt(nodes.size()) * 8.0);
        Map<String, double[]> globalPos = new HashMap<>();

        // Build connected components for non-isolated nodes.
        List<List<String>> components = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String id : nonIsolatedSet) {
            if (!visited.add(id)) {
                continue;
            }
            ArrayDeque<String> dq = new ArrayDeque<>();
            dq.add(id);
            List<String> comp = new ArrayList<>();
            while (!dq.isEmpty()) {
                String cur = dq.poll();
                comp.add(cur);
                for (String nb : adjacency.getOrDefault(cur, Set.of())) {
                    if (nonIsolatedSet.contains(nb) && visited.add(nb)) {
                        dq.add(nb);
                    }
                }
            }
            components.add(comp);
        }
        components.sort((a, b) -> Integer.compare(b.size(), a.size()));

        // Place component anchors across disk (instead of single center cluster).
        List<double[]> anchors = new ArrayList<>();
        for (List<String> comp : components) {
            double compRadius = 10.0 + Math.sqrt(comp.size()) * 4.8;
            double[] anchor = null;
            for (int attempt = 0; attempt < 120; attempt++) {
                double angle = rng.nextDouble() * Math.PI * 2.0;
                double r = Math.sqrt(rng.nextDouble()) * (radius * 0.88);
                double x = centerX + Math.cos(angle) * r;
                double y = centerY + Math.sin(angle) * r;
                boolean ok = true;
                for (double[] prev : anchors) {
                    double minDist = (prev[2] + compRadius) * 0.72;
                    if (Math.hypot(x - prev[0], y - prev[1]) < minDist) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    anchor = new double[] { x, y, compRadius };
                    break;
                }
            }
            if (anchor == null) {
                double angle = rng.nextDouble() * Math.PI * 2.0;
                double r = Math.sqrt(rng.nextDouble()) * (radius * 0.9);
                anchor = new double[] { centerX + Math.cos(angle) * r, centerY + Math.sin(angle) * r, compRadius };
            }
            anchors.add(anchor);
        }

        // Place connected components with force-based internal geometry.
        for (int ci = 0; ci < components.size(); ci++) {
            List<String> comp = components.get(ci);
            double[] anchor = anchors.get(ci);
            Map<String, double[]> local = forceLayoutComponent(comp, adjacency, rng);
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (double[] p : local.values()) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
            double w = Math.max(1.0, maxX - minX);
            double h = Math.max(1.0, maxY - minY);
            double scale = Math.max(0.8, anchor[2] / Math.max(w, h));
            double localCx = (minX + maxX) * 0.5;
            double localCy = (minY + maxY) * 0.5;
            for (String nodeId : comp) {
                double[] p = local.get(nodeId);
                double x = anchor[0] + (p[0] - localCx) * scale;
                double y = anchor[1] + (p[1] - localCy) * scale;
                globalPos.put(nodeId, new double[] { x, y });
            }
        }

        if (!isolated.isEmpty()) {
            List<String> shuffled = new ArrayList<>(isolated);
            Collections.shuffle(shuffled, rng);
            int total = shuffled.size();
            double cell = Math.max(8.0, Math.min(15.0, Math.sqrt((Math.PI * radius * radius) / Math.max(1, total))));
            int grid = Math.max(10, (int) Math.ceil((radius * 2) / cell));
            List<double[]> candidates = new ArrayList<>(grid * grid);
            for (int gy = 0; gy <= grid; gy++) {
                for (int gx = 0; gx <= grid; gx++) {
                    double x = centerX - radius + gx * cell;
                    double y = centerY - radius + gy * cell;
                    if (Math.hypot(x - centerX, y - centerY) <= radius * 0.985) {
                        x += (rng.nextDouble() - 0.5) * cell * 0.45;
                        y += (rng.nextDouble() - 0.5) * cell * 0.45;
                        candidates.add(new double[] { x, y });
                    }
                }
            }
            Collections.shuffle(candidates, rng);
            int take = Math.min(total, candidates.size());
            for (int i = 0; i < take; i++) {
                globalPos.put(shuffled.get(i), candidates.get(i));
            }
            for (int i = take; i < total; i++) {
                double angle = rng.nextDouble() * Math.PI * 2.0;
                double r = Math.sqrt(rng.nextDouble()) * radius;
                globalPos.put(shuffled.get(i), new double[] {
                        centerX + Math.cos(angle) * r,
                        centerY + Math.sin(angle) * r
                });
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            GraphNode n = nodes.get(i);
            double[] p = globalPos.getOrDefault(n.id(), new double[] { 500, 380 });
            nodes.set(i, new GraphNode(n.id(), n.title(), n.unresolved(), p[0], p[1]));
        }
    }

    private Map<String, double[]> forceLayoutComponent(List<String> nodes, Map<String, Set<String>> adjacency, Random rng) {
        Map<String, double[]> pos = new HashMap<>();
        for (String id : nodes) {
            pos.put(id, new double[] { 60 + rng.nextDouble() * 320, 60 + rng.nextDouble() * 320 });
        }
        int n = nodes.size();
        int iterations = Math.min(140, 46 + (int) Math.sqrt(n) * 8);
        int repulsionSamples = Math.min(56, Math.max(12, (int) Math.sqrt(n) * 4));
        double k = Math.max(16.0, 125.0 / Math.sqrt(Math.max(2, n)));

        for (int it = 0; it < iterations; it++) {
            Map<String, double[]> disp = new HashMap<>();
            for (String id : nodes) {
                disp.put(id, new double[] { 0.0, 0.0 });
            }

            // Attractive forces along real edges.
            for (String src : nodes) {
                double[] ps = pos.get(src);
                for (String dst : adjacency.getOrDefault(src, Set.of())) {
                    if (!pos.containsKey(dst) || src.compareTo(dst) >= 0) {
                        continue;
                    }
                    double[] pt = pos.get(dst);
                    double dx = ps[0] - pt[0];
                    double dy = ps[1] - pt[1];
                    double dist = Math.max(1.0, Math.hypot(dx, dy));
                    double force = (dist * dist) / k;
                    double fx = (dx / dist) * force;
                    double fy = (dy / dist) * force;
                    disp.get(src)[0] -= fx;
                    disp.get(src)[1] -= fy;
                    disp.get(dst)[0] += fx;
                    disp.get(dst)[1] += fy;
                }
            }

            // Approximate repulsion by random sampling for scalability.
            for (int i = 0; i < n; i++) {
                String vi = nodes.get(i);
                double[] pv = pos.get(vi);
                for (int s = 0; s < repulsionSamples; s++) {
                    String uj = nodes.get(rng.nextInt(n));
                    if (vi.equals(uj)) {
                        continue;
                    }
                    double[] pu = pos.get(uj);
                    double dx = pv[0] - pu[0];
                    double dy = pv[1] - pu[1];
                    double dist = Math.max(1.0, Math.hypot(dx, dy));
                    double force = (k * k) / dist;
                    disp.get(vi)[0] += (dx / dist) * force;
                    disp.get(vi)[1] += (dy / dist) * force;
                }
            }

            double temperature = Math.max(0.8, 12.0 - it * 0.14);
            for (String id : nodes) {
                double[] p = pos.get(id);
                double[] d = disp.get(id);
                double len = Math.max(1.0, Math.hypot(d[0], d[1]));
                p[0] += (d[0] / len) * Math.min(temperature, Math.abs(d[0]));
                p[1] += (d[1] / len) * Math.min(temperature, Math.abs(d[1]));
                // mild gravity to avoid drift
                p[0] += (180.0 - p[0]) * 0.004;
                p[1] += (180.0 - p[1]) * 0.004;
            }
        }
        return pos;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
