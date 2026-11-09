package com.example.unstoppable.ui.component;

import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.ui.Ui;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class AchievementDialog extends Dialog {

    public AchievementDialog(CampaignService service, UUID campaignId, String subject,
            List<String> knownNames, CampaignSheet.AchievementView existing,
            Runnable onSaved) {
        boolean edit = existing != null;
        setHeaderTitle(edit ? "Edit achievement" : "Add achievement");
        setWidth("min(520px, 94vw)");

        ComboBox<Integer> floor = new ComboBox<>("Floor");
        floor.setItems(IntStream.rangeClosed(1, CampaignService.FLOOR_COUNT).boxed().toList());
        floor.setItemLabelGenerator(value -> "Floor " + value);
        floor.setValue(edit ? existing.floorNumber() : 1);

        ComboBox<String> name = new ComboBox<>("Achievement");
        name.setItems(knownNames);
        name.setAllowCustomValue(true);
        name.addCustomValueSetListener(event -> name.setValue(event.getDetail()));
        name.setValue(edit ? existing.name() : "");
        name.setHelperText("Pick a past achievement or type a new one.");
        name.setWidthFull();

        IntegerField xp = new IntegerField("XP");
        xp.setMin(0);
        xp.setStepButtonsVisible(true);
        xp.setValue(edit ? existing.xp() : 0);

        DatePicker completedAt = new DatePicker("Completed");
        completedAt.setValue(edit ? existing.completedAt() : LocalDate.now());

        FormLayout form = new FormLayout(floor, xp, completedAt);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("460px", 3));
        add(form, name);

        Button cancel = new Button("Cancel", event -> close());
        Button save = new Button(edit ? "Save achievement" : "Add achievement", event -> {
            try {
                if (edit) {
                    service.updateAchievement(campaignId, subject, existing.id(),
                            floor.getValue(), name.getValue(), xp.getValue(),
                            completedAt.getValue());
                } else {
                    service.addAchievement(campaignId, subject, floor.getValue(),
                            name.getValue(), xp.getValue(), completedAt.getValue());
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
