package com.example.multi_stores.dto;

import java.util.List;

public class ColorVariantDto {

    private String color;
    private List<String> imageUrls;
    private Integer quantity;

    public ColorVariantDto() {}

    public ColorVariantDto(String color, List<String> imageUrls, Integer quantity) {
        this.color = color;
        this.imageUrls = imageUrls;
        this.quantity = quantity;
    }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
