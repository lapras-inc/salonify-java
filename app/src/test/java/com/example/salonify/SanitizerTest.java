package com.example.salonify;

import com.example.salonify.support.Sanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanitizerTest {

    private final Sanitizer sanitizer = new Sanitizer();

    @Test
    void removesScriptBlocks() {
        String out = sanitizer.sanitizeHtml("<p>hi</p><script>alert(1)</script>");
        assertFalse(out.contains("script"));
        assertTrue(out.contains("<p>hi</p>"));
    }

    @Test
    void removesEventHandlers() {
        String out = sanitizer.sanitizeHtml("<img src=\"x\" onerror=\"alert(1)\">");
        assertFalse(out.contains("onerror"));
        assertTrue(out.contains("<img"));
    }

    @Test
    void removesJavascriptUrls() {
        String out = sanitizer.sanitizeHtml("<a href=\"javascript:alert(1)\">x</a>");
        assertFalse(out.toLowerCase().contains("javascript:"));
    }

    @Test
    void keepsAllowedTagsAndStripsDisallowed() {
        String out = sanitizer.sanitizeHtml("<p><strong>bold</strong></p><table><tr><td>x</td></tr></table>");
        assertTrue(out.contains("<strong>bold</strong>"));
        assertFalse(out.contains("<table"));
        assertFalse(out.contains("<td"));
    }

    @Test
    void stripsDisallowedAttributesOnAllowedTags() {
        String out = sanitizer.sanitizeHtml("<a href=\"/ok\" class=\"evil\" title=\"t\">x</a>");
        assertTrue(out.contains("href=\"/ok\""));
        assertTrue(out.contains("title=\"t\""));
        assertFalse(out.contains("class"));
    }

    @Test
    void renderMarkdownProducesSanitizedHtml() {
        String out = sanitizer.renderMarkdown("# Title\n\n**bold** and <script>x</script>");
        assertTrue(out.contains("<h1>"));
        assertTrue(out.contains("<strong>bold</strong>"));
        assertFalse(out.contains("script"));
    }

    @Test
    void extractsYouTubeId() {
        assertEquals("dQw4w9WgXcQ", sanitizer.extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertEquals("dQw4w9WgXcQ", sanitizer.extractYouTubeId("https://youtu.be/dQw4w9WgXcQ"));
        assertEquals("dQw4w9WgXcQ", sanitizer.extractYouTubeId("https://www.youtube.com/embed/dQw4w9WgXcQ"));
        assertNull(sanitizer.extractYouTubeId("https://example.com"));
        assertNull(sanitizer.extractYouTubeId(null));
    }
}
