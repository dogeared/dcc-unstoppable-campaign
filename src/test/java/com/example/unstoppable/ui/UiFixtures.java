package com.example.unstoppable.ui;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Benefit;
import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.domain.Difficulty;
import com.example.unstoppable.service.CampaignSheet;
import com.example.unstoppable.service.CampaignSummary;
import com.example.unstoppable.service.CampaignTotals;
import com.example.unstoppable.service.InviteView;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Read-model fixtures shared by the browserless Vaadin view tests.
 */
public final class UiFixtures {

    public static final UUID CAMPAIGN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private UiFixtures() {
    }

    public static CampaignTotals totals() {
        return CampaignTotals.of(40, 1, 9, Difficulty.NORMAL);
    }

    public static CampaignSheet richSheet(AccessRole role) {
        CampaignSheet.DeckSlotView strike = new CampaignSheet.DeckSlotView(
                UUID.randomUUID(), 1, "Strike", "Strike +1", true, "Sharpened",
                true, true, "crossed out");
        CampaignSheet.DeckSlotView rest = new CampaignSheet.DeckSlotView(
                UUID.randomUUID(), 2, "Rest", null, false, null, false, false, null);
        CampaignSheet.DeckSlotView mastered = new CampaignSheet.DeckSlotView(
                UUID.randomUUID(), 3, "Character Card A", "Character Card A",
                false, null, false, true, null);

        CampaignSheet.PlayerView player1 = new CampaignSheet.PlayerView(
                UUID.randomUUID(), 1, "Carl", "Human", "Crawler", 20, 10, 5,
                "Signature A", "Signature B", List.of(strike, rest, mastered));
        CampaignSheet.PlayerView player2 = new CampaignSheet.PlayerView(
                UUID.randomUUID(), 2, " ", "", "", 15, 5, 0, null, null, List.of());

        CampaignSheet.AttemptView win = new CampaignSheet.AttemptView(
                UUID.randomUUID(), LocalDate.of(2026, 1, 2), true, "cleared it");
        CampaignSheet.AttemptView loss = new CampaignSheet.AttemptView(
                UUID.randomUUID(), LocalDate.of(2026, 1, 1), false, null);
        CampaignSheet.AchievementView achievement = new CampaignSheet.AchievementView(
                UUID.randomUUID(), 1, "First blood", 10, LocalDate.of(2026, 1, 2));

        CampaignSheet.FloorView floor1 = new CampaignSheet.FloorView(
                1, 25, 1, 15, true, LocalDate.of(2026, 1, 2), "boss down",
                List.of(win, loss), List.of(achievement));
        CampaignSheet.FloorView floor2 = new CampaignSheet.FloorView(
                2, 0, 0, 0, false, null, null, List.of(), List.of());

        CampaignSheet.PurchaseView purchase = new CampaignSheet.PurchaseView(
                UUID.randomUUID(), player1.id(), 1, "Carl", Benefit.KEEP_STRIKE_UPGRADE,
                5, 4, "Strike", "Sharpened", "kept", Instant.parse("2026-01-03T12:00:00Z"));
        CampaignSheet.PurchaseView second = new CampaignSheet.PurchaseView(
                UUID.randomUUID(), player2.id(), 2, null, Benefit.ELIMINATE_STARTING,
                5, null, null, null, null, null);

        return new CampaignSheet(CAMPAIGN_ID, "The Brave", Difficulty.NORMAL,
                CampaignStatus.ACTIVE, role, List.of(player1, player2),
                List.of(floor1, floor2), List.of(purchase, second), totals());
    }

    public static CampaignSheet completedSheet(AccessRole role) {
        CampaignSheet rich = richSheet(role);
        return new CampaignSheet(rich.id(), rich.partyName(), rich.difficulty(),
                CampaignStatus.COMPLETED, role, rich.players(), rich.floors(),
                rich.purchases(), rich.totals());
    }

    public static CampaignSheet emptySheet(AccessRole role) {
        CampaignSheet.PlayerView player = new CampaignSheet.PlayerView(
                UUID.randomUUID(), 1, null, null, null, 0, 0, 0,
                null, null, List.of());
        CampaignSheet.FloorView floor = new CampaignSheet.FloorView(
                1, 0, 0, 0, false, null, null, List.of(), List.of());
        return new CampaignSheet(UUID.randomUUID(), "Empty", Difficulty.HARD,
                CampaignStatus.ACTIVE, role, List.of(player), List.of(floor),
                List.of(), CampaignTotals.of(0, 0, 0, Difficulty.HARD));
    }

    public static CampaignSummary summary(AccessRole role, CampaignStatus status) {
        return new CampaignSummary(UUID.randomUUID(), "Party " + role, Difficulty.NORMAL,
                status, role, 3, 42, 17, Instant.now());
    }

    public static InviteView invite(boolean usable) {
        return new InviteView(UUID.randomUUID(), AccessRole.EDITOR,
                Instant.parse("2026-01-01T00:00:00Z"),
                usable ? null : Instant.parse("2026-01-15T00:00:00Z"),
                null, usable);
    }
}
