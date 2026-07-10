package com.lamtriphuc.backend.order.repository;

import com.lamtriphuc.backend.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    
}
