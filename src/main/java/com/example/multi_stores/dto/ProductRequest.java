package com.example.multi_stores.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stockQuantity;

    @NotNull
    private Long storeId;

    private Long categoryId;

    private List<String> imageUrls;

    /** Optional: for clothing etc. Each entry is size + quantity (e.g. S=5, M=10). */
    private List<SizeStockDto> sizeStocks;

    /** Optional: same product in different colors, each with its own images. */
    private List<ColorVariantDto> colorVariants;

    public ProductRequest() {}

    public ProductRequest(String name, String description, BigDecimal price, Integer stockQuantity, Long storeId, Long categoryId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.storeId = storeId;
        this.categoryId = categoryId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<SizeStockDto> getSizeStocks() { return sizeStocks; }
    public void setSizeStocks(List<SizeStockDto> sizeStocks) { this.sizeStocks = sizeStocks; }
    public List<ColorVariantDto> getColorVariants() { return colorVariants; }
    public void setColorVariants(List<ColorVariantDto> colorVariants) { this.colorVariants = colorVariants; }
}
