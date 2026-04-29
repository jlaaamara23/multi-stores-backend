package com.example.multi_stores.repository;

import com.example.multi_stores.entity.Order;
import com.example.multi_stores.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStoreId(Long storeId);

    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);
}
