package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

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
}
