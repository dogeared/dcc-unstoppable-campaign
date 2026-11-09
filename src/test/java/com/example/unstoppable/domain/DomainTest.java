package com.example.unstoppable.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {

    private final Campaign campaign = new Campaign("owner-1", "Party", Difficulty.NORMAL);

    @Test
    void campaignLifecycle() {
        assertEquals("owner-1", campaign.getOwnerSub());
        assertEquals("Party", campaign.getPartyName());
        assertEquals(Difficulty.NORMAL, campaign.getDifficulty());
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
        assertNotNull(campaign.getId());
        assertNotNull(campaign.getCreatedAt());
        assertNotNull(campaign.getUpdatedAt());

        campaign.updateParty("Renamed", Difficulty.HARD);
        assertEquals("Renamed", campaign.getPartyName());
        assertEquals(Difficulty.HARD, campaign.getDifficulty());

        campaign.complete();
        assertEquals(CampaignStatus.COMPLETED, campaign.getStatus());
        campaign.reopen();
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());

        Instant before = campaign.getUpdatedAt();
        campaign.touch();
        assertFalse(campaign.getUpdatedAt().isBefore(before));
        assertEquals(0L, campaign.getVersion());
    }

    @Test
    void participantUpdatesProfile() {
        Participant participant = new Participant(campaign, 1);
        assertNotNull(participant.getId());
        assertSame(campaign, participant.getCampaign());
        assertEquals(1, participant.getSeat());
        assertEquals(0, participant.getStartingHealth());

        participant.update("Carl", "Human", "Crawler", 20, 10, 5,
                "Signature A", "Signature B");
        assertEquals("Carl", participant.getCrawler());
        assertEquals("Human", participant.getRace());
        assertEquals("Crawler", participant.getCrawlerClass());
        assertEquals(20, participant.getStartingHealth());
        assertEquals(10, participant.getStartingMana());
        assertEquals(5, participant.getStartingGold());
        assertEquals("Signature A", participant.getSignatureLevel1());
        assertEquals("Signature B", participant.getSignatureLevel2());
    }

    @Test
    void deckEntryStartsFromBaseCardAndCanBeUpdated() {
        Participant participant = new Participant(campaign, 1);
        DeckEntry entry = new DeckEntry(participant, 1, "Strike");
        assertNotNull(entry.getId());
        assertSame(participant, entry.getParticipant());
        assertEquals(1, entry.getPosition());
        assertEquals("Strike", entry.getBaseCard());
        assertEquals("Strike", entry.getCardName());
        assertFalse(entry.isEliminated());
        assertFalse(entry.isMastered());
        assertFalse(entry.isExtraStartingHand());

        entry.update("Strike +1", true, "Sharpened", true, true, "notes");
        assertEquals("Strike +1", entry.getCardName());
        assertTrue(entry.isEliminated());
        assertTrue(entry.isMastered());
        assertTrue(entry.isExtraStartingHand());
        assertEquals("Sharpened", entry.getUpgradeName());
        assertEquals("notes", entry.getNotes());

        entry.markEliminated(false);
        entry.markMastered(false);
        entry.markExtraStartingHand(false);
        entry.setUpgradeName(null);
        assertFalse(entry.isEliminated());
        assertFalse(entry.isMastered());
        assertFalse(entry.isExtraStartingHand());
        assertNull(entry.getUpgradeName());
    }

    @Test
    void floorProgressTracksBossXpAndCompletion() {
        FloorProgress progress = new FloorProgress(campaign, 2);
        assertNotNull(progress.getId());
        assertSame(campaign, progress.getCampaign());
        assertEquals(2, progress.getFloorNumber());
        assertEquals(0, progress.getBossQuestXp());
        assertNull(progress.getCompletedAt());

        progress.updateBossQuestXp(40, "boss down");
        assertEquals(40, progress.getBossQuestXp());
        assertEquals("boss down", progress.getNotes());

        LocalDate day = LocalDate.of(2026, 1, 2);
        progress.complete(day);
        assertEquals(day, progress.getCompletedAt());
        progress.reopen();
        assertNull(progress.getCompletedAt());
    }

    @Test
    void attemptAndAchievementTrackTheirFields() {
        LocalDate day = LocalDate.of(2026, 2, 3);
        FloorAttempt attempt = new FloorAttempt(campaign, 3, day, false, "close");
        assertNotNull(attempt.getId());
        assertSame(campaign, attempt.getCampaign());
        assertEquals(3, attempt.getFloorNumber());
        assertEquals(day, attempt.getPlayedAt());
        assertFalse(attempt.isSuccessful());
        assertEquals("close", attempt.getNotes());

        attempt.update(day.plusDays(1), true, "cleared");
        assertTrue(attempt.isSuccessful());
        assertEquals("cleared", attempt.getNotes());

        Achievement achievement = new Achievement(campaign, 4, "First blood", 10, day);
        assertNotNull(achievement.getId());
        assertSame(campaign, achievement.getCampaign());
        assertEquals(4, achievement.getFloorNumber());
        assertEquals("First blood", achievement.getName());
        assertEquals(10, achievement.getXp());
        assertEquals(day, achievement.getCompletedAt());

        achievement.update(5, "Second blood", 20, day.plusDays(2));
        assertEquals(5, achievement.getFloorNumber());
        assertEquals("Second blood", achievement.getName());
        assertEquals(20, achievement.getXp());
    }

    @Test
    void purchaseExposesItsSnapshot() {
        Participant participant = new Participant(campaign, 1);
        DeckEntry target = new DeckEntry(participant, 1, "Strike");
        Purchase purchase = new Purchase(campaign, participant, Benefit.MASTER_LEVEL1,
                4, 2, target, "Strike", "Sharp", "notes");
        assertNotNull(purchase.getId());
        assertSame(campaign, purchase.getCampaign());
        assertSame(participant, purchase.getParticipant());
        assertEquals(Benefit.MASTER_LEVEL1, purchase.getBenefit());
        assertEquals(4, purchase.getCost());
        assertEquals(2, purchase.getXValue());
        assertSame(target, purchase.getTargetDeckEntry());
        assertEquals("Strike", purchase.getTargetCard());
        assertEquals("Sharp", purchase.getUpgradeName());
        assertEquals("notes", purchase.getNotes());
        assertNotNull(purchase.getPurchasedAt());
    }

    @Test
    void inviteUsabilityAndRevocation() {
        Instant now = Instant.now();
        CampaignInvite forever = new CampaignInvite(campaign, "hash", AccessRole.VIEWER,
                "owner", null);
        assertNotNull(forever.getId());
        assertSame(campaign, forever.getCampaign());
        assertEquals("hash", forever.getTokenHash());
        assertEquals(AccessRole.VIEWER, forever.getRole());
        assertEquals("owner", forever.getCreatedBySub());
        assertNotNull(forever.getCreatedAt());
        assertNull(forever.getExpiresAt());
        assertNull(forever.getRevokedAt());
        assertTrue(forever.isUsable(now));

        CampaignInvite expired = new CampaignInvite(campaign, "hash2", AccessRole.EDITOR,
                "owner", now.minusSeconds(1));
        assertFalse(expired.isUsable(now));

        CampaignInvite revoked = new CampaignInvite(campaign, "hash3", AccessRole.EDITOR,
                "owner", null);
        revoked.revoke();
        assertNotNull(revoked.getRevokedAt());
        assertFalse(revoked.isUsable(now));
    }

    @Test
    void membershipElevatesViewerToEditorOnly() {
        CampaignMembership membership = new CampaignMembership(campaign, "sub", AccessRole.VIEWER);
        assertNotNull(membership.getId());
        assertSame(campaign, membership.getCampaign());
        assertEquals("sub", membership.getSubject());
        assertEquals(AccessRole.VIEWER, membership.getRole());
        assertNotNull(membership.getCreatedAt());

        membership.elevate(AccessRole.OWNER);
        assertEquals(AccessRole.VIEWER, membership.getRole());
        membership.elevate(AccessRole.EDITOR);
        assertEquals(AccessRole.EDITOR, membership.getRole());
        membership.elevate(AccessRole.EDITOR);
        assertEquals(AccessRole.EDITOR, membership.getRole());
    }

    @Test
    void enumsExposeTheirRules() {
        assertEquals("Eliminate a starting card", Benefit.ELIMINATE_STARTING.label());
        assertEquals(5, Benefit.ELIMINATE_STARTING.cost(null));
        assertEquals(1, Benefit.MASTER_LEVEL1.perPlayerLimit());
        assertEquals(2, Benefit.EXTRA_STARTING_HAND.perPlayerLimit());
        assertTrue(Benefit.KEEP_STRIKE_UPGRADE.unlimited());
        assertTrue(Benefit.KEEP_STRIKE_UPGRADE.variableCost());
        assertTrue(Benefit.KEEP_STRIKE_UPGRADE.requiresDeckSlot());
        assertFalse(Benefit.MASTER_LEVEL1.requiresDeckSlot());
        assertEquals(6, Benefit.KEEP_STRIKE_UPGRADE.cost(5));
        assertEquals(8, Benefit.KEEP_CHARACTER_UPGRADE.cost(5));
        assertEquals("X + 1", Benefit.KEEP_STRIKE_UPGRADE.costLabel());
        assertEquals("X + 3", Benefit.KEEP_CHARACTER_UPGRADE.costLabel());
        assertEquals("4", Benefit.MASTER_LEVEL1.costLabel());
        assertThrows(IllegalArgumentException.class,
                () -> Benefit.KEEP_STRIKE_UPGRADE.cost(null));
        assertThrows(IllegalArgumentException.class,
                () -> Benefit.KEEP_CHARACTER_UPGRADE.cost(-1));

        assertEquals("Normal", Difficulty.NORMAL.label());
        assertEquals(1, Difficulty.NORMAL.scoreBonus());
        assertEquals(20, Difficulty.UNBEATABLE.scoreBonus());
        assertTrue(AccessRole.OWNER.canEdit());
        assertTrue(AccessRole.EDITOR.canEdit());
        assertFalse(AccessRole.VIEWER.canEdit());
    }

    @Test
    void idsAreUnique() {
        assertFalse(campaign.getId().equals(UUID.randomUUID()));
        assertFalse(new Participant(campaign, 1).getId()
                .equals(new Participant(campaign, 2).getId()));
    }
}
