package com.example.unstoppable.ui.component;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.service.InviteView;
import com.example.unstoppable.ui.Ui;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinRequest;

import java.util.List;
import java.util.UUID;

/**
 * Owner-only invite management: create reusable links, review existing ones,
 * and revoke them. Shared by the editor's Share tab and the sheet's Share
 * dialog.
 */
public class SharePanel extends Div {

    private final InviteService inviteService;
    private final CurrentUser currentUser;
    private final UUID campaignId;
    private final Div invitesBody = new Div();

    public SharePanel(InviteService inviteService, CurrentUser currentUser, UUID campaignId) {
        this.inviteService = inviteService;
        this.currentUser = currentUser;
        this.campaignId = campaignId;
        addClassName("tab-body");
        add(inviteCard(), invitesBody);
        refreshInvites();
    }

    private Div inviteCard() {
        Div card = new Div();
        card.addClassName("form-card");
        card.add(new H2("Invite the party"));
        card.add(new Paragraph("Invite links are reusable until you revoke them. "
                + "Viewers can read the sheet; editors can change it."));

        ComboBox<AccessRole> role = new ComboBox<>("Role");
        role.setItems(AccessRole.VIEWER, AccessRole.EDITOR);
        role.setItemLabelGenerator(value -> value.name().toLowerCase());
        role.setValue(AccessRole.VIEWER);

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
                refreshInvites();
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
        return card;
    }

    private void refreshInvites() {
        invitesBody.removeAll();
        invitesBody.add(new H2("Active and past links"));

        List<InviteView> invites = inviteService.list(campaignId, currentUser.id());
        if (invites.isEmpty()) {
            invitesBody.add(new Paragraph("No invite links yet."));
            return;
        }

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
                        refreshInvites();
                    }));
            revoke.setEnabled(invite.usable());
            revoke.addThemeVariants(ButtonVariant.LUMO_TERTIARY,
                    ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            return revoke;
        }).setHeader("").setAutoWidth(true);
        grid.setItems(invites);
        grid.setAllRowsVisible(true);
        grid.addClassName("editor-grid");
        invitesBody.add(grid);
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
