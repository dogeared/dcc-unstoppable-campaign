package com.example.unstoppable.ui;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class Ui {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private Ui() {
    }

    public static void success(String message) {
        Notification.show(message, 3000, Notification.Position.BOTTOM_START);
    }

    public static void error(String message) {
        Notification notification = Notification.show(message, 6000,
                Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String date(Instant instant) {
        return instant == null ? "—" : DATE.format(LocalDate.ofInstant(instant, ZoneId.systemDefault()));
    }

    public static String signed(int value) {
        return value >= 0 ? "+" + value : Integer.toString(value);
    }

    public static String blank(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
