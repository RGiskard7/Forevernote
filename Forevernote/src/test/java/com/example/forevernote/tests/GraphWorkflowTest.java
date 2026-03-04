package com.example.forevernote.tests;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.service.links.LinkIndexService;
import com.example.forevernote.ui.workflow.graph.GraphWorkflow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphWorkflowTest {

    @Test
    void buildsGlobalAndLocalGraphs() {
        Note n1 = new Note("A", "[[B]] [[C]]");
        n1.setId("A.md");
        Note n2 = new Note("B", "[[A]]");
        n2.setId("B.md");
        Note n3 = new Note("C", "");
        n3.setId("C.md");

        LinkIndexService links = new LinkIndexService();
        links.rebuildIndex(List.of(n1, n2, n3));

        GraphWorkflow workflow = new GraphWorkflow();
        GraphWorkflow.GraphFilter filter = new GraphWorkflow.GraphFilter(Set.of(), Set.of(), true, 200);

        GraphWorkflow.GraphData global = workflow.buildGlobalGraph(List.of(n1, n2, n3), links, filter);
        assertFalse(global.nodes().isEmpty());
        assertFalse(global.edges().isEmpty());

        GraphWorkflow.GraphData local = workflow.buildLocalGraph("A.md", 1, List.of(n1, n2, n3), links, filter);
        assertFalse(local.nodes().isEmpty());
        assertTrue(local.nodes().stream().anyMatch(n -> "A.md".equals(n.id())));
    }
}
