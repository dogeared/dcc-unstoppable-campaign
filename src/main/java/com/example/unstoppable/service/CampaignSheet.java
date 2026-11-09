package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Benefit;
import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.domain.Difficulty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Fully materialised read model for the sheet and editor. UI code never touches
 * lazy JPA associations.
 */
public record CampaignSheet(
        UUID id,
        String partyName,
        Difficulty difficulty,
        CampaignStatus status,
        AccessRole role,
        List<PlayerView> players,
        List<FloorView> floors,
        List<PurchaseView> purchases,
        CampaignTotals totals) {

    public boolean canEdit() {
        return role.canEdit();
    }

    public record PlayerView(
            UUID id,
            int seat,
            String crawler,
            String race,
            String crawlerClass,
            int startingHealth,
            int startingMana,
            int startingGold,
            String signatureLevel1,
            String signatureLevel2,
            List<DeckSlotView> deck) {
    }

    public record DeckSlotView(
            UUID id,
            int position,
            String baseCard,
            String cardName,
            boolean eliminated,
            String upgradeName,
            boolean extraStartingHand,
            boolean mastered,
            String notes) {
    }

    public record FloorView(
            int floorNumber,
            int floorScore,
            int defeats,
            int bossQuestXp,
            boolean completed,
            LocalDate completedAt,
            String notes,
            List<AttemptView> attempts,
            List<AchievementView> achievements) {
    }

    public record AttemptView(
            UUID id,
            LocalDate playedAt,
            boolean successful,
            String notes) {
    }

    public record AchievementView(
            UUID id,
            int floorNumber,
            String name,
            int xp,
            LocalDate completedAt) {
    }

    public record PurchaseView(
            UUID id,
            UUID participantId,
            int seat,
            String crawler,
            Benefit benefit,
            int cost,
            Integer xValue,
            String targetCard,
            String upgradeName,
            String notes,
            Instant purchasedAt) {
    }
}
