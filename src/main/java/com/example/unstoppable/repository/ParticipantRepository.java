package com.example.unstoppable.repository;

import com.example.unstoppable.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    List<Participant> findByCampaignIdOrderBySeat(UUID campaignId);

    Optional<Participant> findByCampaignIdAndSeat(UUID campaignId, int seat);
}
