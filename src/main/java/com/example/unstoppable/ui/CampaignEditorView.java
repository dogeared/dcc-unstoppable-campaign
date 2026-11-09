package com.example.unstoppable.ui;

import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.domain.Difficulty;
import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.service.CampaignNotFoundException;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.service.InviteView;
import com.example.unstoppable.ui.component.AchievementDialog;
import com.example.unstoppable.ui.component.AttemptDialog;
import com.example.unstoppable.ui.component.DeckSlotDialog;
import com.example.unstoppable.ui.component.PurchaseDialog;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Route(value = "campaigns/:campaignId/edit", layout = MainLayout.class)
@PageTitle("Edit campaign")
@PermitAll
public class CampaignEditorView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(CampaignEditorView.class);

    private final CampaignService campaignService;
    private final InviteService inviteService;
    private final CurrentUser currentUser;

    private UUID campaignId;
    private CampaignSheet sheet;
    private int selectedTabIndex;

    public CampaignEditorView(CampaignService campaignService,
            InviteService inviteService, CurrentUser currentUser) {
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
            if (!sheet.canEdit()) {
                renderProblem("You can view this campaign but not edit it.",
                        "Ask the owner for an editor invite.");
                return;
            }
            render();
        } catch (CampaignNotFoundException | AccessDeniedException exception) {
            removeAll();
            renderProblem("This sheet is not available.",
                    "It may have been removed, or your account does not have access.");
        } catch (RuntimeException exception) {
            log.error("Could not render campaign editor {}", campaignId, exception);
            removeAll();
            renderProblem("This sheet could not be displayed.",
                    "Something went wrong while loading the editor. Please try again.");
        }
    }

    private void renderProblem(String title, String message) {
        Div card = new Div();
        card.addClassName("empty-card");
        card.add(new H1(title));
        card.add(new Paragraph(message));
        Anchor back = campaignId == null
                ? new Anchor("campaigns", "Back to campaigns")
                : new Anchor("campaigns/" + campaignId, "Back to the sheet");
        back.addClassName("btn-primary");
        card.add(back);
        add(card);
    }

    private void reload() {
        sheet = campaignService.load(campaignId, currentUser.id());
        render();
    }

    private void render() {
        removeAll();
        add(header());

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.addClassName("editor-tabs");
        tabs.add("Party", partyTab());
        tabs.add("Players", playersTab());
        tabs.add("Deck lists", decksTab());
        tabs.add("Floors", floorsTab());
        tabs.add("Achievements", achievementsTab());
        tabs.add("Purchases", purchasesTab());
        if (sheet.role() == AccessRole.OWNER) {
            tabs.add("Share", shareTab());
        }
        if (selectedTabIndex < tabs.getTabCount()) {
            tabs.setSelectedIndex(selectedTabIndex);
        }
        tabs.addSelectedChangeListener(event ->
                selectedTabIndex = tabs.getSelectedIndex());
        add(tabs);
    }

    private Div header() {
        Div head = new Div();
        head.addClassName("page-head");

        Div copy = new Div();
        copy.addClassName("page-head-copy");
        Span eyebrow = new Span("Editing");
        eyebrow.addClassName("eyebrow");
        copy.add(new H1(sheet.partyName()));
        Div badges = new Div();
        badges.addClassName("badge-row");
        badges.add(chip(sheet.difficulty().label(), "chip-accent"));
        badges.add(chip(sheet.status() == CampaignStatus.COMPLETED ? "Complete" : "In progress",
                "chip-quiet"));
        copy.add(badges);
        head.add(copy);

        Div actions = new Div();
        actions.addClassName("head-actions");
        Anchor view = new Anchor("campaigns/" + campaignId, "View sheet");
        view.addClassName("btn-ghost");
        actions.add(view);
        head.add(actions);
        return head;
    }

    // ---- party --------------------------------------------------------------

    private Div partyTab() {
        Div body = new Div();
        body.addClassName("tab-body");

        Div card = new Div();
        card.addClassName("form-card");
        card.add(new H2("Party"));

        TextField partyName = new TextField("Party name");
        partyName.setValue(sheet.partyName());
        partyName.setMaxLength(120);
        partyName.setWidthFull();

        ComboBox<Difficulty> difficulty = new ComboBox<>("Difficulty");
        difficulty.setItems(Difficulty.values());
        difficulty.setItemLabelGenerator(Difficulty::label);
        difficulty.setValue(sheet.difficulty());

        FormLayout form = new FormLayout(partyName, difficulty);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("560px", 2));

        Button save = new Button("Save party", event -> {
            try {
                campaignService.updateParty(campaignId, currentUser.id(),
                        partyName.getValue(), difficulty.getValue());
                Ui.success("Party updated");
                reload();
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div actions = new Div();
        actions.addClassName("form-actions");
        actions.add(save);
        card.add(form, actions);
        body.add(card);

        Div summary = new Div();
        summary.addClassName("summary-strip");
        summary.add(stat("Available XP", sheet.totals().availableXp()));
        summary.add(stat("Final score", sheet.totals().finalScore()));
        summary.add(stat("Spent XP", sheet.totals().spentXp()));
        summary.add(stat("Defeats", sheet.totals().totalDefeats()));
        body.add(summary);

        if (sheet.role() == AccessRole.OWNER) {
            Div lifecycle = new Div();
            lifecycle.addClassName("form-card");
            lifecycle.add(new H2("Campaign status"));
            lifecycle.add(new Paragraph(sheet.status() == CampaignStatus.COMPLETED
                    ? "This campaign is marked complete. You can still edit the sheet."
                    : "Mark the campaign complete when the party has finished every floor."));
            Button toggle = new Button(sheet.status() == CampaignStatus.COMPLETED
                    ? "Reopen campaign" : "Mark complete");
            toggle.addClickListener(event -> {
                if (sheet.status() == CampaignStatus.COMPLETED) {
                    campaignService.reopenCampaign(campaignId, currentUser.id());
                } else {
                    campaignService.completeCampaign(campaignId, currentUser.id());
                }
                reload();
            });
            lifecycle.add(toggle);
            body.add(lifecycle);
        }
        return body;
    }

    private Div stat(String label, int value) {
        Div stat = new Div();
        stat.addClassName("mini-stat");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("mini-stat-label");
        Span valueSpan = new Span(Integer.toString(value));
        valueSpan.addClassName("mini-stat-value");
        stat.add(labelSpan, valueSpan);
        return stat;
    }

    // ---- players ------------------------------------------------------------

    private Div playersTab() {
        Div body = new Div();
        body.addClassName("tab-body");
        Div grid = new Div();
        grid.addClassName("player-form-grid");
        for (CampaignSheet.PlayerView player : sheet.players()) {
            grid.add(playerForm(player));
        }
        body.add(grid);
        return body;
    }

    private Div playerForm(CampaignSheet.PlayerView player) {
        Div card = new Div();
        card.addClassName("form-card");
        card.add(new H2("Player " + player.seat()));

        TextField crawler = text("Crawler", player.crawler());
        TextField race = text("Race", player.race());
        TextField crawlerClass = text("Class", player.crawlerClass());
        IntegerField health = number("Starting health", player.startingHealth());
        IntegerField mana = number("Starting mana", player.startingMana());
        IntegerField gold = number("Starting gold", player.startingGold());
        TextField signature1 = text("Signature card (level 1)", player.signatureLevel1());
        TextField signature2 = text("Signature card (level 2)", player.signatureLevel2());

        FormLayout form = new FormLayout(crawler, race, crawlerClass, health, mana, gold,
                signature1, signature2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("480px", 2));

        Button save = new Button("Save player", event -> {
            try {
                campaignService.updatePlayer(campaignId, currentUser.id(), player.seat(),
                        crawler.getValue(), race.getValue(), crawlerClass.getValue(),
                        health.getValue(), mana.getValue(), gold.getValue(),
                        signature1.getValue(), signature2.getValue());
                Ui.success("Player updated");
                reload();
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div actions = new Div();
        actions.addClassName("form-actions");
        actions.add(save);
        card.add(form, actions);
        return card;
    }

    private static TextField text(String label, String value) {
        TextField field = new TextField(label);
        field.setValue(value == null ? "" : value);
        return field;
    }

    private static IntegerField number(String label, int value) {
        IntegerField field = new IntegerField(label);
        field.setMin(0);
        field.setValue(value);
        field.setStepButtonsVisible(true);
        return field;
    }

    // ---- decks --------------------------------------------------------------

    private Div decksTab() {
        Div body = new Div();
        body.addClassName("tab-body");
        for (CampaignSheet.PlayerView player : sheet.players()) {
            body.add(new H2("Player " + player.seat()
                    + (player.crawler() == null ? "" : " — " + player.crawler())));

            Grid<CampaignSheet.DeckSlotView> grid = new Grid<>();
            grid.addColumn(CampaignSheet.DeckSlotView::position).setHeader("#")
                    .setWidth("56px").setFlexGrow(0);
            grid.addColumn(CampaignSheet.DeckSlotView::baseCard).setHeader("Base")
                    .setAutoWidth(true);
            grid.addColumn(CampaignSheet.DeckSlotView::cardName).setHeader("Card")
                    .setAutoWidth(true);
            grid.addComponentColumn(this::deckFlags).setHeader("State").setAutoWidth(true);
            grid.addComponentColumn(slot -> {
                Button edit = new Button("Edit", event -> new DeckSlotDialog(
                        campaignService, campaignId, currentUser.id(), slot,
                        this::reload).open());
                edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                return edit;
            }).setHeader("").setWidth("90px").setFlexGrow(0);
            grid.setItems(player.deck());
            grid.setAllRowsVisible(true);
            grid.addClassName("editor-grid");
            body.add(grid);
        }
        return body;
    }

    private Div deckFlags(CampaignSheet.DeckSlotView slot) {
        Div flags = new Div();
        flags.addClassName("deck-flags");
        if (slot.eliminated()) {
            flags.add(chip("eliminated", "chip-danger"));
        }
        if (slot.mastered()) {
            flags.add(chip("mastered", "chip-quiet"));
        }
        if (slot.extraStartingHand()) {
            flags.add(chip("+ hand", "chip-accent"));
        }
        if (slot.upgradeName() != null && !slot.upgradeName().isBlank()) {
            flags.add(chip("↑ " + slot.upgradeName(), "chip-quiet"));
        }
        return flags;
    }

    // ---- floors -------------------------------------------------------------

    private Div floorsTab() {
        Div body = new Div();
        body.addClassName("tab-body");
        Accordion accordion = new Accordion();
        accordion.setWidthFull();
        for (CampaignSheet.FloorView floor : sheet.floors()) {
            accordion.add("Floor " + floor.floorNumber() + " · "
                            + floor.floorScore() + " XP · " + floor.defeats()
                            + (floor.defeats() == 1 ? " defeat" : " defeats"),
                    floorPanel(floor));
        }
        body.add(accordion);
        return body;
    }

    private Div floorPanel(CampaignSheet.FloorView floor) {
        Div body = new Div();
        body.addClassName("floor-panel");

        Div head = new Div();
        head.addClassName("floor-panel-head");
        head.add(chip(floor.completed() ? "cleared " + Ui.date(floor.completedAt())
                : "not cleared", floor.completed() ? "chip-accent" : "chip-quiet"));
        head.add(new Span("Floor score: " + floor.floorScore() + " XP"));
        head.add(new Span("Defeats: " + floor.defeats()));
        body.add(head);

        IntegerField bossXp = new IntegerField("Boss & quest XP");
        bossXp.setMin(0);
        bossXp.setValue(floor.bossQuestXp());
        bossXp.setStepButtonsVisible(true);
        TextArea notes = new TextArea("Floor notes");
        notes.setValue(floor.notes() == null ? "" : floor.notes());
        notes.setMaxLength(600);
        notes.setWidthFull();

        FormLayout form = new FormLayout(bossXp);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        Button save = new Button("Save floor", event -> {
            try {
                campaignService.updateFloor(campaignId, currentUser.id(),
                        floor.floorNumber(), bossXp.getValue(), notes.getValue());
                Ui.success("Floor updated");
                reload();
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div actions = new Div();
        actions.addClassName("form-actions");
        actions.add(save);
        body.add(form, notes, actions);

        Div attemptHead = new Div();
        attemptHead.addClassName("list-head");
        attemptHead.add(new H3("Attempts"));
        Button log = new Button("Log attempt", event -> new AttemptDialog(
                campaignService, campaignId, currentUser.id(), floor.floorNumber(),
                null, this::reload).open());
        log.addThemeVariants(ButtonVariant.LUMO_SMALL);
        attemptHead.add(log);
        body.add(attemptHead);

        if (floor.attempts().isEmpty()) {
            body.add(new Paragraph("No attempts logged for this floor."));
        } else {
            for (CampaignSheet.AttemptView attempt : floor.attempts()) {
                Div row = new Div();
                row.addClassName("attempt-row");
                row.add(new Span(Ui.date(attempt.playedAt())));
                row.add(chip(attempt.successful() ? "cleared" : "defeat",
                        attempt.successful() ? "chip-accent" : "chip-danger"));
                Span note = new Span(Ui.blank(attempt.notes()));
                note.addClassName("attempt-note");
                row.add(note);

                Button edit = new Button("Edit", event -> new AttemptDialog(
                        campaignService, campaignId, currentUser.id(),
                        floor.floorNumber(), attempt, this::reload).open());
                edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                Button delete = new Button("Delete", event -> confirm(
                        "Delete this attempt?",
                        "The defeat count and floor score will be recalculated.",
                        () -> {
                            campaignService.deleteAttempt(campaignId, currentUser.id(),
                                    attempt.id());
                            reload();
                        }));
                delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL,
                        ButtonVariant.LUMO_ERROR);
                row.add(edit, delete);
                body.add(row);
            }
        }
        return body;
    }

    // ---- achievements -------------------------------------------------------

    private Div achievementsTab() {
        Div body = new Div();
        body.addClassName("tab-body");

        List<CampaignSheet.AchievementView> all = new ArrayList<>();
        for (CampaignSheet.FloorView floor : sheet.floors()) {
            all.addAll(floor.achievements());
        }

        Div actions = new Div();
        actions.addClassName("form-actions");
        Button add = new Button("Add achievement", event -> new AchievementDialog(
                campaignService, campaignId, currentUser.id(),
                campaignService.achievementNames(campaignId, currentUser.id()),
                null, this::reload).open());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actions.add(add);
        body.add(actions);

        Grid<CampaignSheet.AchievementView> grid = new Grid<>();
        grid.addColumn(CampaignSheet.AchievementView::floorNumber).setHeader("Floor")
                .setWidth("80px").setFlexGrow(0);
        grid.addColumn(CampaignSheet.AchievementView::name).setHeader("Achievement")
                .setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(CampaignSheet.AchievementView::xp).setHeader("XP")
                .setWidth("80px").setFlexGrow(0);
        grid.addColumn(a -> Ui.date(a.completedAt())).setHeader("Completed")
                .setWidth("130px").setFlexGrow(0);
        grid.addComponentColumn(achievement -> {
            Div buttons = new Div();
            buttons.addClassName("row-actions");
            Button edit = new Button("Edit", event -> new AchievementDialog(
                    campaignService, campaignId, currentUser.id(),
                    campaignService.achievementNames(campaignId, currentUser.id()),
                    achievement, this::reload).open());
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            Button delete = new Button("Delete", event -> confirm(
                    "Delete this achievement?",
                    "Its XP will be removed from the floor score.",
                    () -> {
                        campaignService.deleteAchievement(campaignId, currentUser.id(),
                                achievement.id());
                        reload();
                    }));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_ERROR);
            buttons.add(edit, delete);
            return buttons;
        }).setHeader("").setWidth("160px").setFlexGrow(0);
        grid.setItems(all);
        grid.setAllRowsVisible(true);
        grid.addClassName("editor-grid");
        body.add(grid);
        if (all.isEmpty()) {
            body.add(new Paragraph("No achievements yet. The first one is the hardest."));
        }
        return body;
    }

    // ---- purchases ----------------------------------------------------------

    private Div purchasesTab() {
        Div body = new Div();
        body.addClassName("tab-body");

        Div summary = new Div();
        summary.addClassName("summary-strip");
        summary.add(stat("Available XP", sheet.totals().availableXp()));
        summary.add(stat("Spent XP", sheet.totals().spentXp()));
        body.add(summary);

        Div actions = new Div();
        actions.addClassName("form-actions");
        Button add = new Button("Buy benefit", event -> new PurchaseDialog(
                campaignService, campaignId, currentUser.id(), sheet.players(),
                sheet.totals(), this::reload).open());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actions.add(add);
        body.add(actions);

        Grid<CampaignSheet.PurchaseView> grid = new Grid<>();
        grid.addColumn(p -> "P" + p.seat()).setHeader("Player")
                .setWidth("80px").setFlexGrow(0);
        grid.addColumn(p -> p.benefit().label()).setHeader("Benefit")
                .setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(CampaignSheet.PurchaseView::cost).setHeader("Cost")
                .setWidth("80px").setFlexGrow(0);
        grid.addColumn(p -> {
            String target = Ui.blank(p.targetCard());
            if (p.upgradeName() != null && !p.upgradeName().isBlank()) {
                target += " ↑ " + p.upgradeName();
            }
            return target;
        }).setHeader("Target").setAutoWidth(true);
        grid.addComponentColumn(purchase -> {
            Button delete = new Button("Delete", event -> confirm(
                    "Delete this purchase?",
                    "Available XP will be recalculated. Deck flags are left as they are.",
                    () -> {
                        campaignService.deletePurchase(campaignId, currentUser.id(),
                                purchase.id());
                        reload();
                    }));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL,
                    ButtonVariant.LUMO_ERROR);
            return delete;
        }).setHeader("").setWidth("100px").setFlexGrow(0);
        grid.setItems(sheet.purchases());
        grid.setAllRowsVisible(true);
        grid.addClassName("editor-grid");
        body.add(grid);
        return body;
    }

    // ---- share --------------------------------------------------------------

    private Div shareTab() {
        Div body = new Div();
        body.addClassName("tab-body");

        Div card = new Div();
        card.addClassName("form-card");
        card.add(new H2("Invite the party"));
        card.add(new Paragraph("Invite links are reusable until you revoke them. "
                + "Viewers can read the sheet; editors can change it."));

        ComboBox<AccessRole> role = new ComboBox<>("Role");
        role.setItems(AccessRole.VIEWER, AccessRole.EDITOR);
        role.setItemLabelGenerator(value -> value.name().toLowerCase());
        role.setValue(AccessRole.EDITOR);

        IntegerField days = new IntegerField("Expires after (days)");
        days.setMin(0);
        days.setValue(30);
        days.setHelperText("0 means the link never expires.");

        FormLayout form = new FormLayout(role, days);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("420px", 2));

        Button create = new Button("Create invite link", event -> {
            try {
                InviteService.CreatedInvite invite = inviteService.create(campaignId,
                        currentUser.id(), role.getValue(), days.getValue());
                String url = inviteLink(invite.token());
                reload();
                showInviteLink(url);
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Div actions = new Div();
        actions.addClassName("form-actions");
        actions.add(create);
        card.add(form, actions);
        body.add(card);

        List<InviteView> invites = inviteService.list(campaignId, currentUser.id());
        body.add(new H2("Active and past links"));
        if (invites.isEmpty()) {
            body.add(new Paragraph("No invite links yet."));
        } else {
            Grid<InviteView> grid = new Grid<>();
            grid.addColumn(invite -> invite.role().name().toLowerCase()).setHeader("Role")
                    .setAutoWidth(true);
            grid.addColumn(invite -> invite.createdAt().toString().substring(0, 10))
                    .setHeader("Created").setAutoWidth(true);
            grid.addColumn(invite -> invite.expiresAt() == null ? "never"
                    : invite.expiresAt().toString().substring(0, 10))
                    .setHeader("Expires").setAutoWidth(true);
            grid.addComponentColumn(invite -> chip(invite.usable() ? "usable" : "revoked",
                    invite.usable() ? "chip-accent" : "chip-danger"))
                    .setHeader("Status").setAutoWidth(true);
            grid.addComponentColumn(invite -> {
                Button revoke = new Button("Revoke", event -> confirm(
                        "Revoke this link?",
                        "Anyone using it will lose access on their next request.",
                        () -> {
                            inviteService.revoke(campaignId, currentUser.id(), invite.id());
                            reload();
                        }));
                revoke.setEnabled(invite.usable());
                revoke.addThemeVariants(ButtonVariant.LUMO_TERTIARY,
                        ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
                return revoke;
            }).setHeader("").setAutoWidth(true);
            grid.setItems(invites);
            grid.setAllRowsVisible(true);
            grid.addClassName("editor-grid");
            body.add(grid);
        }
        return body;
    }

    private void showInviteLink(String url) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Invite link ready");
        dialog.setWidth("min(560px, 94vw)");
        TextField link = new TextField("Share this URL");
        link.setValue(url);
        link.setReadOnly(true);
        link.setWidthFull();
        Button copy = new Button("Copy", event -> {
            UI.getCurrent().getPage().executeJs(
                    "window.Unstoppable.copyText($0)", url);
            Ui.success("Link copied");
        });
        copy.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button close = new Button("Close", event -> dialog.close());
        dialog.add(link);
        dialog.getFooter().add(close, copy);
        dialog.open();
    }

    private String inviteLink(String token) {
        VaadinRequest request = VaadinRequest.getCurrent();
        String host = firstHeader(request, "X-Forwarded-Host", "Host");
        if (host == null) {
            return "/join/" + token;
        }
        String proto = firstHeader(request, "X-Forwarded-Proto");
        if (proto == null) {
            proto = request.isSecure() ? "https" : "http";
        }
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        return proto + "://" + host + context + "/join/" + token;
    }

    private static String firstHeader(VaadinRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return value.split(",")[0].trim();
            }
        }
        return null;
    }

    // ---- helpers ------------------------------------------------------------

    private void confirm(String title, String message, Runnable action) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(title);
        dialog.add(new Paragraph(message));
        Button cancel = new Button("Cancel", event -> dialog.close());
        Button confirm = new Button("Delete", event -> {
            try {
                action.run();
                dialog.close();
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private static Span chip(String text, String className) {
        Span chip = new Span(text);
        chip.addClassName("chip");
        chip.addClassName(className);
        return chip;
    }
}
