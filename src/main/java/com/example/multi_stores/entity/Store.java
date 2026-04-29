package com.example.multi_stores.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stores")
@Getter
@Setter
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoreCategory category;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    public Store() {}

    public static StoreBuilder builder() {
        return new StoreBuilder();
    }

    public static final class StoreBuilder {
        private String name;
        private String slug;
        private StoreCategory category;
        private String iconUrl;
        private Long ownerId;

        private StoreBuilder() {}

        public StoreBuilder name(String name) { this.name = name; return this; }
        public StoreBuilder slug(String slug) { this.slug = slug; return this; }
        public StoreBuilder category(StoreCategory category) { this.category = category; return this; }
        public StoreBuilder iconUrl(String iconUrl) { this.iconUrl = iconUrl; return this; }
        public StoreBuilder ownerId(Long ownerId) { this.ownerId = ownerId; return this; }
        public Store build() {
            Store s = new Store();
            s.setName(name);
            s.setSlug(slug);
            s.setCategory(category);
            s.setIconUrl(iconUrl);
            s.setOwnerId(ownerId);
            return s;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public StoreCategory getCategory() { return category; }
    public void setCategory(StoreCategory category) { this.category = category; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
