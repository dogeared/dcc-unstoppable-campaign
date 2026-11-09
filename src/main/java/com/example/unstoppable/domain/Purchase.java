package com.example.unstoppable.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase")
public class Purchase {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private Benefit benefit;

    @Column(nullable = false)
    private int cost;

    @Column(name = "x_value")
    private Integer xValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_deck_entry_id")
    private DeckEntry targetDeckEntry;

    @Column(name = "target_card", length = 160)
    private String targetCard;

    @Column(name = "upgrade_name", length = 160)
    private String upgradeName;

    @Column(length = 600)
    private String notes;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;

    protected Purchase() {
        // for JPA
    }

    public Purchase(Campaign campaign, Participant participant, Benefit benefit,
            int cost, Integer xValue, DeckEntry targetDeckEntry, String targetCard,
            String upgradeName, String notes) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.participant = participant;
        this.benefit = benefit;
        this.cost = cost;
        this.xValue = xValue;
        this.targetDeckEntry = targetDeckEntry;
        this.targetCard = targetCard;
        this.upgradeName = upgradeName;
        this.notes = notes;
        this.purchasedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public Participant getParticipant() {
        return participant;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public int getCost() {
        return cost;
    }

    public Integer getXValue() {
        return xValue;
    }

    public DeckEntry getTargetDeckEntry() {
        return targetDeckEntry;
    }

    public String getTargetCard() {
        return targetCard;
    }

    public String getUpgradeName() {
        return upgradeName;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }
}
