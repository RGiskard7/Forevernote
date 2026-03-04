package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.forevernote.plugin.PreviewEnhancer;
import com.example.forevernote.ui.workflow.PreviewWorkflow;

class PreviewWorkflowTest {

    @Test
    void buildPreviewHtmlIncludesMarkdownAndEnhancerInjections() {
        PreviewWorkflow workflow = new PreviewWorkflow();
        PreviewEnhancer enhancer = new PreviewEnhancer() {
            @Override
            public String getHeadInjections() {
                return "<meta name=\"x-test\" content=\"1\">";
            }

            @Override
            public String getBodyInjections() {
                return "<script>window.__enhancerLoaded=true;</script>";
            }
        };

        String html = workflow.buildPreviewHtml("# Title", false, List.of(enhancer));

        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("x-test"));
        assertTrue(html.contains("__enhancerLoaded"));
        assertTrue(!html.contains("cdnjs.cloudflare.com"));
        assertTrue(html.contains("typeof hljs !== 'undefined'"));
    }

    @Test
    void buildPreviewHtmlSurvivesBrokenEnhancer() {
        PreviewWorkflow workflow = new PreviewWorkflow();
        PreviewEnhancer broken = new PreviewEnhancer() {
            @Override
            public String getHeadInjections() {
                throw new RuntimeException("boom");
            }
        };
        PreviewEnhancer ok = new PreviewEnhancer() {
            @Override
            public String getBodyInjections() {
                return "<script>window.__ok=true;</script>";
            }
        };

        String html = workflow.buildPreviewHtml("text", true, List.of(broken, ok));

        assertTrue(html.contains("window.__ok=true"));
    }

    @Test
    void buildEmptyHtmlUsesThemeColors() {
        PreviewWorkflow workflow = new PreviewWorkflow();
        String dark = workflow.buildEmptyHtml(true);
        String light = workflow.buildEmptyHtml(false);

        assertTrue(dark.contains("#1E1E1E"));
        assertTrue(light.contains("#FFFFFF"));
    }

    @Test
    void buildPreviewHtmlResolvesObsidianImageEmbedFromNoteFolder(@TempDir Path tempDir) throws Exception {
        Path folder = tempDir.resolve("docs");
        Path images = folder.resolve("images");
        Files.createDirectories(images);
        Path image = images.resolve("diagram.png");
        Files.writeString(image, "fake-image");

        PreviewWorkflow workflow = new PreviewWorkflow();
        PreviewWorkflow.PreviewContext context = new PreviewWorkflow.PreviewContext(
                "filesystem",
                tempDir.toString(),
                "docs/note.md");

        String html = workflow.buildPreviewHtml("![[images/diagram.png]]", false, List.of(), context);

        assertTrue(html.contains(image.toUri().toString()));
        assertTrue(html.contains("<img"));
    }

    @Test
    void buildPreviewHtmlResolvesRelativeMarkdownImageFromNoteFolder(@TempDir Path tempDir) throws Exception {
        Path folder = tempDir.resolve("folder");
        Path images = folder.resolve("assets");
        Files.createDirectories(images);
        Path image = images.resolve("chart.jpg");
        Files.writeString(image, "fake-image");

        PreviewWorkflow workflow = new PreviewWorkflow();
        PreviewWorkflow.PreviewContext context = new PreviewWorkflow.PreviewContext(
                "filesystem",
                tempDir.toString(),
                "folder/current.md");

        String html = workflow.buildPreviewHtml("![alt](assets/chart.jpg)", false, List.of(), context);

        assertTrue(html.contains(image.toUri().toString()));
    }

    @Test
    void buildPreviewHtmlKeepsEmojiCharactersUntouched() {
        PreviewWorkflow workflow = new PreviewWorkflow();
        String html = workflow.buildPreviewHtml("Protocolos 🔌", false, List.of());

        assertTrue(html.contains("🔌"));
    }

    @Test
    void buildPreviewHtmlConvertsWikiLinksToInternalLinks() {
        PreviewWorkflow workflow = new PreviewWorkflow();
        PreviewWorkflow.PreviewContext context = new PreviewWorkflow.PreviewContext(
                "filesystem",
                "/tmp",
                "note.md",
                (rawTarget, sourceNoteId) -> "forevernote://note/" + rawTarget.replace(" ", "_"));
        String html = workflow.buildPreviewHtml("Relacion [[Mi Nota|Abrir nota]]", false, List.of(), context);

        assertTrue(html.contains("class=\"fn-wikilink\""));
        assertTrue(html.contains("forevernote://note/"));
        assertTrue(html.contains("Abrir nota"));
    }
}
