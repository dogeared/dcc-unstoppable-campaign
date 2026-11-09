package com.example.unstoppable;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.theme.lumo.Lumo;

@Push
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet("styles/unstoppable.css")
@JsModule("./scripts/unstoppable.js")
@ColorScheme(ColorScheme.Value.DARK)
// Load every route's web components up front. Direct visits to lazy routes
// such as /campaigns otherwise render server-side content before the matching
// frontend chunk has loaded, so components like vaadin-dialog stay
// unregistered and their executeJs calls fail with "reading 'overlay'".
@LoadDependenciesOnStartup
public class AppShell implements AppShellConfigurator {
}
