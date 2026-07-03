package com.baas.flashsale.flashsale.repository;

import com.baas.flashsale.flashsale.entity.Order;
import com.baas.flashsale.flashsale.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByCampaignIdAndParticipantIdAndStatus(Long campaignId, Long participantId, OrderStatus status);

    List<Order> findByCampaignIdAndParticipantExternalCustomerId(Long campaignId, String externalCustomerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findWithLockById(@Param("id") Long id);

    List<Order> findByStatusAndPaymentExpiresAtBefore(OrderStatus status, LocalDateTime paymentExpiresAt);
}
