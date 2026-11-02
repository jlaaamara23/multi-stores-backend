package com.example.multi_stores.entity;

/**
 * Visibility of a product. DRAFT products are only visible to admins and cannot be ordered.
 * Rows created before this field existed have status NULL and are treated as PUBLISHED.
 */
public enum ProductStatus {
    DRAFT,
    PUBLISHED;

    public static boolean isPublished(ProductStatus status) {
        return status == null || status == PUBLISHED;
    }
}
