package com.example.unstoppable.ui.component;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Site-wide footer: a short credit line linking to the author's GitHub profile
 * and the release version recorded in the build metadata.
 */
public class AppFooter extends Footer {

    static final String GITHUB_URL = "https://github.com/dogeared";
    static final String UNKNOWN_VERSION = "dev";

    public AppFooter() {
        addClassName("app-footer");

        Span heart = new Span("\u2764\ufe0f");
        heart.addClassName("app-footer-heart");

        Anchor author = new Anchor(GITHUB_URL, "dogeared");
        author.addClassName("app-footer-author");
        author.getElement().setAttribute("target", "_blank");
        author.getElement().setAttribute("rel", "noopener noreferrer");

        Span made = new Span("Made with ");
        made.add(heart, new Span(" by "), author);

        Span version = new Span("v" + resolveVersion());
        version.addClassName("app-footer-version");

        add(made, version);
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
