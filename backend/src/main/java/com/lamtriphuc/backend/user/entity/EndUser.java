package com.lamtriphuc.backend.user.entity;

import com.lamtriphuc.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "end_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndUser extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "identifier_hash", nullable = false)
    private String identifierHash;

    @Column(name = "encrypted_profile", nullable = false, columnDefinition = "TEXT")
    private String encryptedProfile;

    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "is_blocked")
    private Boolean isBlocked;
}