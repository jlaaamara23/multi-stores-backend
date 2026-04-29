package com.example.multi_stores.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", insertable = false, updatable = false)
    private Store store;

    public Order() {}

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static final class OrderBuilder {
        private Long customerId;
        private Long storeId;
        private BigDecimal totalPrice;
        private OrderStatus status;
        private Instant createdAt;

        private OrderBuilder() {}

        public OrderBuilder customerId(Long customerId) {
            this.customerId = customerId;
            return this;
        }
        public OrderBuilder storeId(Long storeId) {
            this.storeId = storeId;
            return this;
        }
        public OrderBuilder totalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
            return this;
        }
        public OrderBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }
        public OrderBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public Order build() {
            Order order = new Order();
            order.setCustomerId(customerId);
            order.setStoreId(storeId);
            order.setTotalPrice(totalPrice);
            order.setStatus(status);
            order.setCreatedAt(createdAt);
            return order;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
}
