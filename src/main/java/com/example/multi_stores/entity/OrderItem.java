package com.example.multi_stores.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(length = 20)
    private String size;

    @Column(length = 50)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    public OrderItem() {}

    public static OrderItemBuilder builder() {
        return new OrderItemBuilder();
    }

    public static final class OrderItemBuilder {
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String size;
        private String color;

        private OrderItemBuilder() {}

        public OrderItemBuilder productId(Long productId) {
            this.productId = productId;
            return this;
        }
        public OrderItemBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }
        public OrderItemBuilder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }
        public OrderItemBuilder size(String size) {
            this.size = size;
            return this;
        }
        public OrderItemBuilder color(String color) {
            this.color = color;
            return this;
        }
        public OrderItem build() {
            OrderItem oi = new OrderItem();
            oi.setProductId(productId);
            oi.setQuantity(quantity);
            oi.setUnitPrice(unitPrice);
            oi.setSize(size);
            oi.setColor(color);
            return oi;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
