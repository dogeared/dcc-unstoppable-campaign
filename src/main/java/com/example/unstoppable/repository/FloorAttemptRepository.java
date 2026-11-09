package com.example.unstoppable.repository;

import com.example.unstoppable.domain.FloorAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FloorAttemptRepository extends JpaRepository<FloorAttempt, UUID> {

    List<FloorAttempt> findByCampaignIdOrderByPlayedAtDescFloorNumberAsc(UUID campaignId);

    List<FloorAttempt> findByCampaignIdAndFloorNumberOrderByPlayedAtDesc(
            UUID campaignId, int floorNumber);

    long countByCampaignIdAndFloorNumberAndSuccessfulFalse(UUID campaignId, int floorNumber);
}
