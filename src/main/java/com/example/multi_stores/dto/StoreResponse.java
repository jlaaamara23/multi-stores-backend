package com.example.multi_stores.dto;

import com.example.multi_stores.entity.StoreCategory;

public class StoreResponse {

    private Long id;
    private String name;
    private String slug;
    private StoreCategory category;
    private String iconUrl;
    private Long ownerId;

    public StoreResponse() {}

    public StoreResponse(Long id, String name, String slug, StoreCategory category, String iconUrl, Long ownerId) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.category = category;
        this.iconUrl = iconUrl;
        this.ownerId = ownerId;
    }

    public static StoreResponseBuilder builder() {
        return new StoreResponseBuilder();
    }

    public static final class StoreResponseBuilder {
        private Long id;
        private String name;
        private String slug;
        private StoreCategory category;
        private String iconUrl;
        private Long ownerId;

        private StoreResponseBuilder() {}

        public StoreResponseBuilder id(Long id) { this.id = id; return this; }
        public StoreResponseBuilder name(String name) { this.name = name; return this; }
        public StoreResponseBuilder slug(String slug) { this.slug = slug; return this; }
        public StoreResponseBuilder category(StoreCategory category) { this.category = category; return this; }
        public StoreResponseBuilder iconUrl(String iconUrl) { this.iconUrl = iconUrl; return this; }
        public StoreResponseBuilder ownerId(Long ownerId) { this.ownerId = ownerId; return this; }
        public StoreResponse build() {
            return new StoreResponse(id, name, slug, category, iconUrl, ownerId);
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public StoreCategory getCategory() { return category; }
    public String getIconUrl() { return iconUrl; }
    public Long getOwnerId() { return ownerId; }
}
