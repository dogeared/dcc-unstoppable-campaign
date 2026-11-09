package com.example.unstoppable;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.lumo.Lumo;

@Push
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles/unstoppable.css")
@JsModule("./scripts/unstoppable.js")
@ColorScheme(ColorScheme.Value.DARK)
public class AppShell implements AppShellConfigurator {
}
