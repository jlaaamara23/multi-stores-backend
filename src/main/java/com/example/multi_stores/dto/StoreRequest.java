package com.example.multi_stores.dto;

import com.example.multi_stores.entity.StoreCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StoreRequest {

    @NotBlank(message = "Store name is required")
    private String name;

    @NotNull
    private StoreCategory category;

    private String iconUrl;

    public StoreRequest() {}

    public StoreRequest(String name, StoreCategory category) {
        this.name = name;
        this.category = category;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public StoreCategory getCategory() { return category; }
    public void setCategory(StoreCategory category) { this.category = category; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
}
