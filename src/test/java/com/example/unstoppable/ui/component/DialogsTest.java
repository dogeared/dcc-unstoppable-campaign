package com.example.unstoppable.ui.component;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Benefit;
import com.example.unstoppable.service.CampaignService;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.ui.UiFixtures;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = com.example.unstoppable.ui.UiTestConfig.class)
@ViewPackages(classes = com.example.unstoppable.ui.LandingView.class)
class DialogsTest extends SpringBrowserlessTest {

    @Autowired private CampaignService service;

    private CampaignSheet sheet;

    @BeforeEach
    void setUp() {
        reset(service);
        sheet = UiFixtures.richSheet(AccessRole.OWNER);
    }

    private Button button(Dialog dialog, String text) {
        return find(Button.class, dialog).all().stream()
                .filter(button -> text.equals(button.getText()))
                .findFirst().orElseThrow();
    }

    @Test
    void achievementDialogAddsAndClosesOnCancel() {
        AtomicInteger saved = new AtomicInteger();
        AchievementDialog dialog = new AchievementDialog(service,
                UiFixtures.CAMPAIGN_ID, "owner", List.of("First blood"), null,
                saved::incrementAndGet);
        test(dialog).open();

        test(button(dialog, "Add achievement")).click();

        verify(service).addAchievement(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(1), eq(""), eq(0), any(LocalDate.class));
        assertEquals(1, saved.get());

        AchievementDialog cancelled = new AchievementDialog(service,
                UiFixtures.CAMPAIGN_ID, "owner", List.of(), null,
                saved::incrementAndGet);
        test(cancelled).open();
        test(button(cancelled, "Cancel")).click();
        assertEquals(1, saved.get());
    }

    @Test
    void achievementDialogEditsExisting() {
        CampaignSheet.AchievementView existing = sheet.floors().getFirst()
                .achievements().getFirst();
        AchievementDialog dialog = new AchievementDialog(service,
                UiFixtures.CAMPAIGN_ID, "owner", List.of(), existing, () -> {
                });
        test(dialog).open();

        test(button(dialog, "Save achievement")).click();

        verify(service).updateAchievement(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(existing.id()), eq(existing.floorNumber()), eq(existing.name()),
                eq(existing.xp()), eq(existing.completedAt()));
    }

    @Test
    void attemptDialogLogsAndEdits() {
        AtomicInteger saved = new AtomicInteger();
        AttemptDialog add = new AttemptDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", 3, null, saved::incrementAndGet);
        test(add).open();
        test(button(add, "Log attempt")).click();

        verify(service).addAttempt(eq(UiFixtures.CAMPAIGN_ID), eq("owner"), eq(3),
                any(LocalDate.class), eq(false), eq(""));
        assertEquals(1, saved.get());

        CampaignSheet.AttemptView existing = sheet.floors().getFirst()
                .attempts().getFirst();
        AttemptDialog edit = new AttemptDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", 1, existing, saved::incrementAndGet);
        test(edit).open();
        test(button(edit, "Save attempt")).click();

        verify(service).updateAttempt(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(existing.id()), eq(existing.playedAt()), eq(true),
                eq(existing.notes()));
        assertEquals(2, saved.get());

        AttemptDialog cancel = new AttemptDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", 1, null, saved::incrementAndGet);
        test(cancel).open();
        test(button(cancel, "Cancel")).click();
        assertEquals(2, saved.get());
    }

    @Test
    void dialogSaveErrorsSurfaceAsNotifications() {
        doThrow(new IllegalArgumentException("nope")).when(service)
                .addAchievement(any(), any(), anyInt(), any(), anyInt(), any());

        AchievementDialog dialog = new AchievementDialog(service,
                UiFixtures.CAMPAIGN_ID, "owner", List.of(), null, () -> {
                });
        test(dialog).open();
        test(button(dialog, "Add achievement")).click();

        assertTrue(find(com.vaadin.flow.component.notification.Notification.class)
                .all().stream().anyMatch(notification -> true));
    }

    @Test
    void deckSlotDialogSavesAndCancels() {
        CampaignSheet.DeckSlotView slot = sheet.players().getFirst().deck().getFirst();
        AtomicInteger saved = new AtomicInteger();
        DeckSlotDialog dialog = new DeckSlotDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", slot, saved::incrementAndGet);
        test(dialog).open();
        test(button(dialog, "Save slot")).click();

        verify(service).updateDeckSlot(eq(UiFixtures.CAMPAIGN_ID), eq("owner"),
                eq(slot.id()), eq("Strike +1"), eq(true), eq("Sharpened"),
                eq(true), eq(true), eq("crossed out"));
        assertEquals(1, saved.get());

        DeckSlotDialog cancelled = new DeckSlotDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", slot, saved::incrementAndGet);
        test(cancelled).open();
        test(button(cancelled, "Cancel")).click();
        assertEquals(1, saved.get());
    }

    @Test
    void purchaseDialogWalksThroughBenefits() {
        AtomicInteger saved = new AtomicInteger();
        PurchaseDialog dialog = new PurchaseDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", sheet.players(), sheet.totals(), saved::incrementAndGet);
        test(dialog).open();

        List<com.vaadin.flow.component.combobox.ComboBox> combos =
                find(com.vaadin.flow.component.combobox.ComboBox.class, dialog).all();
        combos.get(1).setValue(Benefit.KEEP_STRIKE_UPGRADE);
        combos.get(0).setValue(sheet.players().get(1));
        combos.get(1).setValue(Benefit.MASTER_LEVEL1);
        combos.get(1).setValue(Benefit.ELIMINATE_STARTING);

        test(button(dialog, "Buy benefit")).click();

        verify(service, atLeastOnce()).addPurchase(any(), any(), any(), any(), any(),
                any(), any(), any(), any());
        assertEquals(1, saved.get());

        PurchaseDialog cancelled = new PurchaseDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", sheet.players(), sheet.totals(), saved::incrementAndGet);
        test(cancelled).open();
        test(button(cancelled, "Cancel")).click();
        assertEquals(1, saved.get());
    }

    @Test
    void purchaseDialogSaveErrorSurfaces() {
        doThrow(new IllegalArgumentException("Too expensive")).when(service)
                .addPurchase(any(), any(), any(), any(), any(), any(), any(), any(), any());
        PurchaseDialog dialog = new PurchaseDialog(service, UiFixtures.CAMPAIGN_ID,
                "owner", sheet.players(), sheet.totals(), () -> {
                });
        test(dialog).open();
        test(button(dialog, "Buy benefit")).click();

        assertTrue(find(com.vaadin.flow.component.notification.Notification.class)
                .all().stream().anyMatch(notification -> true));
    }
}
