package com.example.unstoppable.ui;

import com.vaadin.flow.component.HasStyle;

public final class Css {

    private Css() {
    }

    public static void add(HasStyle component, String... classNames) {
        for (String className : classNames) {
            component.addClassName(className);
        }
    }
}
