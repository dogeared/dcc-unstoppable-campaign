package com.example.unstoppable.ui;

import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.ui.component.AppFooter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route("join/:token")
@PageTitle("Join campaign")
@PermitAll
public class JoinView extends VerticalLayout implements BeforeEnterObserver {

    private final InviteService inviteService;
    private final CurrentUser currentUser;

    public JoinView(InviteService inviteService, CurrentUser currentUser) {
        this.inviteService = inviteService;
        this.currentUser = currentUser;
        setPadding(false);
        setSpacing(false);
        addClassName("join-view");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String token = event.getRouteParameters().get("token").orElse("");
        try {
            var campaignId = inviteService.accept(token, currentUser.id());
            UI.getCurrent().navigate("campaigns/" + campaignId);
        } catch (RuntimeException exception) {
            Div card = new Div();
            card.addClassName("empty-card");
            card.add(new H1("That link did not work."));
            card.add(new Paragraph(exception.getMessage()));
            card.add(new Paragraph("Ask the campaign owner for a fresh invite link."));
            add(card, new AppFooter());
        }
    }
}
