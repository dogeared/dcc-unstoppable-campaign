package com.example.unstoppable.ui;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSummary;
import com.example.unstoppable.service.CurrentUser;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "campaigns", layout = MainLayout.class)
@PageTitle("Campaigns")
@PermitAll
public class CampaignListView extends VerticalLayout {

    private final CampaignService campaignService;
    private final CurrentUser currentUser;

    public CampaignListView(CampaignService campaignService, CurrentUser currentUser) {
        this.campaignService = campaignService;
        this.currentUser = currentUser;
        setPadding(false);
        setSpacing(false);
        addClassName("page");
        render();
    }

    private void render() {
        List<CampaignSummary> campaigns = campaignService.listFor(currentUser.id());

        Div head = new Div();
        head.addClassName("page-head");
        Div copy = new Div();
        copy.addClassName("page-head-copy");
        Span eyebrow = new Span("Your table");
        eyebrow.addClassName("eyebrow");
        copy.add(new H1("Campaigns"), new Paragraph(
                "Every sheet is shared with the players you invite. Pick up where "
                        + "the party left off."));
        head.add(copy);
        Anchor create = new Anchor("campaigns/new", "+ New campaign");
        create.addClassName("btn-primary");
        head.add(create);

        add(head);

        if (campaigns.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("empty-card");
            empty.add(new H2("No campaigns yet."));
            empty.add(new Paragraph("Start a sheet, name the party, and choose a difficulty."));
            Anchor start = new Anchor("campaigns/new", "Create the first campaign");
            start.addClassName("btn-primary");
            empty.add(start);
            add(empty);
            return;
        }

        Div grid = new Div();
        grid.addClassName("campaign-grid");
        for (CampaignSummary campaign : campaigns) {
            grid.add(card(campaign));
        }
        add(grid);
    }

    private Div card(CampaignSummary campaign) {
        Div card = new Div();
        card.addClassName("campaign-card");

        Div top = new Div();
        top.addClassName("campaign-card-top");
        Div names = new Div();
        Span party = new Span(campaign.partyName());
        party.addClassName("campaign-party");
        Span meta = new Span(campaign.difficulty().label()
                + (campaign.status() == CampaignStatus.COMPLETED ? " · complete" : ""));
        meta.addClassName("campaign-meta");
        names.add(party, meta);

        Span role = new Span(campaign.role() == AccessRole.OWNER ? "Owner"
                : campaign.role() == AccessRole.EDITOR ? "Editor" : "Viewer");
        role.addClassName("chip");
        role.addClassName("chip-quiet");
        top.add(names, role);

        Div stats = new Div();
        stats.addClassName("campaign-stats");
        stats.add(stat("Floors", campaign.floorsCompleted() + " / " + CampaignService.FLOOR_COUNT));
        stats.add(stat("Available XP", Integer.toString(campaign.availableXp())));
        stats.add(stat("Final score", Integer.toString(campaign.finalScore())));

        Anchor open = new Anchor("campaigns/" + campaign.id(), "Open sheet");
        open.addClassName("btn-ghost");

        card.add(top, stats, open);
        return card;
    }

    private static Div stat(String label, String value) {
        Div stat = new Div();
        stat.addClassName("mini-stat");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("mini-stat-label");
        Span valueSpan = new Span(value);
        valueSpan.addClassName("mini-stat-value");
        stat.add(labelSpan, valueSpan);
        return stat;
    }
}
