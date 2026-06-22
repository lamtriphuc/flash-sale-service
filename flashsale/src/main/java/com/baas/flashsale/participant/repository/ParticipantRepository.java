package com.baas.flashsale.participant.repository;

import com.baas.flashsale.participant.entity.Participant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    Optional<Participant> findByTenantIdAndExternalCustomerId(Long tenantId, String externalCustomerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participant p where p.tenant.id = :tenantId and p.externalCustomerId = :externalCustomerId")
    Optional<Participant> findByTenantIdAndExternalCustomerIdForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("externalCustomerId") String externalCustomerId
    );

    @Modifying
    @Query(value = """
            insert into participants (tenant_id, external_customer_id, created_at)
            values (:tenantId, :externalCustomerId, now())
            on conflict (tenant_id, external_customer_id) do nothing
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("tenantId") Long tenantId,
            @Param("externalCustomerId") String externalCustomerId
    );
}
