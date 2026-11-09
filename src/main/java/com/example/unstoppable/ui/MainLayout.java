package com.example.unstoppable.ui;

import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.ui.component.AppFooter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout {

    private final Div contentArea = new Div();
    private final AppFooter footer = new AppFooter();

    public MainLayout(CurrentUser currentUser, AuthenticationContext authenticationContext) {
        addClassName("app-shell");

        NativeButton drawerToggle = new NativeButton();
        drawerToggle.addClassName("drawer-toggle");
        drawerToggle.setAriaLabel("Toggle navigation");
        drawerToggle.add(VaadinIcon.MENU.create());
        drawerToggle.addClickListener(event -> setDrawerOpened(!isDrawerOpened()));

        Header header = new Header();
        header.addClassName("app-header");
        header.add(drawerToggle, brand(), desktopNav());
        Div spacer = new Div();
        spacer.addClassName("header-spacer");
        header.add(spacer, account(currentUser, authenticationContext));
        addToNavbar(header);

        addToDrawer(drawer(authenticationContext));
        contentArea.addClassName("app-content");
        setContent(contentArea);
        setDrawerOpened(false);
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        contentArea.removeAll();
        if (content instanceof Component component) {
            Div view = new Div(component);
            view.addClassName("app-content-view");
            contentArea.add(view);
        }
        contentArea.add(footer);
    }

    private static RouterLink brand() {
        RouterLink brand = new RouterLink();
        brand.setRoute(CampaignListView.class);
        brand.addClassName("brand");
        brand.getElement().setAttribute("aria-label", "Unstoppable campaign sheets");
        Span strong = new Span("un");
        strong.addClassName("brand-strong");
        Span light = new Span("stoppable");
        light.addClassName("brand-light");
        brand.add(strong, light);
        return brand;
    }

    private static Nav desktopNav() {
        Nav nav = new Nav();
        nav.addClassName("app-nav");
        nav.add(link("Campaigns", VaadinIcon.MAP_MARKER, CampaignListView.class, "nav-link"));
        nav.add(link("New campaign", VaadinIcon.PLUS, NewCampaignView.class, "nav-link"));
        return nav;
    }

    private static Div account(CurrentUser currentUser,
            AuthenticationContext authenticationContext) {
        Div account = new Div();
        account.addClassName("nav-account");

        NativeButton signOut = new NativeButton("Sign out");
        Css.add(signOut, "btn", "btn-ghost", "nav-signout");
        signOut.addClickListener(event -> authenticationContext.logout());

        account.add(userChip(currentUser), signOut);
        return account;
    }

    private static Div userChip(CurrentUser currentUser) {
        Div chip = new Div();
        chip.addClassName("user-chip");
        Span avatar = new Span(currentUser.initials());
        avatar.addClassName("avatar");
        Div text = new Div();
        text.addClassName("user-text");
        Span name = new Span(currentUser.displayName());
        name.addClassName("user-name");
        Span username = new Span("@" + currentUser.username());
        username.addClassName("user-handle");
        text.add(name, username);
        chip.add(avatar, text);
        return chip;
    }

    private static Div drawer(AuthenticationContext authenticationContext) {
        Div drawer = new Div();
        drawer.addClassName("drawer");

        Div head = new Div();
        head.addClassName("drawer-head");
        Span title = new Span("Campaigns");
        title.addClassName("drawer-title");
        Span hint = new Span("Track every floor, defeat, and purchase.");
        hint.addClassName("drawer-hint");
        head.add(title, hint);

        Nav nav = new Nav();
        nav.addClassName("drawer-nav");
        nav.add(link("Campaigns", VaadinIcon.MAP_MARKER, CampaignListView.class, "drawer-link"));
        nav.add(link("New campaign", VaadinIcon.PLUS, NewCampaignView.class, "drawer-link"));

        NativeButton signOut = new NativeButton("Sign out");
        Css.add(signOut, "btn", "btn-ghost", "drawer-signout");
        signOut.addClickListener(event -> authenticationContext.logout());

        drawer.add(head, nav, signOut);
        return drawer;
    }

    private static RouterLink link(String text, VaadinIcon icon,
            Class<? extends Component> route, String className) {
        RouterLink link = new RouterLink();
        link.setRoute(route);
        link.addClassName(className);
        Icon graphic = icon.create();
        graphic.addClassName("nav-icon");
        link.add(graphic, new Span(text));
        return link;
    }
}
