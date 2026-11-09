package com.example.unstoppable.ui;

import com.example.unstoppable.domain.Difficulty;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CurrentUser;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@Route(value = "campaigns/new", layout = MainLayout.class)
@PageTitle("New campaign")
@PermitAll
public class NewCampaignView extends VerticalLayout {

    private final CampaignService campaignService;
    private final CurrentUser currentUser;

    public NewCampaignView(CampaignService campaignService, CurrentUser currentUser) {
        this.campaignService = campaignService;
        this.currentUser = currentUser;
        setPadding(false);
        setSpacing(false);
        addClassName("page");
        render();
    }

    private void render() {
        Div head = new Div();
        head.addClassName("page-head");
        Div copy = new Div();
        copy.addClassName("page-head-copy");
        Span eyebrow = new Span("New sheet");
        eyebrow.addClassName("eyebrow");
        copy.add(new H1("Start a campaign"));
        copy.add(new Paragraph("You can change the party name and difficulty later."));
        head.add(copy);
        add(head);

        Div card = new Div();
        card.addClassName("form-card");

        TextField partyName = new TextField("Party name");
        partyName.setRequiredIndicatorVisible(true);
        partyName.setMaxLength(120);
        partyName.setPlaceholder("The brave and the doomed");
        partyName.setValueChangeMode(ValueChangeMode.EAGER);

        ComboBox<Difficulty> difficulty = new ComboBox<>("Difficulty");
        difficulty.setItems(Difficulty.values());
        difficulty.setItemLabelGenerator(Difficulty::label);
        difficulty.setValue(Difficulty.NORMAL);

        FormLayout form = new FormLayout(partyName, difficulty);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("620px", 2));

        Button create = new Button("Create campaign");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.setEnabled(false);
        partyName.addValueChangeListener(event ->
                create.setEnabled(!event.getValue().isBlank()));
        create.addClickListener(event -> {
            try {
                var id = campaignService.create(currentUser.id(), partyName.getValue(),
                        difficulty.getValue());
                UI.getCurrent().navigate("campaigns/" + id + "/edit");
            } catch (RuntimeException exception) {
                Ui.error(exception.getMessage());
            }
        });

        Div actions = new Div();
        actions.addClassName("form-actions");
        actions.add(create);

        card.add(form, actions);
        add(card);
    }
}
