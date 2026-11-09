package com.example.unstoppable.ui;

import com.example.unstoppable.service.CampaignNotFoundException;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.ui.component.CampaignSheetContent;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

@Route(value = "campaigns/:campaignId", layout = MainLayout.class)
@PageTitle("Campaign sheet")
@PermitAll
public class CampaignSheetView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(CampaignSheetView.class);

    private final CampaignService campaignService;
    private final InviteService inviteService;
    private final CurrentUser currentUser;

    private UUID campaignId;

    public CampaignSheetView(CampaignService campaignService, InviteService inviteService,
            CurrentUser currentUser) {
        this.campaignService = campaignService;
        this.inviteService = inviteService;
        this.currentUser = currentUser;
        setPadding(false);
        setSpacing(false);
        addClassName("page");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String raw = event.getRouteParameters().get("campaignId").orElse("");
        try {
            campaignId = UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            renderProblem("This sheet is not available.", "The link is malformed.");
            return;
        }
        try {
            CampaignSheet sheet = campaignService.load(campaignId, currentUser.id());
            add(new CampaignSheetContent(sheet, inviteService, currentUser, false));
        } catch (CampaignNotFoundException | AccessDeniedException exception) {
            removeAll();
            renderProblem("This sheet is not available.",
                    "It may have been removed, or your account does not have access.");
        } catch (RuntimeException exception) {
            log.error("Could not render campaign sheet {}", campaignId, exception);
            removeAll();
            renderProblem("This sheet could not be displayed.",
                    "Something went wrong while loading the sheet. Please try again.");
        }
    }

    private void renderProblem(String title, String message) {
        Div card = new Div();
        card.addClassName("empty-card");
        card.add(new H1(title));
        card.add(new Paragraph(message));
        Anchor back = new Anchor("campaigns", "Back to campaigns");
        back.addClassName("btn-primary");
        card.add(back);
        add(card);
    }
}
