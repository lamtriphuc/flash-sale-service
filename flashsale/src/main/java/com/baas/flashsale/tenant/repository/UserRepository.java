package com.baas.flashsale.tenant.repository;

import com.baas.flashsale.tenant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTenantIdAndUsername(Long tenantId, String username);

    Optional<User> findByTenantCodeAndUsername(String tenantCode, String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByTenantIdAndUsername(Long tenantId, String username);

    List<User> findByTenantId(Long tenantId);
}
