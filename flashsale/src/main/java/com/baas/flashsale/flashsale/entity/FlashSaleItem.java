package com.baas.flashsale.flashsale.entity;

import com.baas.flashsale.campaign.entity.Campaign;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "flash_sale_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(nullable = false)
    private String itemCode;

    @Column(nullable = false)
    private String itemName;

    private Long originalPrice;

    private Long salePrice;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    private Integer maxPerParticipant = 1;

    private Boolean active = true;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        if (this.active == null) {
            this.active = true;
        }
        if (this.remainingQuantity == null) {
            this.remainingQuantity = this.totalQuantity;
        }
        if (this.maxPerParticipant == null) {
            this.maxPerParticipant = 1;
        }
    }
}
