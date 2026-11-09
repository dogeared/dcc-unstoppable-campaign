package com.example.unstoppable.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign")
public class Campaign {

    @Id
    private UUID id;

    @Column(name = "owner_sub", nullable = false, length = 128)
    private String ownerSub;

    @Column(name = "party_name", nullable = false, length = 120)
    private String partyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CampaignStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Campaign() {
        // for JPA
    }

    public Campaign(String ownerSub, String partyName, Difficulty difficulty) {
        this.id = UUID.randomUUID();
        this.ownerSub = ownerSub;
        this.partyName = partyName;
        this.difficulty = difficulty;
        this.status = CampaignStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void updateParty(String partyName, Difficulty difficulty) {
        this.partyName = partyName;
        this.difficulty = difficulty;
        touch();
    }

    public void complete() {
        this.status = CampaignStatus.COMPLETED;
        touch();
    }

    public void reopen() {
        this.status = CampaignStatus.ACTIVE;
        touch();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerSub() {
        return ownerSub;
    }

    public String getPartyName() {
        return partyName;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
