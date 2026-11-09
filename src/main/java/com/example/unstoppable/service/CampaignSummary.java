package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;
import com.example.unstoppable.domain.CampaignStatus;
import com.example.unstoppable.domain.Difficulty;

import java.time.Instant;
import java.util.UUID;

public record CampaignSummary(
        UUID id,
        String partyName,
        Difficulty difficulty,
        CampaignStatus status,
        AccessRole role,
        int floorsCompleted,
        int finalScore,
        int availableXp,
        Instant updatedAt) {
}
