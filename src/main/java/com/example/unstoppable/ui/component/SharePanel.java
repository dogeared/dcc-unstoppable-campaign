package com.example.unstoppable.ui.component;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.service.InviteView;
import com.example.unstoppable.ui.Ui;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
 * Owner-only invite management: create reusable links, edit their role and
 * visibility, review existing ones, and revoke them. Shared by the editor's
 * Share tab and the sheet's Share dialog.
 */
public class SharePanel extends Div {

    static final String PUBLIC_HELPER = "Public links can be opened without signing in. "
            + "Editor links must be private.";

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

        ComboBox<AccessRole> role = roleField(AccessRole.VIEWER);
        IntegerField days = new IntegerField("Expires after (days)");
        days.setMin(0);
        days.setValue(30);
        days.setHelperText("0 means the link never expires.");
        Checkbox publicLink = publicField(false, AccessRole.VIEWER);
        role.addValueChangeListener(event -> syncVisibility(role.getValue(), publicLink));

        FormLayout form = new FormLayout(role, days, publicLink);
        form.setColspan(publicLink, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("420px", 2));

        Button create = new Button("Create invite link", event -> {
            try {
                InviteService.CreatedInvite invite = inviteService.create(campaignId,
                        currentUser.id(), role.getValue(), publicLink.getValue(),
                        days.getValue());
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
        grid.addComponentColumn(invite -> chip(
                invite.publicAccess() ? "public" : "private",
                invite.publicAccess() ? "chip-accent" : "chip-quiet"))
                .setHeader("Visibility").setAutoWidth(true);
        grid.addColumn(invite -> invite.createdAt().toString().substring(0, 10))
                .setHeader("Created").setAutoWidth(true);
        grid.addColumn(invite -> invite.expiresAt() == null ? "never"
                : invite.expiresAt().toString().substring(0, 10))
                .setHeader("Expires").setAutoWidth(true);
        grid.addComponentColumn(invite -> chip(invite.usable() ? "usable" : "revoked",
                invite.usable() ? "chip-accent" : "chip-danger"))
                .setHeader("Status").setAutoWidth(true);
        grid.addComponentColumn(invite -> {
            Div cell = new Div();
            cell.addClassName("invite-actions");

            Button edit = new Button("Edit", event -> editInvite(invite));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            cell.add(edit);

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
            cell.add(revoke);
            return cell;
        }).setHeader("").setAutoWidth(true);
        grid.setItems(invites);
        grid.setAllRowsVisible(true);
        grid.addClassName("editor-grid");
        invitesBody.add(grid);
    }

    private void editInvite(InviteView invite) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit invite link");
        dialog.setWidth("min(520px, 94vw)");
        dialog.add(new Paragraph("The link URL stays the same. Changing the role or "
                + "visibility takes effect immediately."));

        ComboBox<AccessRole> role = roleField(invite.role());
        Checkbox publicLink = publicField(invite.publicAccess(), invite.role());
        role.addValueChangeListener(event -> syncVisibility(role.getValue(), publicLink));

        FormLayout form = new FormLayout(role, publicLink);
        form.setColspan(publicLink, 2);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("420px", 2));

        Button save = new Button("Save", event -> {
            try {
                inviteService.update(campaignId, currentUser.id(), invite.id(),
                        role.getValue(), publicLink.getValue());
                dialog.close();
                refreshInvites();
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button("Cancel", event -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private static ComboBox<AccessRole> roleField(AccessRole value) {
        ComboBox<AccessRole> role = new ComboBox<>("Role");
        role.setItems(AccessRole.VIEWER, AccessRole.EDITOR);
        role.setItemLabelGenerator(selected -> selected.name().toLowerCase());
        role.setValue(value);
        return role;
    }

    private static Checkbox publicField(boolean value, AccessRole role) {
        Checkbox publicLink = new Checkbox("Public link");
        publicLink.setHelperText(PUBLIC_HELPER);
        publicLink.setValue(value);
        syncVisibility(role, publicLink);
        return publicLink;
    }

    static void syncVisibility(AccessRole role, Checkbox publicLink) {
        boolean canBePublic = role != AccessRole.EDITOR;
        publicLink.setEnabled(canBePublic);
        if (!canBePublic) {
            publicLink.setValue(false);
        }
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
