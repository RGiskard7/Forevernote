package com.example.forevernote.tests;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.service.links.LinkIndexService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkResolutionTest {

    @Test
    void resolvesByPathAndByNameAndFlagsUnresolved() {
        LinkIndexService service = new LinkIndexService();

        Note source = new Note("root/source.md", "Source", "[[folder/target.md]] [[target2]] [[missing]]", null, null);
        source.setId("root/source.md");
        Note target1 = new Note("folder/target.md", "Target", "", null, null);
        target1.setId("folder/target.md");
        Note target2 = new Note("target2.md", "target2", "", null, null);
        target2.setId("target2.md");

        service.rebuildIndex(List.of(source, target1, target2));

        assertFalse(service.getOutgoing("root/source.md").isEmpty());
        assertEquals(3, service.getOutgoing("root/source.md").size());

        LinkIndexService.Resolution resolvedByPath = service.resolveTarget("folder/target.md", "root/source.md");
        assertEquals("folder/target.md", resolvedByPath.targetNoteId());
        assertFalse(resolvedByPath.unresolved());

        LinkIndexService.Resolution resolvedByName = service.resolveTarget("target2", "root/source.md");
        assertEquals("target2.md", resolvedByName.targetNoteId());
        assertFalse(resolvedByName.unresolved());

        LinkIndexService.Resolution unresolved = service.resolveTarget("unknown", "root/source.md");
        assertTrue(unresolved.unresolved());
        assertEquals(null, unresolved.targetNoteId());

        assertNotNull(service.getUnresolved());
        assertTrue(service.getUnresolved().stream().anyMatch(e -> "missing".equals(e.rawTarget())));
    }
}
