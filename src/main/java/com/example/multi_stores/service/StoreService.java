package com.example.multi_stores.service;

import com.example.multi_stores.dto.StoreRequest;
import com.example.multi_stores.dto.StoreResponse;
import com.example.multi_stores.entity.Store;
import com.example.multi_stores.entity.UserRole;
import com.example.multi_stores.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<StoreResponse> findAll() {
        return storeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public StoreResponse findBySlug(String slug) {
        Store store = storeRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + slug));
        return toResponse(store);
    }

    public StoreResponse findById(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + id));
        return toResponse(store);
    }

    @Transactional
    public StoreResponse createStore(StoreRequest request, Long ownerId) {
        if (storeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Store name already exists: " + request.getName());
        }
        String slug = generateSlug(request.getName());
        if (storeRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        String iconUrl = request.getIconUrl();
        if (iconUrl != null) iconUrl = iconUrl.trim();
        if (iconUrl != null && iconUrl.isEmpty()) iconUrl = null;
        Store store = Store.builder()
                .name(request.getName())
                .slug(slug)
                .category(request.getCategory())
                .iconUrl(iconUrl)
                .ownerId(ownerId)
                .build();
        store = storeRepository.save(store);
        return toResponse(store);
    }

    public boolean isStoreNameAvailable(String name) {
        return !storeRepository.existsByName(name);
    }

    @Transactional
    public StoreResponse updateStore(Long storeId, StoreRequest request, Long userId, UserRole userRole) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (userRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only admin can update this store");
        }
        store.setName(request.getName());
        store.setCategory(request.getCategory());
        String iconUrl = request.getIconUrl();
        if (iconUrl != null) iconUrl = iconUrl.trim();
        store.setIconUrl(iconUrl != null && !iconUrl.isEmpty() ? iconUrl : null);
        store = storeRepository.save(store);
        return toResponse(store);
    }

    @Transactional
    public void deleteStore(Long storeId, Long userId, UserRole userRole) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (userRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only admin can delete this store");
        }
        storeRepository.delete(store);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private StoreResponse toResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .slug(store.getSlug())
                .category(store.getCategory())
                .iconUrl(store.getIconUrl())
                .ownerId(store.getOwnerId())
                .build();
    }
}
