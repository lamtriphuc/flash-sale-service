package com.lamtriphuc.backend.tenant.repository;

import com.lamtriphuc.backend.tenant.entity.TenantAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantAccountRepository extends JpaRepository<TenantAccount, UUID> {
    Optional<TenantAccount> findByEmail(String email);
    boolean existsByEmail(String email);
}
