package com.example.unstoppable.ui;

import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiHelpersTest {

    @Test
    void datesHandleNullAndFormatValues() {
        assertEquals("—", Ui.date((LocalDate) null));
        assertEquals("—", Ui.date((Instant) null));
        assertEquals("2 Jan 2026", Ui.date(LocalDate.of(2026, 1, 2)));
        assertEquals("2 Jan 2026", Ui.date(Instant.parse("2026-01-02T12:00:00Z")));
    }

    @Test
    void signedAddsPlusToPositiveValues() {
        assertEquals("+4", Ui.signed(4));
        assertEquals("+0", Ui.signed(0));
        assertEquals("-3", Ui.signed(-3));
    }

    @Test
    void blankFallsBackToDash() {
        assertEquals("—", Ui.blank(null));
        assertEquals("—", Ui.blank("  "));
        assertEquals("Carl", Ui.blank("Carl"));
    }

    @Test
    void cssAddsEveryClassName() {
        Div div = new Div();
        Css.add(div, "one", "two");
        assertTrue(div.hasClassName("one"));
        assertTrue(div.hasClassName("two"));
    }
}
