package com.project.demo.entity;

import com.project.demo.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Data // Của Lombok: Tự sinh getter, setter, toString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Tên chiến dịch (VD: Flash Sale 11/11)

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    //  DRAFT, ACTIVE, ENDED
    @Enumerated(EnumType.STRING)
    private CampaignStatus status;
}