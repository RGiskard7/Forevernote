package com.example.forevernote.tests;

import com.example.forevernote.service.links.ObsidianLinkParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObsidianLinkParserTest {

    @Test
    void parsesWikiLinksAliasesAndEmbeds() {
        ObsidianLinkParser parser = new ObsidianLinkParser();
        String markdown = "Hola [[Nota Uno]] y [[carpeta/Nota Dos|Alias Dos]] y ![[img.png]]";

        ObsidianLinkParser.ParsedLinks parsed = parser.parse(markdown);

        assertEquals(3, parsed.links().size());
        assertFalse(parsed.links().get(0).embed());
        assertEquals("Nota Uno", parsed.links().get(0).target());
        assertEquals("", parsed.links().get(0).alias());

        assertFalse(parsed.links().get(1).embed());
        assertEquals("carpeta/Nota Dos", parsed.links().get(1).target());
        assertEquals("Alias Dos", parsed.links().get(1).alias());

        assertTrue(parsed.links().get(2).embed());
        assertEquals("img.png", parsed.links().get(2).target());
    }
}
