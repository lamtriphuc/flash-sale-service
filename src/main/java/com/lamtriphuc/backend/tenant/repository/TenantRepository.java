package com.lamtriphuc.backend.tenant.repository;

import com.lamtriphuc.backend.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
