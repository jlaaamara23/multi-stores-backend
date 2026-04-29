package com.example.multi_stores.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_size_stock", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "size" }))
public class ProductSizeStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 20)
    private String size;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    public ProductSizeStock() {}

    public ProductSizeStock(Long id, Long productId, String size, Integer quantity, Product product) {
        this.id = id;
        this.productId = productId;
        this.size = size;
        this.quantity = quantity;
        this.product = product;
    }

    public static ProductSizeStockBuilder builder() {
        return new ProductSizeStockBuilder();
    }

    public static final class ProductSizeStockBuilder {
        private Long productId;
        private String size;
        private Integer quantity;

        private ProductSizeStockBuilder() {}

        public ProductSizeStockBuilder productId(Long productId) { this.productId = productId; return this; }
        public ProductSizeStockBuilder size(String size) { this.size = size; return this; }
        public ProductSizeStockBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public ProductSizeStock build() {
            ProductSizeStock e = new ProductSizeStock();
            e.setProductId(productId);
            e.setSize(size);
            e.setQuantity(quantity);
            return e;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
