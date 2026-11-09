package com.example.unstoppable.ui;

import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.ui.component.AppFooter;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("")
@PageTitle("Unstoppable")
@AnonymousAllowed
public class LandingView extends VerticalLayout {

    private static final String LOGIN_URL = "/oauth2/authorization/keycloak";
    private static final String REGISTER_URL = "/oauth2/authorization/keycloak?mode=register";

    public LandingView(CurrentUser currentUser) {
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        Div inner = new Div();
        inner.addClassName("landing-inner");

        Div brand = new Div();
        brand.addClassName("landing-brand");
        Span brandStrong = new Span("un");
        brandStrong.addClassName("landing-brand-strong");
        Span brandLight = new Span("stoppable");
        brandLight.addClassName("landing-brand-light");
        brand.add(brandStrong, brandLight);

        Span eyebrow = new Span("Dungeon Crawler Carl");
        eyebrow.addClassName("eyebrow");

        H1 title = new H1("Run the whole crawl from one sheet");
        Paragraph copy = new Paragraph(
                "Track floors, XP, defeats, deck changes, achievements, and permanent "
                        + "benefits across every session. Share the sheet with your party "
                        + "without passing paper around.");
        copy.addClassName("landing-copy");

        Div actions = new Div();
        actions.addClassName("landing-actions");
        if (currentUser.isAuthenticated()) {
            Anchor enter = new Anchor("/campaigns", "Open your campaigns");
            enter.addClassName("btn-primary");
            Anchor fresh = new Anchor("campaigns/new", "New campaign");
            fresh.addClassName("btn-ghost");
            actions.add(enter, fresh);
        } else {
            Anchor login = new Anchor(LOGIN_URL, "Log in");
            login.addClassName("btn-primary");
            Anchor register = new Anchor(REGISTER_URL, "Register");
            register.addClassName("btn-ghost");
            // The OAuth endpoints must be a full browser navigation; without
            // this Vaadin's client router treats them as in-app routes.
            login.getElement().setAttribute("router-ignore", true);
            register.getElement().setAttribute("router-ignore", true);
            actions.add(login, register);
        }

        Div rule = new Div();
        rule.addClassName("landing-rule");

        inner.add(brand, eyebrow, title, copy, actions, rule);
        add(inner, new AppFooter());
    }
}
