package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.forevernote.util.MarkdownProcessor;

class MarkdownProcessorSecurityTest {

    @Test
    void markdownToHtmlEscapesRawHtmlTags() {
        String html = MarkdownProcessor.markdownToHtml("<script>alert('xss')</script>");
        assertFalse(html.contains("<script>alert('xss')</script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void markdownToHtmlSanitizesDangerousUrls() {
        String html = MarkdownProcessor.markdownToHtml("[x](javascript:alert('xss'))");
        assertFalse(html.toLowerCase().contains("javascript:alert"));
    }
}
