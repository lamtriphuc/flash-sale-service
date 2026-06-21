package com.baas.flashsale.campaign.entity;

import com.baas.flashsale.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "campaigns",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tenant_id", "code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String code; // FS_12_12

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CampaignStatus.ACTIVE;
        }
    }
}
