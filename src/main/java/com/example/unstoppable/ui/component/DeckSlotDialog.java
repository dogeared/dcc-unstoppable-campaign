package com.example.unstoppable.ui.component;

import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.ui.Ui;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.UUID;

public class DeckSlotDialog extends Dialog {

    public DeckSlotDialog(CampaignService service, UUID campaignId, String subject,
            CampaignSheet.DeckSlotView slot, Runnable onSaved) {
        setHeaderTitle("Deck slot " + slot.position() + " · " + slot.baseCard());
        setWidth("min(520px, 94vw)");

        TextField cardName = new TextField("Card name");
        cardName.setValue(slot.cardName() == null ? slot.baseCard() : slot.cardName());
        TextField upgradeName = new TextField("Upgrade");
        upgradeName.setValue(slot.upgradeName() == null ? "" : slot.upgradeName());
        upgradeName.setPlaceholder("Name written next to the card");
        Checkbox eliminated = new Checkbox("Eliminated (crossed out)");
        eliminated.setValue(slot.eliminated());
        Checkbox mastered = new Checkbox("Mastered");
        mastered.setValue(slot.mastered());
        Checkbox extra = new Checkbox("Extra starting-hand card");
        extra.setValue(slot.extraStartingHand());
        TextArea notes = new TextArea("Notes");
        notes.setValue(slot.notes() == null ? "" : slot.notes());
        notes.setMaxLength(600);

        FormLayout form = new FormLayout(cardName, upgradeName);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("460px", 2));
        add(form, eliminated, mastered, extra, notes);

        Button cancel = new Button("Cancel", event -> close());
        Button save = new Button("Save slot", event -> {
            try {
                service.updateDeckSlot(campaignId, subject, slot.id(),
                        cardName.getValue(), eliminated.getValue(),
                        upgradeName.getValue(), extra.getValue(),
                        mastered.getValue(), notes.getValue());
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
