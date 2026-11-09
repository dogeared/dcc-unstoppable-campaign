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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_membership",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campaign_id", "subject"}))
public class CampaignMembership {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false, length = 128)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccessRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CampaignMembership() {
        // for JPA
    }

    public CampaignMembership(Campaign campaign, String subject, AccessRole role) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.subject = subject;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public void elevate(AccessRole role) {
        if (role == AccessRole.EDITOR && this.role == AccessRole.VIEWER) {
            this.role = AccessRole.EDITOR;
        }
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getSubject() {
        return subject;
    }

    public AccessRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
