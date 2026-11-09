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
@Table(name = "campaign_invite")
public class CampaignInvite {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccessRole role;

    @Column(name = "public_access", nullable = false)
    private boolean publicAccess;

    @Column(name = "created_by_sub", nullable = false, length = 128)
    private String createdBySub;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected CampaignInvite() {
        // for JPA
    }

    public CampaignInvite(Campaign campaign, String tokenHash, AccessRole role,
            boolean publicAccess, String createdBySub, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.campaign = campaign;
        this.tokenHash = tokenHash;
        this.createdBySub = createdBySub;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        apply(role, publicAccess);
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    public void update(AccessRole role, boolean publicAccess) {
        apply(role, publicAccess);
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    private void apply(AccessRole role, boolean publicAccess) {
        if (publicAccess && role == AccessRole.EDITOR) {
            throw new IllegalArgumentException("Editor links must be private");
        }
        this.role = role;
        this.publicAccess = publicAccess;
    }

    public UUID getId() {
        return id;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public AccessRole getRole() {
        return role;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public String getCreatedBySub() {
        return createdBySub;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
