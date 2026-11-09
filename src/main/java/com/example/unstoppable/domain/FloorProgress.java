package com.example.unstoppable.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "floor_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "floor_number"}))
public class FloorProgress {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(name = "boss_quest_xp", nullable = false)
    private int bossQuestXp;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Column(length = 600)
    private String notes;

    protected FloorProgress() {
        // for JPA
    }

    public FloorProgress(Campaign campaign, int floorNumber) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.floorNumber = floorNumber;
    }

    public void updateBossQuestXp(int bossQuestXp, String notes) {
        this.bossQuestXp = bossQuestXp;
        this.notes = notes;
    }

    public void complete(LocalDate date) {
        this.completedAt = date;
    }

    public void reopen() {
        this.completedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public int getBossQuestXp() {
        return bossQuestXp;
    }

    public LocalDate getCompletedAt() {
        return completedAt;
    }

    public String getNotes() {
        return notes;
    }
}
