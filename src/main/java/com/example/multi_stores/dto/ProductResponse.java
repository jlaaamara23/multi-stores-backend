package com.example.multi_stores.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long storeId;
    private Long categoryId;
    private List<String> imageUrls;
    private List<SizeStockDto> sizeStock;
    private List<ColorVariantDto> colorVariants;

    public ProductResponse() {}

    public ProductResponse(Long id, String name, String description, BigDecimal price, Integer stockQuantity, Long storeId, Long categoryId, List<String> imageUrls, List<SizeStockDto> sizeStock, List<ColorVariantDto> colorVariants) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.storeId = storeId;
        this.categoryId = categoryId;
        this.imageUrls = imageUrls;
        this.sizeStock = sizeStock;
        this.colorVariants = colorVariants;
    }

    public static ProductResponseBuilder builder() {
        return new ProductResponseBuilder();
    }

    public static final class ProductResponseBuilder {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stockQuantity;
        private Long storeId;
        private Long categoryId;
        private List<String> imageUrls;
        private List<SizeStockDto> sizeStock;
        private List<ColorVariantDto> colorVariants;

        private ProductResponseBuilder() {}

        public ProductResponseBuilder id(Long id) { this.id = id; return this; }
        public ProductResponseBuilder name(String name) { this.name = name; return this; }
        public ProductResponseBuilder description(String description) { this.description = description; return this; }
        public ProductResponseBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductResponseBuilder stockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; return this; }
        public ProductResponseBuilder storeId(Long storeId) { this.storeId = storeId; return this; }
        public ProductResponseBuilder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public ProductResponseBuilder imageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; return this; }
        public ProductResponseBuilder sizeStock(List<SizeStockDto> sizeStock) { this.sizeStock = sizeStock; return this; }
        public ProductResponseBuilder colorVariants(List<ColorVariantDto> colorVariants) { this.colorVariants = colorVariants; return this; }
        public ProductResponse build() {
            return new ProductResponse(id, name, description, price, stockQuantity, storeId, categoryId, imageUrls, sizeStock, colorVariants);
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public Long getStoreId() { return storeId; }
    public Long getCategoryId() { return categoryId; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<SizeStockDto> getSizeStock() { return sizeStock; }
    public void setSizeStock(List<SizeStockDto> sizeStock) { this.sizeStock = sizeStock; }
    public List<ColorVariantDto> getColorVariants() { return colorVariants; }
    public void setColorVariants(List<ColorVariantDto> colorVariants) { this.colorVariants = colorVariants; }
}
