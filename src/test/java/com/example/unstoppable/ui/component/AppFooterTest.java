package com.example.unstoppable.ui.component;

import com.vaadin.flow.dom.Element;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppFooterTest {

    @Test
    void footerCreditsAuthorAndShowsVersion() {
        AppFooter footer = new AppFooter();

        assertTrue(footer.getClassNames().contains("app-footer"));
        assertTrue(footer.getElement().getTextRecursively().contains("Made with"));
        assertTrue(footer.getElement().getTextRecursively().contains("dogeared"));
        assertTrue(footer.getElement().getTextRecursively()
                .contains("Dungeon Crawler Carl is © and ™ Matt Dinniman, LLC."));
        assertTrue(footer.getElement().getTextRecursively()
                .contains("Dungeon Crawler Carl: Unstoppable is "
                        + "© 2026 Renegade Game Studios."));

        Element author = descendants(footer.getElement())
                .filter(element -> !element.isTextNode())
                .filter(element -> "a".equals(element.getTag()))
                .findFirst()
                .orElseThrow();
        assertEquals(AppFooter.GITHUB_URL, author.getAttribute("href"));
        assertEquals("dogeared", author.getText());
        assertEquals("_blank", author.getAttribute("target"));
        assertEquals("noopener noreferrer", author.getAttribute("rel"));

        Element version = descendants(footer.getElement())
                .filter(element -> !element.isTextNode())
                .filter(element -> element.getClassList().contains("app-footer-version"))
                .findFirst()
                .orElseThrow();
        assertEquals("v" + AppFooter.resolveVersion(), version.getText());
    }

    @Test
    void versionIsReadFromBuildInfo() {
        String version = AppFooter.resolveVersion();

        assertFalse(version.isBlank());
        assertFalse(version.startsWith("v"));
    }

    private static Stream<Element> descendants(Element element) {
        return element.getChildren()
                .flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }
}
