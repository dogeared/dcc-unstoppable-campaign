package com.example.unstoppable.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "deck_entry",
        uniqueConstraints = @UniqueConstraint(columnNames = {"participant_id", "position"}))
public class DeckEntry {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false)
    private int position;

    @Column(name = "base_card", nullable = false, length = 80)
    private String baseCard;

    @Column(name = "card_name", length = 120)
    private String cardName;

    @Column(nullable = false)
    private boolean eliminated;

    @Column(name = "upgrade_name", length = 160)
    private String upgradeName;

    @Column(name = "extra_starting_hand", nullable = false)
    private boolean extraStartingHand;

    @Column(nullable = false)
    private boolean mastered;

    @Column(length = 600)
    private String notes;

    protected DeckEntry() {
        // for JPA
    }

    public DeckEntry(Participant participant, int position, String baseCard) {
        this.id = UUID.randomUUID();
        this.participant = participant;
        this.position = position;
        this.baseCard = baseCard;
        this.cardName = baseCard;
    }

    public void update(String cardName, boolean eliminated, String upgradeName,
            boolean extraStartingHand, boolean mastered, String notes) {
        this.cardName = cardName;
        this.eliminated = eliminated;
        this.upgradeName = upgradeName;
        this.extraStartingHand = extraStartingHand;
        this.mastered = mastered;
        this.notes = notes;
    }

    public void markEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    public void markMastered(boolean mastered) {
        this.mastered = mastered;
    }

    public void markExtraStartingHand(boolean extraStartingHand) {
        this.extraStartingHand = extraStartingHand;
    }

    public void setUpgradeName(String upgradeName) {
        this.upgradeName = upgradeName;
    }

    public UUID getId() {
        return id;
    }

    public Participant getParticipant() {
        return participant;
    }

    public int getPosition() {
        return position;
    }

    public String getBaseCard() {
        return baseCard;
    }

    public String getCardName() {
        return cardName;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public String getUpgradeName() {
        return upgradeName;
    }

    public boolean isExtraStartingHand() {
        return extraStartingHand;
    }

    public boolean isMastered() {
        return mastered;
    }

    public String getNotes() {
        return notes;
    }
}
