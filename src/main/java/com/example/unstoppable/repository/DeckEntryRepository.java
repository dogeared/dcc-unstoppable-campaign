package com.example.unstoppable.repository;

import com.example.unstoppable.domain.DeckEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeckEntryRepository extends JpaRepository<DeckEntry, UUID> {

    List<DeckEntry> findByParticipantIdOrderByPosition(UUID participantId);
}
