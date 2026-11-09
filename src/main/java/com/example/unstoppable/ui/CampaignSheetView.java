package com.example.unstoppable.ui;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.service.CampaignNotFoundException;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.ui.component.SharePanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
    private CampaignSheet sheet;

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
            sheet = campaignService.load(campaignId, currentUser.id());
            render();
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

    private void render() {
        add(header());
        add(scoreboard());
        add(section("Side A — campaign sheet", paper()));
        add(section("Achievements", achievements()));
        add(section("Purchases", purchases()));
        add(section("Attempt log", attempts()));
    }

    private Div header() {
        Div head = new Div();
        head.addClassName("page-head");

        Div copy = new Div();
        copy.addClassName("page-head-copy");
        Span eyebrow = new Span("Campaign sheet");
        eyebrow.addClassName("eyebrow");
        copy.add(new H1(sheet.partyName()));
        Div badges = new Div();
        badges.addClassName("badge-row");
        badges.add(chip(sheet.difficulty().label(), "chip-accent"));
        badges.add(chip(sheet.status() == CampaignStatus.COMPLETED ? "Complete" : "In progress",
                "chip-quiet"));
        badges.add(chip(sheet.role().name().toLowerCase(), "chip-quiet"));
        copy.add(badges);
        head.add(copy);

        Div actions = new Div();
        actions.addClassName("head-actions");
        Anchor back = new Anchor("campaigns", "All campaigns");
        back.addClassName("btn-ghost");
        actions.add(back);
        if (sheet.role() == AccessRole.OWNER) {
            Button share = new Button("Share", event -> openShareDialog());
            share.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actions.add(share);
        }
        if (sheet.canEdit()) {
            Anchor edit = new Anchor("campaigns/" + sheet.id() + "/edit", "Edit sheet");
            edit.addClassName(sheet.role() == AccessRole.OWNER ? "btn-ghost" : "btn-primary");
            actions.add(edit);
        }
        head.add(actions);
        return head;
    }

    private void openShareDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Share this campaign");
        dialog.setWidth("min(760px, 96vw)");
        dialog.add(new SharePanel(inviteService, currentUser, campaignId));
        Button close = new Button("Close", event -> dialog.close());
        dialog.getFooter().add(close);
        dialog.open();
    }

    private Div scoreboard() {
        Div grid = new Div();
        grid.addClassName("stat-grid");
        grid.add(statCard("Available XP", Integer.toString(sheet.totals().availableXp()),
                "Spendable campaign XP", true));
        grid.add(statCard("Final score", Integer.toString(sheet.totals().finalScore()),
                "Includes difficulty bonus", false));
        grid.add(statCard("Floor XP", Integer.toString(sheet.totals().floorScoreTotal()),
                "Earned across all floors", false));
        grid.add(statCard("Spent XP", Integer.toString(sheet.totals().spentXp()),
                "Permanent benefits", false));
        grid.add(statCard("Defeats", Integer.toString(sheet.totals().totalDefeats()),
                "−3 each on final score", false));
        return grid;
    }

    private Div statCard(String label, String value, String hint, boolean accent) {
        Div card = new Div();
        card.addClassName("stat-card");
        if (accent) {
            card.addClassName("stat-card-accent");
        }
        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");
        Span valueSpan = new Span(value);
        valueSpan.addClassName("stat-value");
        Span hintSpan = new Span(hint);
        hintSpan.addClassName("stat-hint");
        card.add(labelSpan, valueSpan, hintSpan);
        return card;
    }

    private Div section(String title, Div body) {
        Div section = new Div();
        section.addClassName("sheet-section");
        H2 heading = new H2(title);
        heading.addClassName("section-title");
        section.add(heading, body);
        return section;
    }

    private Div paper() {
        Div paper = new Div();
        paper.addClassName("paper");

        Div title = new Div();
        title.addClassName("paper-title");
        title.add(new Span("Campaign Sheet (Side A)"));
        paper.add(title);

        Div scroll = new Div();
        scroll.addClassName("paper-scroll");
        scroll.add(floorGrid());
        paper.add(scroll);

        paper.add(partyStrip());

        Div players = new Div();
        players.addClassName("player-grid");
        for (CampaignSheet.PlayerView player : sheet.players()) {
            players.add(playerPanel(player));
        }
        paper.add(players);
        return paper;
    }

    private Div floorGrid() {
        Div grid = new Div();
        grid.addClassName("floor-grid");

        Div head = new Div();
        head.addClassName("floor-row");
        head.addClassName("floor-head");
        head.add(cell("Floor", "floor-label"));
        String[] ordinals = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th"};
        for (String ordinal : ordinals) {
            head.add(cell(ordinal, "floor-cell"));
        }
        grid.add(head);

        Div scores = new Div();
        scores.addClassName("floor-row");
        scores.add(cell("Floor Score", "floor-label"));
        for (CampaignSheet.FloorView floor : sheet.floors()) {
            scores.add(cell(Integer.toString(floor.floorScore()), "floor-cell"));
        }
        grid.add(scores);

        Div defeats = new Div();
        defeats.addClassName("floor-row");
        defeats.add(cell("Defeats (−3 each)", "floor-label"));
        for (CampaignSheet.FloorView floor : sheet.floors()) {
            defeats.add(cell(Integer.toString(floor.defeats()), "floor-cell"));
        }
        grid.add(defeats);

        Div status = new Div();
        status.addClassName("floor-row");
        status.add(cell("Completed", "floor-label"));
        for (CampaignSheet.FloorView floor : sheet.floors()) {
            Div cell = cell("", "floor-cell");
            if (floor.completed()) {
                Span done = new Span("✓");
                done.addClassName("tick");
                cell.add(done);
            } else {
                cell.add(new Span("—"));
            }
            status.add(cell);
        }
        grid.add(status);
        return grid;
    }

    private Div partyStrip() {
        Div strip = new Div();
        strip.addClassName("party-strip");
        strip.add(field("Party Name", sheet.partyName(), true));
        strip.add(field("Difficulty", sheet.difficulty().label(), false));
        strip.add(field("XP", Integer.toString(sheet.totals().availableXp()), true));
        return strip;
    }

    private Div playerPanel(CampaignSheet.PlayerView player) {
        Div panel = new Div();
        panel.addClassName("player-panel");

        Div head = new Div();
        head.addClassName("player-head");
        Span title = new Span("Player " + player.seat()
                + (player.crawler() == null || player.crawler().isBlank()
                        ? "" : " — " + player.crawler()));
        head.add(title);
        panel.add(head);

        Div fields = new Div();
        fields.addClassName("field-grid");
        fields.add(field("Crawler", player.crawler(), false));
        fields.add(field("Race", player.race(), false));
        fields.add(field("Class", player.crawlerClass(), false));
        fields.add(field("Starting Health", Integer.toString(player.startingHealth()), false));
        fields.add(field("Starting Mana", Integer.toString(player.startingMana()), false));
        fields.add(field("Starting Gold", Integer.toString(player.startingGold()), false));
        fields.add(field("Signature Card (level 1)", player.signatureLevel1(), false));
        fields.add(field("Signature Card (level 2)", player.signatureLevel2(), false));
        panel.add(fields);

        Div deckHead = new Div();
        deckHead.addClassName("deck-title");
        deckHead.add(new Span("Deck list"), new Span("· record changes after each session"));
        panel.add(deckHead);
        panel.add(deck(player));
        return panel;
    }

    private Div deck(CampaignSheet.PlayerView player) {
        Div table = new Div();
        table.addClassName("deck-table");

        Div header = new Div();
        header.addClassName("deck-row");
        header.addClassName("deck-head");
        header.add(cell("#", "deck-pos"));
        header.add(cell("Base", "deck-base"));
        header.add(cell("Card / upgrade", "deck-name"));
        header.add(cell("State", "deck-flags"));
        table.add(header);

        for (CampaignSheet.DeckSlotView slot : player.deck()) {
            Div row = new Div();
            row.addClassName("deck-row");
            if (slot.eliminated()) {
                row.addClassName("deck-eliminated");
            }
            row.add(cell(Integer.toString(slot.position()), "deck-pos"));
            row.add(cell(slot.baseCard(), "deck-base"));
            Div name = new Div();
            name.addClassName("deck-name");
            if (slot.cardName() != null && !slot.cardName().isBlank()
                    && !slot.cardName().equals(slot.baseCard())) {
                name.add(new Span(slot.cardName()));
            }
            if (slot.upgradeName() != null && !slot.upgradeName().isBlank()) {
                Span upgrade = new Span("↑ " + slot.upgradeName());
                upgrade.addClassName("deck-upgrade");
                name.add(upgrade);
            }
            if (name.getChildren().findAny().isEmpty()) {
                name.add(new Span("—"));
            }
            row.add(name);

            Div flags = new Div();
            flags.addClassName("deck-flags");
            if (slot.eliminated()) {
                flags.add(chip("eliminated", "chip-danger"));
            }
            if (slot.mastered()) {
                flags.add(chip("mastered", "chip-quiet"));
            }
            if (slot.extraStartingHand()) {
                flags.add(chip("+ starting hand", "chip-accent"));
            }
            if (flags.getChildren().findAny().isEmpty()) {
                flags.add(new Span("—"));
            }
            row.add(flags);
            table.add(row);
        }
        return table;
    }

    private Div achievements() {
        Div wrap = new Div();
        wrap.addClassName("list-stack");
        boolean any = false;
        for (CampaignSheet.FloorView floor : sheet.floors()) {
            if (floor.achievements().isEmpty()) {
                continue;
            }
            any = true;
            Div card = new Div();
            card.addClassName("list-card");
            Div head = new Div();
            head.addClassName("list-head");
            head.add(new H3("Floor " + floor.floorNumber()));
            head.add(new Span(achievementXp(floor) + " XP recorded"));
            card.add(head);
            for (CampaignSheet.AchievementView achievement : floor.achievements()) {
                Div row = new Div();
                row.addClassName("list-row");
                row.add(new Span(achievement.name()));
                row.add(new Span(Ui.signed(achievement.xp()) + " XP"));
                row.add(new Span(Ui.date(achievement.completedAt())));
                card.add(row);
            }
            wrap.add(card);
        }
        if (!any) {
            wrap.add(new Paragraph("No achievements recorded yet."));
        }
        return wrap;
    }

    private static int achievementXp(CampaignSheet.FloorView floor) {
        return floor.achievements().stream()
                .mapToInt(CampaignSheet.AchievementView::xp).sum();
    }

    private Div purchases() {
        Div wrap = new Div();
        wrap.addClassName("list-stack");
        if (sheet.purchases().isEmpty()) {
            wrap.add(new Paragraph("No permanent benefits purchased yet."));
            return wrap;
        }
        Div table = new Div();
        table.addClassName("data-table");
        Div header = new Div();
        header.addClassName("data-row");
        header.addClassName("data-head");
        header.add(cell("Player", "data-cell"));
        header.add(cell("Benefit", "data-cell data-wide"));
        header.add(cell("Cost", "data-cell"));
        header.add(cell("Target", "data-cell"));
        header.add(cell("When", "data-cell"));
        table.add(header);
        for (CampaignSheet.PurchaseView purchase : sheet.purchases()) {
            Div row = new Div();
            row.addClassName("data-row");
            row.add(cell("P" + purchase.seat()
                    + (purchase.crawler() == null ? "" : " · " + purchase.crawler()),
                    "data-cell"));
            String benefit = purchase.benefit().label();
            if (purchase.xValue() != null) {
                benefit += " (X = " + purchase.xValue() + ")";
            }
            row.add(cell(benefit, "data-cell data-wide"));
            row.add(cell(Integer.toString(purchase.cost()), "data-cell"));
            String target = Ui.blank(purchase.targetCard());
            if (purchase.upgradeName() != null && !purchase.upgradeName().isBlank()) {
                target += " ↑ " + purchase.upgradeName();
            }
            row.add(cell(target, "data-cell"));
            row.add(cell(Ui.date(purchase.purchasedAt()), "data-cell"));
            table.add(row);
        }
        wrap.add(table);
        return wrap;
    }

    private Div attempts() {
        Div wrap = new Div();
        wrap.addClassName("list-stack");
        boolean any = false;
        for (CampaignSheet.FloorView floor : sheet.floors()) {
            if (floor.attempts().isEmpty()) {
                continue;
            }
            any = true;
            Div card = new Div();
            card.addClassName("list-card");
            Div head = new Div();
            head.addClassName("list-head");
            head.add(new H3("Floor " + floor.floorNumber()));
            head.add(new Span(floor.attempts().size() + " attempt"
                    + (floor.attempts().size() == 1 ? "" : "s")
                    + " · " + floor.defeats() + " defeat"
                    + (floor.defeats() == 1 ? "" : "s")));
            card.add(head);
            for (CampaignSheet.AttemptView attempt : floor.attempts()) {
                Div row = new Div();
                row.addClassName("list-row");
                Span result = chip(attempt.successful() ? "cleared" : "defeat",
                        attempt.successful() ? "chip-accent" : "chip-danger");
                row.add(new Span(Ui.date(attempt.playedAt())));
                row.add(result);
                row.add(new Span(Ui.blank(attempt.notes())));
                card.add(row);
            }
            wrap.add(card);
        }
        if (!any) {
            wrap.add(new Paragraph("No sessions logged yet. Add attempts from the editor."));
        }
        return wrap;
    }

    private static Div cell(String text, String className) {
        Div cell = new Div();
        cell.addClassNames(className.split("\\s+"));
        if (!text.isEmpty()) {
            cell.add(new Span(text));
        }
        return cell;
    }

    private static Span chip(String text, String className) {
        Span chip = new Span(text);
        chip.addClassName("chip");
        chip.addClassName(className);
        return chip;
    }

    private static Div field(String label, String value, boolean accent) {
        Div field = new Div();
        field.addClassName("sheet-field");
        if (accent) {
            field.addClassName("sheet-field-accent");
        }
        Span labelSpan = new Span(label);
        labelSpan.addClassName("sheet-field-label");
        Span valueSpan = new Span(Ui.blank(value));
        valueSpan.addClassName("sheet-field-value");
        field.add(labelSpan, valueSpan);
        return field;
    }
}
