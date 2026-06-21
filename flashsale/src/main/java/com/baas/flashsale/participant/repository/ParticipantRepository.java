package com.baas.flashsale.participant.repository;

import com.baas.flashsale.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByTenantIdAndExternalCustomerId(Long tenantId, String externalCustomerId);
}
