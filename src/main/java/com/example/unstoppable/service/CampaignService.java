package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.Achievement;
import com.example.unstoppable.domain.Benefit;
import com.example.unstoppable.domain.Campaign;
import com.example.unstoppable.domain.CampaignMembership;
import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.domain.DeckEntry;
import com.example.unstoppable.domain.Difficulty;
import com.example.unstoppable.domain.FloorAttempt;
import com.example.unstoppable.domain.FloorProgress;
import com.example.unstoppable.domain.Participant;
import com.example.unstoppable.domain.Purchase;
import com.example.unstoppable.repository.AchievementRepository;
import com.example.unstoppable.repository.CampaignMembershipRepository;
import com.example.unstoppable.repository.CampaignRepository;
import com.example.unstoppable.repository.DeckEntryRepository;
import com.example.unstoppable.repository.FloorAttemptRepository;
import com.example.unstoppable.repository.FloorProgressRepository;
import com.example.unstoppable.repository.ParticipantRepository;
import com.example.unstoppable.repository.PurchaseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    public static final int FLOOR_COUNT = 9;

    private static final List<String> DECK_BLUEPRINT = List.of(
            "Strike", "Strike", "Strike", "Strike", "Strike", "Strike",
            "New Achievement!", "Rest",
            "Character Card A", "Character Card B", "Character Card C",
            "???", "???", "???");

    private final CampaignRepository campaigns;
    private final CampaignMembershipRepository memberships;
    private final ParticipantRepository participants;
    private final DeckEntryRepository deckEntries;
    private final FloorProgressRepository floorProgress;
    private final FloorAttemptRepository attempts;
    private final AchievementRepository achievements;
    private final PurchaseRepository purchases;
    private final CampaignAccessService access;

    public CampaignService(CampaignRepository campaigns,
            CampaignMembershipRepository memberships,
            ParticipantRepository participants,
            DeckEntryRepository deckEntries,
            FloorProgressRepository floorProgress,
            FloorAttemptRepository attempts,
            AchievementRepository achievements,
            PurchaseRepository purchases,
            CampaignAccessService access) {
        this.campaigns = campaigns;
        this.memberships = memberships;
        this.participants = participants;
        this.deckEntries = deckEntries;
        this.floorProgress = floorProgress;
        this.attempts = attempts;
        this.achievements = achievements;
        this.purchases = purchases;
        this.access = access;
    }

    // ---- campaign lifecycle -------------------------------------------------

    @Transactional
    public UUID create(String subject, String partyName, Difficulty difficulty) {
        requireText(partyName, "Party name");
        Campaign campaign = new Campaign(subject, partyName.trim(), difficulty);
        campaigns.save(campaign);

        for (int seat = 1; seat <= 2; seat++) {
            Participant participant = new Participant(campaign, seat);
            participants.save(participant);
            int position = 1;
            for (String baseCard : DECK_BLUEPRINT) {
                deckEntries.save(new DeckEntry(participant, position++, baseCard));
            }
        }
        for (int floor = 1; floor <= FLOOR_COUNT; floor++) {
            floorProgress.save(new FloorProgress(campaign, floor));
        }
        return campaign.getId();
    }

    @Transactional
    public void updateParty(UUID campaignId, String subject, String partyName,
            Difficulty difficulty) {
        requireText(partyName, "Party name");
        CampaignAccess campaignAccess = access.requireEditable(campaignId, subject);
        campaignAccess.campaign().updateParty(partyName.trim(), difficulty);
    }

    @Transactional
    public void completeCampaign(UUID campaignId, String subject) {
        access.requireOwner(campaignId, subject).campaign().complete();
    }

    @Transactional
    public void reopenCampaign(UUID campaignId, String subject) {
        access.requireOwner(campaignId, subject).campaign().reopen();
    }

    /**
     * Removes a campaign shared with the user from their list. Only viewer and
     * editor memberships can be removed; owners keep their own campaigns.
     */
    @Transactional
    public void removeSharedCampaign(UUID campaignId, String subject) {
        CampaignAccess campaignAccess = access.requireReadable(campaignId, subject);
        if (campaignAccess.role() == AccessRole.OWNER) {
            throw new IllegalArgumentException(
                    "Owners cannot remove their own campaign from the list");
        }
        memberships.findByCampaignIdAndSubject(campaignId, subject)
                .ifPresent(memberships::delete);
    }

    @Transactional(readOnly = true)
    public List<CampaignSummary> listFor(String subject) {
        Map<UUID, AccessRole> roles = new LinkedHashMap<>();
        for (Campaign campaign : campaigns.findByOwnerSubOrderByUpdatedAtDesc(subject)) {
            roles.put(campaign.getId(), AccessRole.OWNER);
        }
        for (CampaignMembership membership : memberships.findBySubject(subject)) {
            roles.putIfAbsent(membership.getCampaign().getId(), membership.getRole());
        }

        List<CampaignSummary> result = new ArrayList<>();
        for (Map.Entry<UUID, AccessRole> entry : roles.entrySet()) {
            campaigns.findById(entry.getKey()).ifPresent(campaign -> {
                CampaignTotals totals = totalsFor(campaign.getId(), campaign.getDifficulty());
                long completed = floorProgress.findByCampaignIdOrderByFloorNumber(campaign.getId())
                        .stream().filter(f -> f.getCompletedAt() != null).count();
                result.add(new CampaignSummary(campaign.getId(), campaign.getPartyName(),
                        campaign.getDifficulty(), campaign.getStatus(), entry.getValue(),
                        (int) completed, totals.finalScore(), totals.availableXp(),
                        campaign.getUpdatedAt()));
            });
        }
        result.sort(Comparator.comparing(CampaignSummary::updatedAt).reversed());
        return result;
    }

    @Transactional(readOnly = true)
    public CampaignSheet load(UUID campaignId, String subject) {
        CampaignAccess campaignAccess = access.requireReadable(campaignId, subject);
        Campaign campaign = campaignAccess.campaign();

        List<Participant> playerEntities =
                participants.findByCampaignIdOrderBySeat(campaignId);
        Map<UUID, Participant> playersById = playerEntities.stream()
                .collect(Collectors.toMap(Participant::getId, p -> p));

        List<CampaignSheet.PlayerView> players = new ArrayList<>();
        for (Participant participant : playerEntities) {
            List<CampaignSheet.DeckSlotView> deck =
                    deckEntries.findByParticipantIdOrderByPosition(participant.getId())
                            .stream()
                            .map(slot -> new CampaignSheet.DeckSlotView(slot.getId(),
                                    slot.getPosition(), slot.getBaseCard(),
                                    slot.getCardName(), slot.isEliminated(),
                                    slot.getUpgradeName(), slot.isExtraStartingHand(),
                                    slot.isMastered(), slot.getNotes()))
                            .toList();
            players.add(new CampaignSheet.PlayerView(participant.getId(),
                    participant.getSeat(), participant.getCrawler(),
                    participant.getRace(), participant.getCrawlerClass(),
                    participant.getStartingHealth(), participant.getStartingMana(),
                    participant.getStartingGold(), participant.getSignatureLevel1(),
                    participant.getSignatureLevel2(), deck));
        }

        Map<Integer, List<FloorAttempt>> attemptsByFloor = attempts
                .findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(campaignId).stream()
                .collect(Collectors.groupingBy(FloorAttempt::getFloorNumber));
        Map<Integer, List<Achievement>> achievementsByFloor = achievements
                .findByCampaignIdOrderByFloorNumberAscNameAsc(campaignId).stream()
                .collect(Collectors.groupingBy(Achievement::getFloorNumber));

        List<CampaignSheet.FloorView> floors = new ArrayList<>();
        for (FloorProgress progress : floorProgress
                .findByCampaignIdOrderByFloorNumber(campaignId)) {
            int floor = progress.getFloorNumber();
            List<Achievement> floorAchievements =
                    achievementsByFloor.getOrDefault(floor, List.of());
            List<FloorAttempt> floorAttempts =
                    attemptsByFloor.getOrDefault(floor, List.of());
            int achievementXp = floorAchievements.stream()
                    .mapToInt(Achievement::getXp).sum();
            int defeats = (int) floorAttempts.stream()
                    .filter(a -> !a.isSuccessful()).count();
            List<CampaignSheet.AttemptView> attemptViews = floorAttempts.stream()
                    .sorted(Comparator.comparing(FloorAttempt::getPlayedAt).reversed())
                    .map(a -> new CampaignSheet.AttemptView(a.getId(), a.getPlayedAt(),
                            a.isSuccessful(), a.getNotes()))
                    .toList();
            List<CampaignSheet.AchievementView> achievementViews = floorAchievements
                    .stream()
                    .map(a -> new CampaignSheet.AchievementView(a.getId(),
                            a.getFloorNumber(), a.getName(), a.getXp(),
                            a.getCompletedAt()))
                    .toList();
            floors.add(new CampaignSheet.FloorView(floor,
                    achievementXp + progress.getBossQuestXp(), defeats,
                    progress.getBossQuestXp(), progress.getCompletedAt() != null,
                    progress.getCompletedAt(), progress.getNotes(),
                    attemptViews, achievementViews));
        }

        List<CampaignSheet.PurchaseView> purchaseViews = purchases
                .findByCampaignIdOrderByPurchasedAtDesc(campaignId).stream()
                .map(p -> {
                    Participant participant = playersById.get(p.getParticipant().getId());
                    String targetCard = p.getTargetCard();
                    DeckEntry target = p.getTargetDeckEntry();
                    if ((targetCard == null || targetCard.isBlank()) && target != null) {
                        targetCard = "#" + target.getPosition() + " · " + target.getBaseCard();
                    }
                    return new CampaignSheet.PurchaseView(p.getId(),
                            p.getParticipant().getId(), participant == null ? 0 : participant.getSeat(),
                            participant == null ? "" : participant.getCrawler(),
                            p.getBenefit(), p.getCost(), p.getXValue(),
                            targetCard, p.getUpgradeName(), p.getNotes(),
                            p.getPurchasedAt());
                })
                .toList();

        int floorTotal = floors.stream().mapToInt(CampaignSheet.FloorView::floorScore).sum();
        int defeatTotal = floors.stream().mapToInt(CampaignSheet.FloorView::defeats).sum();
        int spent = purchaseViews.stream().mapToInt(CampaignSheet.PurchaseView::cost).sum();
        CampaignTotals totals = CampaignTotals.of(floorTotal, defeatTotal, spent,
                campaign.getDifficulty());

        return new CampaignSheet(campaign.getId(), campaign.getPartyName(),
                campaign.getDifficulty(), campaign.getStatus(), campaignAccess.role(),
                players, floors, purchaseViews, totals);
    }

    // ---- players and decks --------------------------------------------------

    @Transactional
    public void updatePlayer(UUID campaignId, String subject, int seat, String crawler,
            String race, String crawlerClass, int startingHealth, int startingMana,
            int startingGold, String signatureLevel1, String signatureLevel2) {
        requireEditable(campaignId, subject);
        Participant participant = participants.findByCampaignIdAndSeat(campaignId, seat)
                .orElseThrow(() -> new IllegalArgumentException("No player in seat " + seat));
        if (startingHealth < 0 || startingMana < 0 || startingGold < 0) {
            throw new IllegalArgumentException("Starting stats cannot be negative");
        }
        participant.update(trim(crawler), trim(race), trim(crawlerClass),
                startingHealth, startingMana, startingGold,
                trim(signatureLevel1), trim(signatureLevel2));
    }

    @Transactional
    public void updateDeckSlot(UUID campaignId, String subject, UUID slotId,
            String cardName, boolean eliminated, String upgradeName,
            boolean extraStartingHand, boolean mastered, String notes) {
        requireEditable(campaignId, subject);
        DeckEntry slot = requireSlot(campaignId, slotId);
        slot.update(trim(cardName), eliminated, trim(upgradeName),
                extraStartingHand, mastered, trim(notes));
    }

    // ---- floors and attempts ------------------------------------------------

    @Transactional
    public void updateFloor(UUID campaignId, String subject, int floorNumber,
            int bossQuestXp, String notes) {
        requireEditable(campaignId, subject);
        if (bossQuestXp < 0) {
            throw new IllegalArgumentException("Boss and quest XP cannot be negative");
        }
        FloorProgress progress = requireFloor(campaignId, floorNumber);
        progress.updateBossQuestXp(bossQuestXp, trim(notes));
    }

    @Transactional
    public UUID addAttempt(UUID campaignId, String subject, int floorNumber,
            LocalDate playedAt, boolean successful, String notes) {
        requireEditable(campaignId, subject);
        requireFloorNumber(floorNumber);
        Campaign campaign = requireCampaign(campaignId);
        FloorAttempt attempt = attempts.save(new FloorAttempt(campaign, floorNumber,
                playedAt == null ? LocalDate.now() : playedAt, successful, trim(notes)));
        refreshFloorCompletion(campaignId, floorNumber);
        return attempt.getId();
    }

    @Transactional
    public void updateAttempt(UUID campaignId, String subject, UUID attemptId,
            LocalDate playedAt, boolean successful, String notes) {
        requireEditable(campaignId, subject);
        FloorAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        requireCampaignMatch(campaignId, attempt.getCampaign().getId());
        attempt.update(playedAt == null ? LocalDate.now() : playedAt, successful,
                trim(notes));
        refreshFloorCompletion(campaignId, attempt.getFloorNumber());
    }

    @Transactional
    public void deleteAttempt(UUID campaignId, String subject, UUID attemptId) {
        requireEditable(campaignId, subject);
        FloorAttempt attempt = attempts.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found"));
        requireCampaignMatch(campaignId, attempt.getCampaign().getId());
        int floor = attempt.getFloorNumber();
        attempts.delete(attempt);
        attempts.flush();
        refreshFloorCompletion(campaignId, floor);
    }

    private void refreshFloorCompletion(UUID campaignId, int floorNumber) {
        FloorProgress progress = requireFloor(campaignId, floorNumber);
        List<FloorAttempt> floorAttempts = attempts
                .findByCampaignIdAndFloorNumberOrderByPlayedAtDesc(campaignId, floorNumber);
        LocalDate lastSuccess = floorAttempts.stream()
                .filter(FloorAttempt::isSuccessful)
                .map(FloorAttempt::getPlayedAt)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (lastSuccess == null) {
            progress.reopen();
        } else {
            progress.complete(lastSuccess);
        }
    }

    // ---- achievements -------------------------------------------------------

    @Transactional
    public UUID addAchievement(UUID campaignId, String subject, int floorNumber,
            String name, int xp, LocalDate completedAt) {
        requireEditable(campaignId, subject);
        requireFloorNumber(floorNumber);
        requireText(name, "Achievement name");
        if (xp < 0) {
            throw new IllegalArgumentException("XP cannot be negative");
        }
        String cleanName = name.trim();
        achievements.findByCampaignIdAndNameIgnoreCase(campaignId, cleanName)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "That achievement is already recorded: " + existing.getName());
                });
        Achievement achievement = achievements.save(new Achievement(
                requireCampaign(campaignId), floorNumber, cleanName, xp,
                completedAt == null ? LocalDate.now() : completedAt));
        return achievement.getId();
    }

    @Transactional
    public void updateAchievement(UUID campaignId, String subject, UUID achievementId,
            int floorNumber, String name, int xp, LocalDate completedAt) {
        requireEditable(campaignId, subject);
        requireFloorNumber(floorNumber);
        requireText(name, "Achievement name");
        if (xp < 0) {
            throw new IllegalArgumentException("XP cannot be negative");
        }
        Achievement achievement = achievements.findById(achievementId)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found"));
        requireCampaignMatch(campaignId, achievement.getCampaign().getId());
        String cleanName = name.trim();
        achievements.findByCampaignIdAndNameIgnoreCase(campaignId, cleanName)
                .filter(other -> !other.getId().equals(achievementId))
                .ifPresent(other -> {
                    throw new IllegalArgumentException(
                            "That achievement is already recorded: " + other.getName());
                });
        achievement.update(floorNumber, cleanName, xp,
                completedAt == null ? LocalDate.now() : completedAt);
    }

    @Transactional
    public void deleteAchievement(UUID campaignId, String subject, UUID achievementId) {
        requireEditable(campaignId, subject);
        Achievement achievement = achievements.findById(achievementId)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found"));
        requireCampaignMatch(campaignId, achievement.getCampaign().getId());
        achievements.delete(achievement);
    }

    @Transactional(readOnly = true)
    public List<String> achievementNames(UUID campaignId, String subject) {
        access.requireReadable(campaignId, subject);
        return achievements.findDistinctNamesForCampaign(campaignId);
    }

    // ---- purchases ----------------------------------------------------------

    @Transactional
    public UUID addPurchase(UUID campaignId, String subject, UUID participantId,
            Benefit benefit, Integer xValue, UUID targetDeckEntryId, String targetCard,
            String upgradeName, String notes) {
        requireEditable(campaignId, subject);
        Campaign campaign = requireCampaign(campaignId);
        Participant participant = participants.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        requireCampaignMatch(campaignId, participant.getCampaign().getId());

        if (!benefit.unlimited()) {
            long count = purchases.countByParticipantIdAndBenefit(participantId, benefit);
            if (count >= benefit.perPlayerLimit()) {
                throw new IllegalArgumentException(benefit.label()
                        + " has already been purchased the maximum number of times");
            }
        }
        if (benefit.variableCost() && (xValue == null || xValue < 0)) {
            throw new IllegalArgumentException(
                    "Enter the upgrade's gold cost X (zero or greater)");
        }

        DeckEntry target = null;
        if (targetDeckEntryId != null) {
            target = requireSlot(campaignId, targetDeckEntryId);
            if (!target.getParticipant().getId().equals(participantId)) {
                throw new IllegalArgumentException(
                        "The target card belongs to the other player");
            }
        }
        if (benefit.requiresDeckSlot() && target == null) {
            throw new IllegalArgumentException("Choose a deck slot for this benefit");
        }
        if (benefit == Benefit.KEEP_STRIKE_UPGRADE && target != null) {
            if (!List.of("Strike", "New Achievement!", "Rest")
                    .contains(target.getBaseCard())) {
                throw new IllegalArgumentException(
                        "That benefit only applies to Strike, New Achievement! or Rest");
            }
        }
        if (benefit == Benefit.KEEP_CHARACTER_UPGRADE && target != null) {
            if (!target.getBaseCard().startsWith("Character Card")
                    && !target.isMastered()) {
                throw new IllegalArgumentException(
                        "That benefit only applies to a character card or a mastered card");
            }
        }
        if ((benefit == Benefit.MASTER_LEVEL1 || benefit == Benefit.MASTER_LEVEL2)
                && isBlank(targetCard) && target != null) {
            targetCard = target.getCardName();
        }
        if ((benefit == Benefit.MASTER_LEVEL1 || benefit == Benefit.MASTER_LEVEL2)
                && isBlank(targetCard)) {
            throw new IllegalArgumentException("Name the card being mastered");
        }
        if (benefit.variableCost() && isBlank(upgradeName)) {
            throw new IllegalArgumentException("Name the upgrade being kept");
        }

        int cost = benefit.cost(xValue);
        CampaignTotals totals = totalsFor(campaignId, campaign.getDifficulty());
        if (cost > totals.availableXp()) {
            throw new IllegalArgumentException("Not enough available XP: "
                    + totals.availableXp() + " available, " + cost + " required");
        }

        Purchase purchase = purchases.save(new Purchase(campaign, participant, benefit,
                cost, xValue, target, trim(targetCard), trim(upgradeName), trim(notes)));

        if (target != null) {
            switch (benefit) {
                case ELIMINATE_STARTING -> target.markEliminated(true);
                case MASTER_LEVEL1, MASTER_LEVEL2 -> target.markMastered(true);
                case EXTRA_STARTING_HAND -> target.markExtraStartingHand(true);
                case KEEP_STRIKE_UPGRADE, KEEP_CHARACTER_UPGRADE ->
                        target.setUpgradeName(trim(upgradeName));
            }
        }
        campaign.touch();
        return purchase.getId();
    }

    @Transactional
    public void deletePurchase(UUID campaignId, String subject, UUID purchaseId) {
        requireEditable(campaignId, subject);
        Purchase purchase = purchases.findById(purchaseId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));
        requireCampaignMatch(campaignId, purchase.getCampaign().getId());
        purchases.delete(purchase);
        purchase.getCampaign().touch();
    }

    // ---- helpers ------------------------------------------------------------

    private CampaignTotals totalsFor(UUID campaignId, Difficulty difficulty) {
        int achievementXp = achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaignId)
                .stream().mapToInt(Achievement::getXp).sum();
        int bossXp = floorProgress.findByCampaignIdOrderByFloorNumber(campaignId).stream()
                .mapToInt(FloorProgress::getBossQuestXp).sum();
        int defeats = (int) attempts
                .findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(campaignId).stream()
                .filter(a -> !a.isSuccessful()).count();
        int spent = purchases.findByCampaignIdOrderByPurchasedAtDesc(campaignId).stream()
                .mapToInt(Purchase::getCost).sum();
        return CampaignTotals.of(achievementXp + bossXp, defeats, spent, difficulty);
    }

    private CampaignAccess requireEditable(UUID campaignId, String subject) {
        return access.requireEditable(campaignId, subject);
    }

    private Campaign requireCampaign(UUID campaignId) {
        return campaigns.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));
    }

    private FloorProgress requireFloor(UUID campaignId, int floorNumber) {
        requireFloorNumber(floorNumber);
        return floorProgress.findByCampaignIdAndFloorNumber(campaignId, floorNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Floor " + floorNumber + " does not exist"));
    }

    private DeckEntry requireSlot(UUID campaignId, UUID slotId) {
        DeckEntry slot = deckEntries.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Deck slot not found"));
        requireCampaignMatch(campaignId, slot.getParticipant().getCampaign().getId());
        return slot;
    }

    private static void requireCampaignMatch(UUID expected, UUID actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AccessDeniedException("That record belongs to another campaign");
        }
    }

    private static void requireFloorNumber(int floorNumber) {
        if (floorNumber < 1 || floorNumber > FLOOR_COUNT) {
            throw new IllegalArgumentException("Floor must be between 1 and " + FLOOR_COUNT);
        }
    }

    private static void requireText(String value, String label) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
