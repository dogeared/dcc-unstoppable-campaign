package com.example.unstoppable.service;

import com.example.unstoppable.domain.AccessRole;

import java.time.Instant;
import java.util.UUID;

public record InviteView(
        UUID id,
        AccessRole role,
        boolean publicAccess,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        boolean usable) {
}
