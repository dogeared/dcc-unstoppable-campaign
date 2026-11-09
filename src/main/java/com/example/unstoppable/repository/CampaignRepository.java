package com.example.unstoppable.repository;

import com.example.unstoppable.domain.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByOwnerSubOrderByUpdatedAtDesc(String ownerSub);
}
