package com.lamtriphuc.backend.user.repository;

import com.lamtriphuc.backend.user.entity.EndUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EndUserRepository extends JpaRepository<EndUser, UUID> {
    Optional<EndUser> findByTenantIdAndIdentifierHash(UUID tenantId, String identifierHash);
}