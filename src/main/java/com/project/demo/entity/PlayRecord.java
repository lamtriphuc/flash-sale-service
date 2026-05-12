package com.project.demo.entity;

import com.project.demo.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "play_records",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "campaign_id", "idempotency_key"})
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private Long prizeId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey; // Mã UUID từ Frontend gửi lên

    @Enumerated(EnumType.STRING)
    private RecordStatus status; // PROCESSING, SUCCESS, FAILED

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
