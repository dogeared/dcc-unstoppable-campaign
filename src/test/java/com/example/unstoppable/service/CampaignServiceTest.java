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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignServiceTest {

    @Mock private CampaignRepository campaigns;
    @Mock private CampaignMembershipRepository memberships;
    @Mock private ParticipantRepository participants;
    @Mock private DeckEntryRepository deckEntries;
    @Mock private FloorProgressRepository floorProgress;
    @Mock private FloorAttemptRepository attempts;
    @Mock private AchievementRepository achievements;
    @Mock private PurchaseRepository purchases;
    @Mock private CampaignAccessService access;

    private CampaignService service;
    private Campaign campaign;
    private Participant p1;
    private Participant p2;

    @BeforeEach
    void setUp() {
        service = new CampaignService(campaigns, memberships, participants, deckEntries,
                floorProgress, attempts, achievements, purchases, access);
        campaign = new Campaign("owner", "Party", Difficulty.NORMAL);
        p1 = new Participant(campaign, 1);
        p2 = new Participant(campaign, 2);

        when(access.requireReadable(any(), any()))
                .thenAnswer(inv -> new CampaignAccess(campaign, AccessRole.OWNER));
        when(access.requireEditable(any(), any()))
                .thenAnswer(inv -> new CampaignAccess(campaign, AccessRole.OWNER));
        when(access.requireOwner(any(), any()))
                .thenAnswer(inv -> new CampaignAccess(campaign, AccessRole.OWNER));
        when(campaigns.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(participants.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deckEntries.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(floorProgress.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(attempts.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(achievements.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(purchases.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void emptyTotals() {
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(any()))
                .thenReturn(List.of());
        when(floorProgress.findByCampaignIdOrderByFloorNumber(any()))
                .thenReturn(List.of());
        when(attempts.findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(any()))
                .thenReturn(List.of());
        when(purchases.findByCampaignIdOrderByPurchasedAtDesc(any()))
                .thenReturn(List.of());
    }

    // ---- lifecycle ----------------------------------------------------------

    @Test
    void createSeedsPlayersDeckAndFloors() {
        UUID id = service.create("owner", "  Party  ", Difficulty.HARD);

        assertNotNull(id);
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaigns).save(campaignCaptor.capture());
        assertEquals("Party", campaignCaptor.getValue().getPartyName());
        assertEquals(Difficulty.HARD, campaignCaptor.getValue().getDifficulty());
        verify(participants, times(2)).save(any(Participant.class));
        verify(deckEntries, times(28)).save(any(DeckEntry.class));
        verify(floorProgress, times(CampaignService.FLOOR_COUNT)).save(any(FloorProgress.class));
    }

    @Test
    void createRejectsBlankPartyName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create("owner", "  ", Difficulty.NORMAL));
        verify(campaigns, never()).save(any());
    }

    @Test
    void updatePartyTrimsAndDelegatesToAccess() {
        service.updateParty(campaign.getId(), "owner", " New ", Difficulty.EXPERT);
        assertEquals("New", campaign.getPartyName());
        assertEquals(Difficulty.EXPERT, campaign.getDifficulty());
    }

    @Test
    void updatePartyRejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateParty(campaign.getId(), "owner", " ", Difficulty.NORMAL));
    }

    @Test
    void completeAndReopenCampaign() {
        service.completeCampaign(campaign.getId(), "owner");
        assertEquals(CampaignStatus.COMPLETED, campaign.getStatus());
        service.reopenCampaign(campaign.getId(), "owner");
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
    }

    // ---- listing ------------------------------------------------------------

    @Test
    void listForMergesOwnedAndMemberCampaigns() {
        Campaign owned = new Campaign("owner", "Owned", Difficulty.NORMAL);
        Campaign shared = new Campaign("other", "Shared", Difficulty.HARD);
        when(campaigns.findByOwnerSubOrderByUpdatedAtDesc("owner")).thenReturn(List.of(owned));
        when(memberships.findBySubject("owner")).thenReturn(List.of(
                new CampaignMembership(shared, "owner", AccessRole.EDITOR),
                new CampaignMembership(owned, "owner", AccessRole.VIEWER)));
        when(campaigns.findById(owned.getId())).thenReturn(Optional.of(owned));
        when(campaigns.findById(shared.getId())).thenReturn(Optional.of(shared));
        when(floorProgress.findByCampaignIdOrderByFloorNumber(any())).thenReturn(List.of());
        emptyTotals();

        List<CampaignSummary> summaries = service.listFor("owner");

        assertEquals(2, summaries.size());
        CampaignSummary sharedSummary = summaries.stream()
                .filter(s -> s.partyName().equals("Shared")).findFirst().orElseThrow();
        assertEquals(AccessRole.EDITOR, sharedSummary.role());
        CampaignSummary ownedSummary = summaries.stream()
                .filter(s -> s.partyName().equals("Owned")).findFirst().orElseThrow();
        assertEquals(AccessRole.OWNER, ownedSummary.role());
    }

    @Test
    void listForCountsCompletedFloorsAndSkipsMissingCampaigns() {
        Campaign owned = new Campaign("owner", "Owned", Difficulty.NORMAL);
        when(campaigns.findByOwnerSubOrderByUpdatedAtDesc("owner")).thenReturn(List.of(owned));
        when(memberships.findBySubject("owner")).thenReturn(List.of(
                new CampaignMembership(owned, "ghost", AccessRole.VIEWER)));
        when(campaigns.findById(owned.getId())).thenReturn(Optional.of(owned));
        when(campaigns.findById(owned.getId())).thenReturn(Optional.of(owned));
        when(floorProgress.findByCampaignIdOrderByFloorNumber(owned.getId()))
                .thenReturn(List.of(new FloorProgress(owned, 1)));
        emptyTotals();

        List<CampaignSummary> summaries = service.listFor("owner");

        assertEquals(1, summaries.size());
        assertEquals(0, summaries.getFirst().floorsCompleted());
    }

    // ---- removing shared campaigns -----------------------------------------

    @Test
    void removeSharedCampaignDeletesViewerMembership() {
        CampaignMembership membership =
                new CampaignMembership(campaign, "viewer", AccessRole.VIEWER);
        when(access.requireReadable(campaign.getId(), "viewer"))
                .thenReturn(new CampaignAccess(campaign, AccessRole.VIEWER));
        when(memberships.findByCampaignIdAndSubject(campaign.getId(), "viewer"))
                .thenReturn(Optional.of(membership));

        service.removeSharedCampaign(campaign.getId(), "viewer");

        verify(memberships).delete(membership);
    }

    @Test
    void removeSharedCampaignDeletesEditorMembership() {
        CampaignMembership membership =
                new CampaignMembership(campaign, "editor", AccessRole.EDITOR);
        when(access.requireReadable(campaign.getId(), "editor"))
                .thenReturn(new CampaignAccess(campaign, AccessRole.EDITOR));
        when(memberships.findByCampaignIdAndSubject(campaign.getId(), "editor"))
                .thenReturn(Optional.of(membership));

        service.removeSharedCampaign(campaign.getId(), "editor");

        verify(memberships).delete(membership);
    }

    @Test
    void removeSharedCampaignRejectsOwners() {
        assertThrows(IllegalArgumentException.class,
                () -> service.removeSharedCampaign(campaign.getId(), "owner"));

        verify(memberships, never()).delete(any());
    }

    @Test
    void removeSharedCampaignRequiresReadableAccess() {
        when(access.requireReadable(campaign.getId(), "stranger"))
                .thenThrow(new AccessDeniedException("nope"));

        assertThrows(AccessDeniedException.class,
                () -> service.removeSharedCampaign(campaign.getId(), "stranger"));

        verify(memberships, never()).delete(any());
    }

    // ---- loading ------------------------------------------------------------

    @Test
    void loadMaterialisesSheetFromRepositories() {
        DeckEntry strike = new DeckEntry(p1, 1, "Strike");
        strike.update("Strike +1", true, "Sharp", true, true, "note");
        DeckEntry character = new DeckEntry(p1, 2, "Character Card A");

        Achievement achievement = new Achievement(campaign, 1, "First", 10,
                LocalDate.of(2026, 1, 1));
        FloorAttempt win = new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 1), true, "won");
        FloorAttempt loss = new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 2), false, "lost");
        FloorProgress floor1 = new FloorProgress(campaign, 1);
        floor1.updateBossQuestXp(5, "boss");
        floor1.complete(LocalDate.of(2026, 1, 3));
        FloorProgress floor2 = new FloorProgress(campaign, 2);

        Purchase purchase = new Purchase(campaign, p1, Benefit.MASTER_LEVEL1, 4, null,
                character, null, null, "mastered");
        Purchase standalone = new Purchase(campaign, p2, Benefit.ELIMINATE_STARTING, 5, null,
                null, "Strike", null, null);

        when(participants.findByCampaignIdOrderBySeat(campaign.getId())).thenReturn(List.of(p1, p2));
        when(deckEntries.findByParticipantIdOrderByPosition(p1.getId()))
                .thenReturn(List.of(strike, character));
        when(deckEntries.findByParticipantIdOrderByPosition(p2.getId())).thenReturn(List.of());
        when(attempts.findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(campaign.getId()))
                .thenReturn(List.of(win, loss));
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of(achievement));
        when(floorProgress.findByCampaignIdOrderByFloorNumber(campaign.getId()))
                .thenReturn(List.of(floor1, floor2));
        when(purchases.findByCampaignIdOrderByPurchasedAtDesc(campaign.getId()))
                .thenReturn(List.of(purchase, standalone));

        CampaignSheet sheet = service.load(campaign.getId(), "owner");

        assertEquals("Party", sheet.partyName());
        assertTrue(sheet.canEdit());
        assertEquals(2, sheet.players().size());
        assertEquals(2, sheet.players().getFirst().deck().size());
        assertTrue(sheet.players().getFirst().deck().getFirst().eliminated());

        CampaignSheet.FloorView first = sheet.floors().getFirst();
        assertEquals(15, first.floorScore());
        assertEquals(1, first.defeats());
        assertTrue(first.completed());
        assertEquals(2, first.attempts().size());
        assertEquals(1, first.achievements().size());
        assertFalse(sheet.floors().get(1).completed());

        assertEquals(2, sheet.purchases().size());
        assertEquals("#2 · Character Card A", sheet.purchases().getFirst().targetCard());
        assertEquals(1, sheet.purchases().getFirst().seat());

        assertEquals(15, sheet.totals().floorScoreTotal());
        assertEquals(1, sheet.totals().totalDefeats());
        assertEquals(9, sheet.totals().spentXp());
        assertEquals(6, sheet.totals().availableXp());
        assertEquals(13, sheet.totals().finalScore());
    }

    @Test
    void loadPublicReportsViewerWithoutMembershipCheck() {
        when(campaigns.findById(campaign.getId())).thenReturn(Optional.of(campaign));
        when(participants.findByCampaignIdOrderBySeat(campaign.getId())).thenReturn(List.of());
        when(attempts.findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(campaign.getId()))
                .thenReturn(List.of());
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of());
        when(floorProgress.findByCampaignIdOrderByFloorNumber(campaign.getId()))
                .thenReturn(List.of());
        when(purchases.findByCampaignIdOrderByPurchasedAtDesc(campaign.getId()))
                .thenReturn(List.of());

        CampaignSheet sheet = service.loadPublic(campaign.getId());

        assertEquals(AccessRole.VIEWER, sheet.role());
        assertFalse(sheet.canEdit());
        verify(access, never()).requireReadable(any(), any());
    }

    @Test
    void loadFallsBackWhenParticipantIsMissing() {
        Purchase orphan = new Purchase(campaign, p1, Benefit.ELIMINATE_STARTING, 5, null,
                null, null, null, null);
        when(participants.findByCampaignIdOrderBySeat(campaign.getId())).thenReturn(List.of());
        when(attempts.findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(campaign.getId()))
                .thenReturn(List.of());
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of());
        when(floorProgress.findByCampaignIdOrderByFloorNumber(campaign.getId()))
                .thenReturn(List.of());
        when(purchases.findByCampaignIdOrderByPurchasedAtDesc(campaign.getId()))
                .thenReturn(List.of(orphan));

        CampaignSheet sheet = service.load(campaign.getId(), "owner");

        assertEquals(0, sheet.purchases().getFirst().seat());
        assertEquals("", sheet.purchases().getFirst().crawler());
        assertEquals(null, sheet.purchases().getFirst().targetCard());
    }

    // ---- players and decks --------------------------------------------------

    @Test
    void updatePlayerValidatesAndUpdates() {
        when(participants.findByCampaignIdAndSeat(campaign.getId(), 1)).thenReturn(Optional.of(p1));

        service.updatePlayer(campaign.getId(), "owner", 1, " Carl ", " Human ",
                " Crawler ", 20, 10, 5, " A ", " B ");

        assertEquals("Carl", p1.getCrawler());
        assertEquals("Human", p1.getRace());
        assertEquals("Crawler", p1.getCrawlerClass());
        assertEquals(20, p1.getStartingHealth());
        assertEquals(10, p1.getStartingMana());
        assertEquals(5, p1.getStartingGold());
        assertEquals("A", p1.getSignatureLevel1());
        assertEquals("B", p1.getSignatureLevel2());
    }

    @Test
    void updatePlayerRejectsNegativeStatsAndMissingSeat() {
        when(participants.findByCampaignIdAndSeat(campaign.getId(), 1)).thenReturn(Optional.of(p1));

        assertThrows(IllegalArgumentException.class, () -> service.updatePlayer(
                campaign.getId(), "owner", 1, "Carl", "Human", "Crawler", -1, 0, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.updatePlayer(
                campaign.getId(), "owner", 2, "Carl", "Human", "Crawler", 0, -1, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.updatePlayer(
                campaign.getId(), "owner", 2, "Carl", "Human", "Crawler", 0, 0, -1, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.updatePlayer(
                campaign.getId(), "owner", 9, "Carl", "Human", "Crawler", 0, 0, 0, null, null));
    }

    @Test
    void updateDeckSlotValidatesCampaignMatch() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));

        service.updateDeckSlot(campaign.getId(), "owner", slot.getId(), "Strike +1",
                true, "Sharp", true, true, " notes ");

        assertEquals("Strike +1", slot.getCardName());
        assertTrue(slot.isEliminated());
        assertEquals("Sharp", slot.getUpgradeName());
        assertTrue(slot.isExtraStartingHand());
        assertTrue(slot.isMastered());
        assertEquals("notes", slot.getNotes());

        assertThrows(IllegalArgumentException.class, () -> service.updateDeckSlot(
                campaign.getId(), "owner", UUID.randomUUID(), "x", false, null, false, false, null));
    }

    @Test
    void updateDeckSlotRejectsSlotFromAnotherCampaign() {
        Campaign other = new Campaign("other", "Other", Difficulty.NORMAL);
        DeckEntry slot = new DeckEntry(new Participant(other, 1), 1, "Strike");
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));

        assertThrows(AccessDeniedException.class, () -> service.updateDeckSlot(
                campaign.getId(), "owner", slot.getId(), "x", false, null, false, false, null));
    }

    // ---- floors and attempts ------------------------------------------------

    @Test
    void updateFloorValidatesAndUpdates() {
        FloorProgress progress = new FloorProgress(campaign, 1);
        when(floorProgress.findByCampaignIdAndFloorNumber(campaign.getId(), 1))
                .thenReturn(Optional.of(progress));

        service.updateFloor(campaign.getId(), "owner", 1, 25, " notes ");
        assertEquals(25, progress.getBossQuestXp());
        assertEquals("notes", progress.getNotes());

        assertThrows(IllegalArgumentException.class,
                () -> service.updateFloor(campaign.getId(), "owner", 1, -1, null));
    }

    @Test
    void addAttemptDefaultsDateAndCompletesFloor() {
        FloorProgress progress = new FloorProgress(campaign, 1);
        when(floorProgress.findByCampaignIdAndFloorNumber(campaign.getId(), 1))
                .thenReturn(Optional.of(progress));
        when(attempts.findByCampaignIdAndFloorNumberOrderByPlayedAtDesc(campaign.getId(), 1))
                .thenReturn(List.of(new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 1),
                        true, null)));

        UUID id = service.addAttempt(campaign.getId(), "owner", 1, null, true, " notes ");

        assertNotNull(id);
        verify(attempts).save(any(FloorAttempt.class));
        assertNotNull(progress.getCompletedAt());
        assertThrows(IllegalArgumentException.class, () -> service.addAttempt(
                campaign.getId(), "owner", 0, null, true, null));
        assertThrows(IllegalArgumentException.class, () -> service.addAttempt(
                campaign.getId(), "owner", 10, null, true, null));
    }

    @Test
    void addAttemptReopensFloorWhenThereIsNoSuccess() {
        FloorProgress progress = new FloorProgress(campaign, 1);
        progress.complete(LocalDate.of(2026, 1, 1));
        when(floorProgress.findByCampaignIdAndFloorNumber(campaign.getId(), 1))
                .thenReturn(Optional.of(progress));
        when(attempts.findByCampaignIdAndFloorNumberOrderByPlayedAtDesc(campaign.getId(), 1))
                .thenReturn(List.of(new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 2),
                        false, null)));

        service.addAttempt(campaign.getId(), "owner", 1, LocalDate.of(2026, 1, 2), false, null);

        assertEquals(null, progress.getCompletedAt());
    }

    @Test
    void updateAttemptValidatesAndRefreshes() {
        FloorAttempt attempt = new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 1),
                false, null);
        FloorProgress progress = new FloorProgress(campaign, 1);
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(floorProgress.findByCampaignIdAndFloorNumber(campaign.getId(), 1))
                .thenReturn(Optional.of(progress));
        when(attempts.findByCampaignIdAndFloorNumberOrderByPlayedAtDesc(campaign.getId(), 1))
                .thenReturn(List.of(new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 5),
                        true, null)));

        service.updateAttempt(campaign.getId(), "owner", attempt.getId(), null, true, " won ");

        assertTrue(attempt.isSuccessful());
        assertEquals("won", attempt.getNotes());
        assertNotNull(progress.getCompletedAt());

        when(attempts.findById(UUID.randomUUID())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.updateAttempt(
                campaign.getId(), "owner", UUID.randomUUID(), null, true, null));
    }

    @Test
    void updateAttemptRejectsAttemptFromAnotherCampaign() {
        Campaign other = new Campaign("other", "Other", Difficulty.NORMAL);
        FloorAttempt attempt = new FloorAttempt(other, 1, LocalDate.now(), true, null);
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));

        assertThrows(AccessDeniedException.class, () -> service.updateAttempt(
                campaign.getId(), "owner", attempt.getId(), null, true, null));
    }

    @Test
    void deleteAttemptRemovesItAndRefreshesCompletion() {
        FloorAttempt attempt = new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 1),
                true, null);
        FloorProgress progress = new FloorProgress(campaign, 1);
        progress.complete(LocalDate.of(2026, 1, 1));
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(floorProgress.findByCampaignIdAndFloorNumber(campaign.getId(), 1))
                .thenReturn(Optional.of(progress));
        when(attempts.findByCampaignIdAndFloorNumberOrderByPlayedAtDesc(campaign.getId(), 1))
                .thenReturn(List.of());

        service.deleteAttempt(campaign.getId(), "owner", attempt.getId());

        verify(attempts).delete(attempt);
        verify(attempts).flush();
        assertEquals(null, progress.getCompletedAt());
    }

    @Test
    void deleteAttemptRejectsUnknownAttempt() {
        when(attempts.findById(any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.deleteAttempt(
                campaign.getId(), "owner", UUID.randomUUID()));
    }

    // ---- achievements -------------------------------------------------------

    @Test
    void addAchievementValidatesAndSaves() {
        when(achievements.findByCampaignIdAndNameIgnoreCase(campaign.getId(), "First"))
                .thenReturn(Optional.empty());

        UUID id = service.addAchievement(campaign.getId(), "owner", 1, " First ", 10, null);

        assertNotNull(id);
        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievements).save(captor.capture());
        assertEquals("First", captor.getValue().getName());
        assertEquals(10, captor.getValue().getXp());
        assertNotNull(captor.getValue().getCompletedAt());
    }

    @Test
    void addAchievementRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> service.addAchievement(
                campaign.getId(), "owner", 0, "First", 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.addAchievement(
                campaign.getId(), "owner", 1, " ", 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.addAchievement(
                campaign.getId(), "owner", 1, "First", -1, null));

        when(achievements.findByCampaignIdAndNameIgnoreCase(campaign.getId(), "first"))
                .thenReturn(Optional.of(new Achievement(campaign, 1, "First", 1, LocalDate.now())));
        assertThrows(IllegalArgumentException.class, () -> service.addAchievement(
                campaign.getId(), "owner", 1, "first", 1, null));
    }

    @Test
    void updateAchievementValidatesOwnershipAndDuplicates() {
        Achievement achievement = new Achievement(campaign, 1, "First", 10, LocalDate.now());
        when(achievements.findById(achievement.getId())).thenReturn(Optional.of(achievement));
        when(achievements.findByCampaignIdAndNameIgnoreCase(campaign.getId(), "Second"))
                .thenReturn(Optional.empty());

        service.updateAchievement(campaign.getId(), "owner", achievement.getId(), 2,
                " Second ", 20, null);

        assertEquals("Second", achievement.getName());
        assertEquals(2, achievement.getFloorNumber());
        assertEquals(20, achievement.getXp());
    }

    @Test
    void updateAchievementRejectsInvalidState() {
        Achievement achievement = new Achievement(campaign, 1, "First", 10, LocalDate.now());
        Achievement other = new Achievement(campaign, 1, "Other", 10, LocalDate.now());
        when(achievements.findById(achievement.getId())).thenReturn(Optional.of(achievement));
        when(achievements.findByCampaignIdAndNameIgnoreCase(campaign.getId(), "Other"))
                .thenReturn(Optional.of(other));

        assertThrows(IllegalArgumentException.class, () -> service.updateAchievement(
                campaign.getId(), "owner", achievement.getId(), 0, "First", 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.updateAchievement(
                campaign.getId(), "owner", achievement.getId(), 1, " ", 1, null));
        assertThrows(IllegalArgumentException.class, () -> service.updateAchievement(
                campaign.getId(), "owner", achievement.getId(), 1, "First", -1, null));
        assertThrows(IllegalArgumentException.class, () -> service.updateAchievement(
                campaign.getId(), "owner", achievement.getId(), 1, "Other", 1, null));
    }

    @Test
    void updateAchievementRejectsUnknownOrForeign() {
        when(achievements.findById(any())).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.updateAchievement(
                campaign.getId(), "owner", UUID.randomUUID(), 1, "First", 1, null));

        Campaign otherCampaign = new Campaign("other", "Other", Difficulty.NORMAL);
        Achievement foreign = new Achievement(otherCampaign, 1, "First", 1, LocalDate.now());
        when(achievements.findById(foreign.getId())).thenReturn(Optional.of(foreign));
        assertThrows(AccessDeniedException.class, () -> service.updateAchievement(
                campaign.getId(), "owner", foreign.getId(), 1, "First", 1, null));
    }

    @Test
    void deleteAchievementValidatesAndDeletes() {
        Achievement achievement = new Achievement(campaign, 1, "First", 10, LocalDate.now());
        when(achievements.findById(achievement.getId())).thenReturn(Optional.of(achievement));

        service.deleteAchievement(campaign.getId(), "owner", achievement.getId());
        verify(achievements).delete(achievement);

        assertThrows(IllegalArgumentException.class, () -> service.deleteAchievement(
                campaign.getId(), "owner", UUID.randomUUID()));
    }

    @Test
    void achievementNamesRequiresReadAccess() {
        when(achievements.findDistinctNamesForCampaign(campaign.getId()))
                .thenReturn(List.of("A", "B"));
        assertEquals(List.of("A", "B"),
                service.achievementNames(campaign.getId(), "owner"));
    }

    // ---- purchases ----------------------------------------------------------

    @Test
    void addFixedCostPurchaseMarksDeckSlot() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));
        emptyTotals();
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of(new Achievement(campaign, 1, "A", 20, LocalDate.now())));

        UUID id = service.addPurchase(campaign.getId(), "owner", p1.getId(),
                Benefit.ELIMINATE_STARTING, null, slot.getId(), null, null, " notes ");

        assertNotNull(id);
        assertTrue(slot.isEliminated());
        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).save(captor.capture());
        assertEquals(5, captor.getValue().getCost());
        assertEquals("notes", captor.getValue().getNotes());
    }

    @Test
    void addVariableCostPurchaseRequiresNameAndComputesCost() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));
        emptyTotals();
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of(new Achievement(campaign, 1, "A", 20, LocalDate.now())));

        service.addPurchase(campaign.getId(), "owner", p1.getId(),
                Benefit.KEEP_STRIKE_UPGRADE, 4, slot.getId(), null, " Sharp ", "notes");

        assertEquals("Sharp", slot.getUpgradeName());
        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).save(captor.capture());
        assertEquals(5, captor.getValue().getCost());
        assertEquals(4, captor.getValue().getXValue());
        assertEquals(campaign, captor.getValue().getCampaign());
    }

    @Test
    void addPurchaseRejectsBadInput() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));
        emptyTotals();

        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", UUID.randomUUID(), Benefit.ELIMINATE_STARTING,
                null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.MASTER_LEVEL1,
                null, slot.getId(), "Strike", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.KEEP_STRIKE_UPGRADE,
                null, slot.getId(), null, "Sharp", null));
        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.KEEP_STRIKE_UPGRADE,
                1, slot.getId(), null, " ", null));
        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.ELIMINATE_STARTING,
                null, null, null, null, null));
    }

    @Test
    void addPurchaseEnforcesPerPlayerLimits() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));
        emptyTotals();
        when(purchases.countByParticipantIdAndBenefit(p1.getId(), Benefit.ELIMINATE_STARTING))
                .thenReturn(1L);

        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.ELIMINATE_STARTING,
                null, slot.getId(), null, null, null));
    }

    @Test
    void addPurchaseRejectsWrongDeckSlotAndBenefitTargets() {
        Participant other = new Participant(campaign, 2);
        DeckEntry otherSlot = new DeckEntry(other, 1, "Strike");
        DeckEntry character = new DeckEntry(p1, 1, "Character Card A");
        DeckEntry mastered = new DeckEntry(p1, 2, "Rest");
        mastered.markMastered(true);
        DeckEntry nonStrike = new DeckEntry(p1, 3, "???");
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(otherSlot.getId())).thenReturn(Optional.of(otherSlot));
        when(deckEntries.findById(character.getId())).thenReturn(Optional.of(character));
        when(deckEntries.findById(mastered.getId())).thenReturn(Optional.of(mastered));
        when(deckEntries.findById(nonStrike.getId())).thenReturn(Optional.of(nonStrike));
        emptyTotals();
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of(new Achievement(campaign, 1, "A", 20, LocalDate.now())));

        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.ELIMINATE_STARTING,
                null, otherSlot.getId(), null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.KEEP_STRIKE_UPGRADE,
                1, nonStrike.getId(), null, "Sharp", null));
        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.KEEP_CHARACTER_UPGRADE,
                1, nonStrike.getId(), null, "Sharp", null));

        service.addPurchase(campaign.getId(), "owner", p1.getId(),
                Benefit.KEEP_CHARACTER_UPGRADE, 1, mastered.getId(), null, "Sharp", null);
        assertEquals("Sharp", mastered.getUpgradeName());
    }

    @Test
    void addPurchaseRequiresAffordableCost() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));
        emptyTotals();

        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.ELIMINATE_STARTING,
                null, slot.getId(), null, null, null));
    }

    @Test
    void addPurchaseDerivesTargetCardAndMarksMastered() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        slot.update("Strike +1", false, null, false, false, null);
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));
        emptyTotals();
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of(new Achievement(campaign, 1, "A", 20, LocalDate.now())));

        service.addPurchase(campaign.getId(), "owner", p1.getId(),
                Benefit.MASTER_LEVEL1, null, slot.getId(), null, null, null);

        assertTrue(slot.isMastered());
        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).save(captor.capture());
        assertEquals("Strike +1", captor.getValue().getTargetCard());
    }

    @Test
    void addPurchaseRequiresTargetCardForMasteringWithoutSlot() {
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        emptyTotals();

        assertThrows(IllegalArgumentException.class, () -> service.addPurchase(
                campaign.getId(), "owner", p1.getId(), Benefit.MASTER_LEVEL2,
                null, null, " ", null, null));
    }

    @Test
    void addExtraStartingHandMarksSlot() {
        DeckEntry slot = new DeckEntry(p1, 1, "Strike");
        when(participants.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(deckEntries.findById(slot.getId())).thenReturn(Optional.of(slot));
        emptyTotals();
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(campaign.getId()))
                .thenReturn(List.of(new Achievement(campaign, 1, "A", 20, LocalDate.now())));

        service.addPurchase(campaign.getId(), "owner", p1.getId(),
                Benefit.EXTRA_STARTING_HAND, null, slot.getId(), null, null, null);

        assertTrue(slot.isExtraStartingHand());
    }

    @Test
    void deletePurchaseValidatesAndDeletes() {
        Purchase purchase = new Purchase(campaign, p1, Benefit.ELIMINATE_STARTING, 5, null,
                null, null, null, null);
        when(purchases.findById(purchase.getId())).thenReturn(Optional.of(purchase));

        service.deletePurchase(campaign.getId(), "owner", purchase.getId());
        verify(purchases).delete(purchase);

        assertThrows(IllegalArgumentException.class, () -> service.deletePurchase(
                campaign.getId(), "owner", UUID.randomUUID()));
    }

    @Test
    void deletePurchaseRejectsForeignPurchase() {
        Campaign other = new Campaign("other", "Other", Difficulty.NORMAL);
        Purchase purchase = new Purchase(other, p1, Benefit.ELIMINATE_STARTING, 5, null,
                null, null, null, null);
        when(purchases.findById(purchase.getId())).thenReturn(Optional.of(purchase));

        assertThrows(AccessDeniedException.class, () -> service.deletePurchase(
                campaign.getId(), "owner", purchase.getId()));
    }

    @Test
    void missingCampaignIsReported() {
        when(campaigns.findById(any())).thenReturn(Optional.empty());
        when(access.requireEditable(any(), any())).thenThrow(new CampaignNotFoundException(
                campaign.getId()));
        assertThrows(CampaignNotFoundException.class, () -> service.addAttempt(
                campaign.getId(), "owner", 1, null, true, null));
    }

    @Test
    void accessDeniedPropagates() {
        when(access.requireEditable(any(), any()))
                .thenThrow(new AccessDeniedException("no"));
        assertThrows(AccessDeniedException.class, () -> service.updateFloor(
                campaign.getId(), "viewer", 1, 1, null));
    }

    @Test
    void loadSortsAttemptsNewestFirst() {
        FloorAttempt old = new FloorAttempt(campaign, 1, LocalDate.of(2026, 1, 1), true, null);
        FloorAttempt recent = new FloorAttempt(campaign, 1, LocalDate.of(2026, 2, 1), false, null);
        when(participants.findByCampaignIdOrderBySeat(any())).thenReturn(List.of());
        when(attempts.findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(any()))
                .thenReturn(new ArrayList<>(List.of(old, recent)));
        when(achievements.findByCampaignIdOrderByFloorNumberAscNameAsc(any()))
                .thenReturn(List.of());
        when(floorProgress.findByCampaignIdOrderByFloorNumber(any()))
                .thenReturn(List.of(new FloorProgress(campaign, 1)));
        when(purchases.findByCampaignIdOrderByPurchasedAtDesc(any())).thenReturn(List.of());

        CampaignSheet sheet = service.load(campaign.getId(), "owner");

        assertEquals(recent.getId(), sheet.floors().getFirst().attempts().getFirst().id());
        assertEquals(1, sheet.floors().getFirst().defeats());
    }
}
