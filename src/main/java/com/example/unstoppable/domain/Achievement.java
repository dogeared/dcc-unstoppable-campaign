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
@Table(name = "achievement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "name"}))
public class Achievement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private int xp;

    @Column(name = "completed_at", nullable = false)
    private LocalDate completedAt;

    protected Achievement() {
        // for JPA
    }

    public Achievement(Campaign campaign, int floorNumber, String name, int xp,
            LocalDate completedAt) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.floorNumber = floorNumber;
        this.name = name;
        this.xp = xp;
        this.completedAt = completedAt;
    }

    public void update(int floorNumber, String name, int xp, LocalDate completedAt) {
        this.floorNumber = floorNumber;
        this.name = name;
        this.xp = xp;
        this.completedAt = completedAt;
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

    public String getName() {
        return name;
    }

    public int getXp() {
        return xp;
    }

    public LocalDate getCompletedAt() {
        return completedAt;
    }
}
