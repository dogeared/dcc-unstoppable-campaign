package com.example.unstoppable.ui.component;

import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.ui.Ui;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextArea;

import java.time.LocalDate;
import java.util.UUID;

public class AttemptDialog extends Dialog {

    public AttemptDialog(CampaignService service, UUID campaignId, String subject,
            int floorNumber, CampaignSheet.AttemptView existing, Runnable onSaved) {
        boolean edit = existing != null;
        setHeaderTitle((edit ? "Edit" : "Log") + " attempt · Floor " + floorNumber);
        setWidth("min(480px, 94vw)");

        DatePicker playedAt = new DatePicker("Date played");
        playedAt.setValue(edit ? existing.playedAt() : LocalDate.now());
        Checkbox successful = new Checkbox("Floor cleared");
        successful.setValue(edit && existing.successful());
        TextArea notes = new TextArea("Notes");
        notes.setValue(edit && existing.notes() != null ? existing.notes() : "");
        notes.setMaxLength(600);
        notes.setPlaceholder("Party, deaths, close calls, anything worth remembering");

        add(playedAt, successful, notes);

        Button cancel = new Button("Cancel", event -> close());
        Button save = new Button(edit ? "Save attempt" : "Log attempt", event -> {
            try {
                if (edit) {
                    service.updateAttempt(campaignId, subject, existing.id(),
                            playedAt.getValue(), successful.getValue(), notes.getValue());
                } else {
                    service.addAttempt(campaignId, subject, floorNumber,
                            playedAt.getValue(), successful.getValue(), notes.getValue());
                }
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
