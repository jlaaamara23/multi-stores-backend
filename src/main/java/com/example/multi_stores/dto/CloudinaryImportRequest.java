package com.example.multi_stores.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CloudinaryImportRequest {

    @NotNull
    private Long storeId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal defaultPrice;

    private Integer defaultStock = 10;

    /** Cloudinary folder to import from (default: multi-stores). */
    private String folder = "multi-stores";

    /** Safety limit (default 2000). */
    private Integer maxImages = 2000;

    private String namePrefix = "Product";

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public BigDecimal getDefaultPrice() { return defaultPrice; }
    public void setDefaultPrice(BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }

    public Integer getDefaultStock() { return defaultStock; }
    public void setDefaultStock(Integer defaultStock) { this.defaultStock = defaultStock; }

    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }

    public Integer getMaxImages() { return maxImages; }
    public void setMaxImages(Integer maxImages) { this.maxImages = maxImages; }

    public String getNamePrefix() { return namePrefix; }
    public void setNamePrefix(String namePrefix) { this.namePrefix = namePrefix; }
}
