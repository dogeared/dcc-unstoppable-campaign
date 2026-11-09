package com.example.unstoppable.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "floor_attempt")
public class FloorAttempt {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "floor_number", nullable = false)
    private int floorNumber;

    @Column(name = "played_at", nullable = false)
    private LocalDate playedAt;

    @Column(nullable = false)
    private boolean successful;

    @Column(length = 600)
    private String notes;

    protected FloorAttempt() {
        // for JPA
    }

    public FloorAttempt(Campaign campaign, int floorNumber, LocalDate playedAt,
            boolean successful, String notes) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.floorNumber = floorNumber;
        this.playedAt = playedAt;
        this.successful = successful;
        this.notes = notes;
    }

    public void update(LocalDate playedAt, boolean successful, String notes) {
        this.playedAt = playedAt;
        this.successful = successful;
        this.notes = notes;
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

    public LocalDate getPlayedAt() {
        return playedAt;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getNotes() {
        return notes;
    }
}
