package com.example.unstoppable.ui.component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Site-wide footer: a short credit line linking to the author's GitHub profile,
 * the release version recorded in the build metadata, and the project's
 * copyright disclaimers.
 */
public class AppFooter extends Footer {

    static final String GITHUB_URL = "https://github.com/dogeared";
    static final String UNKNOWN_VERSION = "dev";

    public AppFooter() {
        addClassName("app-footer");

        Span heart = new Span("\u2764\ufe0f");
        heart.addClassName("app-footer-heart");
        heart.getElement().setAttribute("aria-hidden", "true");

        Anchor author = new Anchor(GITHUB_URL, "dogeared");
        author.addClassName("app-footer-author");
        author.getElement().setAttribute("target", "_blank");
        author.getElement().setAttribute("rel", "noopener noreferrer");

        // Keep the credit words as separate items and let the flex gap supply the
        // spacing, so leading/trailing text-node whitespace cannot collapse.
        Span made = new Span(new Span("Made with"), heart, new Span("by"), author);
        made.addClassName("app-footer-made");
        made.getElement().setAttribute("aria-label", "Made with love by dogeared");

        Span version = new Span("v" + resolveVersion());
        version.addClassName("app-footer-version");

        Span disclaimers = new Span();
        disclaimers.addClassName("app-footer-disclaimers");
        Span dinniman = new Span("An unofficial companion. "
                + "Dungeon Crawler Carl is © and ™ Matt Dinniman, LLC.");
        dinniman.addClassName("app-footer-disclaimer");
        Span renegade = new Span("Dungeon Crawler Carl: Unstoppable is "
                + "© 2026 Renegade Game Studios.");
        renegade.addClassName("app-footer-disclaimer");
        disclaimers.add(dinniman, renegade);

        add(made, version, disclaimers);
    }

    /**
     * Reads the version that Spring Boot's {@code build-info} goal writes into
     * the jar. Falls back to {@code dev} when the app runs without build
     * metadata (for example, a bare IDE launch).
     */
    static String resolveVersion() {
        try (InputStream input = AppFooter.class
                .getResourceAsStream("/META-INF/build-info.properties")) {
            if (input == null) {
                return UNKNOWN_VERSION;
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty("build.version", UNKNOWN_VERSION);
        } catch (IOException exception) {
            return UNKNOWN_VERSION;
        }
    }
}
