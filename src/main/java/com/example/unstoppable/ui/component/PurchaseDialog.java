package com.example.unstoppable.ui.component;

import com.example.unstoppable.domain.Benefit;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.service.CampaignTotals;
import com.example.unstoppable.ui.Ui;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.UUID;

public class PurchaseDialog extends Dialog {

    public PurchaseDialog(CampaignService service, UUID campaignId, String subject,
            List<CampaignSheet.PlayerView> players, CampaignTotals totals,
            Runnable onSaved) {
        setHeaderTitle("Buy a permanent benefit");
        setWidth("min(600px, 94vw)");

        ComboBox<CampaignSheet.PlayerView> player = new ComboBox<>("Player");
        player.setItems(players);
        player.setItemLabelGenerator(p -> "Player " + p.seat()
                + (p.crawler() == null || p.crawler().isBlank() ? "" : " — " + p.crawler()));
        player.setValue(players.getFirst());

        ComboBox<Benefit> benefit = new ComboBox<>("Benefit");
        benefit.setItems(Benefit.values());
        benefit.setItemLabelGenerator(b -> b.label() + "  (" + b.costLabel() + " XP)");
        benefit.setValue(Benefit.ELIMINATE_STARTING);

        IntegerField x = new IntegerField("X — upgrade gold cost");
        x.setMin(0);
        x.setStepButtonsVisible(true);
        x.setValue(0);

        ComboBox<CampaignSheet.DeckSlotView> slot = new ComboBox<>("Deck slot");
        slot.setItemLabelGenerator(s -> s.position() + " · " + s.baseCard()
                + (s.cardName() == null || s.cardName().equals(s.baseCard())
                        ? "" : " → " + s.cardName()));
        slot.setWidthFull();

        TextField targetCard = new TextField("Card being mastered");
        TextField upgradeName = new TextField("Upgrade name");
        TextArea notes = new TextArea("Notes");
        notes.setMaxLength(600);

        Span cost = new Span();
        cost.addClassName("cost-preview");
        Span available = new Span("Available XP: " + totals.availableXp());
        available.addClassName("cost-available");

        FormLayout form = new FormLayout(player, benefit, x, slot, targetCard,
                upgradeName);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("520px", 2));
        add(form, notes, cost, available);

        Runnable refresh = () -> {
            Benefit selected = benefit.getValue();
            boolean variable = selected.variableCost();
            x.setVisible(variable);
            slot.setVisible(selected.requiresDeckSlot());
            upgradeName.setVisible(variable);
            targetCard.setVisible(selected == Benefit.MASTER_LEVEL1
                    || selected == Benefit.MASTER_LEVEL2);

            if (player.getValue() != null) {
                slot.setItems(player.getValue().deck());
            } else {
                slot.setItems(List.of());
            }

            int value;
            try {
                value = selected.cost(variable ? x.getValue() : null);
            } catch (RuntimeException ignored) {
                value = 0;
            }
            cost.setText("Cost: " + value + " XP");
        };

        benefit.addValueChangeListener(event -> refresh.run());
        player.addValueChangeListener(event -> {
            slot.clear();
            refresh.run();
        });
        x.addValueChangeListener(event -> refresh.run());
        refresh.run();

        Button cancel = new Button("Cancel", event -> close());
        Button save = new Button("Buy benefit", event -> {
            try {
                service.addPurchase(campaignId, subject, player.getValue().id(),
                        benefit.getValue(),
                        benefit.getValue().variableCost() ? x.getValue() : null,
                        slot.getValue() == null ? null : slot.getValue().id(),
                        targetCard.getValue(), upgradeName.getValue(), notes.getValue());
                close();
                onSaved.run();
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }
}
