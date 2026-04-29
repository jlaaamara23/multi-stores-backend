package com.example.multi_stores.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_color_variant", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "color" }))
public class ProductColorVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String color;

    @Column(name = "image_urls", length = 2000)
    private String imageUrls;

    @Column(nullable = false)
    private Integer quantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    public ProductColorVariant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getImageUrls() { return imageUrls; }
    public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}
