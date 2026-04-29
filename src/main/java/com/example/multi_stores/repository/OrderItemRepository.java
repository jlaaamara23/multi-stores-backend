package com.example.multi_stores.repository;

import com.example.multi_stores.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    boolean existsByProductId(Long productId);

    void deleteByOrderId(Long orderId);
}
