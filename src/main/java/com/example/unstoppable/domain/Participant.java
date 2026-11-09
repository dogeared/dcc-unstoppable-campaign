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
@Table(name = "participant",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "seat"}))
public class Participant {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private int seat;

    @Column(length = 120)
    private String crawler;

    @Column(length = 80)
    private String race;

    @Column(name = "class_name", length = 80)
    private String crawlerClass;

    @Column(name = "starting_health", nullable = false)
    private int startingHealth;

    @Column(name = "starting_mana", nullable = false)
    private int startingMana;

    @Column(name = "starting_gold", nullable = false)
    private int startingGold;

    @Column(name = "signature_level1", length = 120)
    private String signatureLevel1;

    @Column(name = "signature_level2", length = 120)
    private String signatureLevel2;

    protected Participant() {
        // for JPA
    }

    public Participant(Campaign campaign, int seat) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.seat = seat;
    }

    public void update(String crawler, String race, String crawlerClass,
            int startingHealth, int startingMana, int startingGold,
            String signatureLevel1, String signatureLevel2) {
        this.crawler = crawler;
        this.race = race;
        this.crawlerClass = crawlerClass;
        this.startingHealth = startingHealth;
        this.startingMana = startingMana;
        this.startingGold = startingGold;
        this.signatureLevel1 = signatureLevel1;
        this.signatureLevel2 = signatureLevel2;
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public int getSeat() {
        return seat;
    }

    public String getCrawler() {
        return crawler;
    }

    public String getRace() {
        return race;
    }

    public String getCrawlerClass() {
        return crawlerClass;
    }

    public int getStartingHealth() {
        return startingHealth;
    }

    public int getStartingMana() {
        return startingMana;
    }

    public int getStartingGold() {
        return startingGold;
    }

    public String getSignatureLevel1() {
        return signatureLevel1;
    }

    public String getSignatureLevel2() {
        return signatureLevel2;
    }
}
