package com.example.unstoppable.ui;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.service.CampaignNotFoundException;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CurrentUser;
import com.example.unstoppable.service.InviteService;
import com.example.unstoppable.service.InviteView;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = UiTestConfig.class)
@ViewPackages(classes = CampaignEditorView.class)
class CampaignEditorViewTest extends SpringBrowserlessTest {

    @Autowired private CampaignService campaignService;
    @Autowired private InviteService inviteService;
    @Autowired private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        reset(campaignService, inviteService, currentUser);
        lenient().when(currentUser.id()).thenReturn("owner");
        lenient().when(inviteService.list(any(), any())).thenReturn(List.of());
    }

    private void openEditor(CampaignSheetFixture fixture) {
        when(campaignService.load(any(), any())).thenReturn(fixture.sheet());
        navigate(CampaignEditorView.class,
                Map.of("campaignId", UiFixtures.CAMPAIGN_ID.toString()));
    }

    private record CampaignSheetFixture(com.example.unstoppable.service.CampaignSheet sheet) {
    }

    private Button button(String text) {
        return find(Button.class).all().stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst().orElseThrow();
    }

    private List<Button> buttons(String text) {
        return find(Button.class).all().stream()
                .filter(button -> text.equals(button.getText()))
                .toList();
    }

    private Grid gridWithHeader(String header) {
        return find(Grid.class).all().stream()
                .filter(grid -> header.equals(((Grid.Column<?>) grid.getColumns()
                        .getFirst()).getHeaderText()))
                .findFirst().orElseThrow();
    }

    @Test
    void editorRendersEveryTabAndGridRowActions() {
        when(campaignService.achievementNames(any(), any()))
                .thenReturn(List.of("First blood"));
        when(inviteService.list(any(), any()))
                .thenReturn(List.of(UiFixtures.invite(true), UiFixtures.invite(false)));
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.OWNER)));

        assertEquals("The Brave", find(H1.class).first().getText());
        TabSheet tabs = find(TabSheet.class).first();
        assertEquals(7, tabs.getTabCount());

        test(tabs).select("Deck lists");
        Grid decks = gridWithHeader("#");
        Component deckActions = test(decks).renderCellComponent(0, 4);
        assertTrue(deckActions instanceof Button);
        test((Button) deckActions).click();
        assertTrue(find(Dialog.class).all().stream()
                .anyMatch(dialog -> dialog.getHeaderTitle() != null
                        && dialog.getHeaderTitle().contains("Deck slot")));

        test(find(TabSheet.class).first()).select("Achievements");
        Grid achievements = gridWithHeader("Floor");
        Component achievementActions = test(achievements).renderCellComponent(0, 4);
        List<Button> achievementButtons = find(Button.class, achievementActions).all();
        test(achievementButtons.get(0)).click();
        assertTrue(find(Dialog.class).all().stream()
                .anyMatch(dialog -> "Edit achievement".equals(dialog.getHeaderTitle())));

        test(find(TabSheet.class).first()).select("Purchases");
        Grid purchases = gridWithHeader("Player");
        Component purchaseActions = test(purchases).renderCellComponent(0, 4);
        test((Button) purchaseActions).click();
        Dialog confirm = find(Dialog.class).all().stream()
                .filter(dialog -> "Delete this purchase?".equals(dialog.getHeaderTitle()))
                .findFirst().orElseThrow();
        Button delete = find(Button.class, confirm).all().stream()
                .filter(button -> "Delete".equals(button.getText()))
                .findFirst().orElseThrow();
        test(delete).click();
        verify(campaignService).deletePurchase(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                any());

        test(find(TabSheet.class).first()).select("Share");
        Grid invites = gridWithHeader("Role");
        Component inviteActions = test(invites).renderCellComponent(0, 5);
        Button revokeButtonInCell = find(Button.class, inviteActions).all().stream()
                .filter(button -> "Revoke".equals(button.getText()))
                .findFirst().orElseThrow();
        test(revokeButtonInCell).click();
        Dialog revoke = find(Dialog.class).all().stream()
                .filter(dialog -> "Revoke this link?".equals(dialog.getHeaderTitle()))
                .findFirst().orElseThrow();
        Button revokeButton = find(Button.class, revoke).all().stream()
                .filter(button -> "Delete".equals(button.getText()))
                .findFirst().orElseThrow();
        test(revokeButton).click();
        verify(inviteService).revoke(eq(UiFixtures.CAMPAIGN_ID), eq("owner"), any());
    }

    @Test
    void editorSavesPartyPlayersAndFloors() {
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.OWNER)));

        TabSheet tabs = find(TabSheet.class).first();
        test(tabs).select("Party");
        test(button("Save party")).click();
        verify(campaignService).updateParty(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq("The Brave"), any());

        test(find(TabSheet.class).first()).select("Players");
        TextField crawler = find(TextField.class).all().stream()
                .filter(field -> "Carl".equals(field.getValue()))
                .findFirst().orElseThrow();
        crawler.setValue("Carl the Crawler");
        test(buttons("Save player").getFirst()).click();
        verify(campaignService).updatePlayer(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(1), eq("Carl the Crawler"), eq("Human"), eq("Crawler"),
                eq(20), eq(10), eq(5), eq("Signature A"), eq("Signature B"));

        test(find(TabSheet.class).first()).select("Floors");
        IntegerField bossXp = find(IntegerField.class).all().stream()
                .filter(field -> "Boss & quest XP".equals(field.getLabel()))
                .findFirst().orElseThrow();
        bossXp.setValue(30);
        test(buttons("Save floor").getFirst()).click();
        verify(campaignService).updateFloor(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(1), eq(30), any());
    }

    @Test
    void editorTogglesCampaignStatus() {
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.OWNER)));

        TabSheet tabs = find(TabSheet.class).first();
        test(tabs).select("Party");
        test(button("Mark complete")).click();
        verify(campaignService).completeCampaign(UiFixtures.CAMPAIGN_ID, "owner");

        when(campaignService.load(any(), any()))
                .thenReturn(UiFixtures.completedSheet(AccessRole.OWNER));
        navigate(CampaignEditorView.class,
                Map.of("campaignId", UiFixtures.CAMPAIGN_ID.toString()));
        test(find(TabSheet.class).first()).select("Party");
        test(button("Reopen campaign")).click();
        verify(campaignService).reopenCampaign(UiFixtures.CAMPAIGN_ID, "owner");
    }

    @Test
    void editorCreatesAndShowsAnInviteLink() {
        AtomicInteger reloads = new AtomicInteger();
        when(campaignService.load(any(), any())).thenAnswer(invocation -> {
            reloads.incrementAndGet();
            return UiFixtures.richSheet(AccessRole.OWNER);
        });
        when(inviteService.create(any(), any(), any(), anyBoolean(), anyInt()))
                .thenReturn(new InviteService.CreatedInvite("token-1",
                        UiFixtures.invite(true)));

        navigate(CampaignEditorView.class,
                Map.of("campaignId", UiFixtures.CAMPAIGN_ID.toString()));
        test(find(TabSheet.class).first()).select("Share");
        test(button("Create invite link")).click();

        verify(inviteService).create(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(AccessRole.VIEWER), eq(false), anyInt());
        Dialog link = find(Dialog.class).all().stream()
                .filter(dialog -> "Invite link ready".equals(dialog.getHeaderTitle()))
                .findFirst().orElseThrow();
        TextField url = find(TextField.class, link).first();
        assertTrue(url.getValue().endsWith("/join/token-1"));

        Button copy = find(Button.class, link).all().stream()
                .filter(button -> "Copy".equals(button.getText()))
                .findFirst().orElseThrow();
        test(copy).click();
        Button close = find(Button.class, link).all().stream()
                .filter(button -> "Close".equals(button.getText()))
                .findFirst().orElseThrow();
        test(close).click();
        assertFalse(test(link).isOpen());
    }

    @Test
    void shareTabDefaultsToViewerRole() {
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.OWNER)));

        test(find(TabSheet.class).first()).select("Share");
        ComboBox<?> role = find(ComboBox.class).all().stream()
                .filter(combo -> "Role".equals(combo.getLabel()))
                .findFirst().orElseThrow();

        assertEquals(AccessRole.VIEWER, role.getValue());
    }

    @Test
    void shareTabDefaultsToPrivateViewerLinks() {
        when(inviteService.list(any(), any())).thenReturn(List.of());
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.OWNER)));

        test(find(TabSheet.class).first()).select("Share");
        ComboBox<?> role = find(ComboBox.class).all().stream()
                .filter(combo -> "Role".equals(combo.getLabel()))
                .findFirst().orElseThrow();
        Checkbox publicLink = find(Checkbox.class).all().stream()
                .filter(box -> "Public link".equals(box.getLabel()))
                .findFirst().orElseThrow();

        assertEquals(AccessRole.VIEWER, role.getValue());
        assertFalse(publicLink.getValue());
        assertTrue(publicLink.isEnabled());
    }

    @Test
    void choosingTheEditorRoleClearsAndLocksPublicVisibility() {
        when(inviteService.list(any(), any())).thenReturn(List.of());
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.OWNER)));

        test(find(TabSheet.class).first()).select("Share");
        ComboBox<AccessRole> role = find(ComboBox.class).all().stream()
                .filter(combo -> "Role".equals(combo.getLabel()))
                .findFirst().map(combo -> (ComboBox<AccessRole>) combo)
                .orElseThrow();
        Checkbox publicLink = find(Checkbox.class).all().stream()
                .filter(box -> "Public link".equals(box.getLabel()))
                .findFirst().orElseThrow();

        publicLink.setValue(true);
        role.setValue(AccessRole.EDITOR);

        assertFalse(publicLink.isEnabled());
        assertFalse(publicLink.getValue());

        role.setValue(AccessRole.VIEWER);
        assertTrue(publicLink.isEnabled());
    }

    @Test
    void shareGridShowsVisibilityAndEditsAnExistingLink() {
        InviteView invite = UiFixtures.invite(true);
        when(inviteService.list(any(), any())).thenReturn(List.of(invite));
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.OWNER)));

        test(find(TabSheet.class).first()).select("Share");
        Grid invites = gridWithHeader("Role");
        Component visibility = test(invites).renderCellComponent(0, 1);
        assertEquals("private", ((com.vaadin.flow.component.html.Span) visibility).getText());

        Component inviteActions = test(invites).renderCellComponent(0, 5);
        Button edit = find(Button.class, inviteActions).all().stream()
                .filter(button -> "Edit".equals(button.getText()))
                .findFirst().orElseThrow();
        test(edit).click();

        Dialog dialog = find(Dialog.class).all().stream()
                .filter(open -> "Edit invite link".equals(open.getHeaderTitle()))
                .findFirst().orElseThrow();
        ComboBox<AccessRole> role = find(ComboBox.class, dialog).first();
        assertEquals(AccessRole.EDITOR, role.getValue());

        role.setValue(AccessRole.VIEWER);
        Checkbox publicLink = find(Checkbox.class, dialog).first();
        publicLink.setValue(true);
        Button save = find(Button.class, dialog).all().stream()
                .filter(button -> "Save".equals(button.getText()))
                .findFirst().orElseThrow();
        test(save).click();

        verify(inviteService).update(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(invite.id()), eq(AccessRole.VIEWER), eq(true));
    }

    @Test
    void editorRejectsViewers() {
        openEditor(new CampaignSheetFixture(UiFixtures.richSheet(AccessRole.VIEWER)));

        assertEquals("You can view this campaign but not edit it.",
                find(H1.class).first().getText());
        assertTrue(find(TabSheet.class).all().isEmpty());
    }

    @Test
    void editorHandlesMalformedAndMissingCampaigns() {
        navigate(CampaignEditorView.class, Map.of("campaignId", "nope"));
        assertEquals("This sheet is not available.", find(H1.class).first().getText());

        org.mockito.Mockito.doThrow(new CampaignNotFoundException(UUID.randomUUID()))
                .when(campaignService).load(any(), any());
        navigate(CampaignEditorView.class,
                Map.of("campaignId", UUID.randomUUID().toString()));
        assertEquals("This sheet is not available.", find(H1.class).first().getText());

        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(campaignService).load(any(), any());
        navigate(CampaignEditorView.class,
                Map.of("campaignId", UUID.randomUUID().toString()));
        assertEquals("This sheet could not be displayed.", find(H1.class).first().getText());
    }
}
