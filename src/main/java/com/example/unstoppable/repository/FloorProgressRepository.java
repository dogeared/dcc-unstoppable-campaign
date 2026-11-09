package com.example.unstoppable.repository;

import com.example.unstoppable.domain.FloorProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FloorProgressRepository extends JpaRepository<FloorProgress, UUID> {

    List<FloorProgress> findByCampaignIdOrderByFloorNumber(UUID campaignId);

    Optional<FloorProgress> findByCampaignIdAndFloorNumber(UUID campaignId, int floorNumber);
}
