package com.example.multi_stores.controller;

import com.example.multi_stores.entity.Category;
import com.example.multi_stores.entity.Product;
import com.example.multi_stores.entity.User;
import com.example.multi_stores.entity.UserRole;
import com.example.multi_stores.repository.CategoryRepository;
import com.example.multi_stores.repository.ProductRepository;
import com.example.multi_stores.repository.StoreRepository;
import com.example.multi_stores.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Sub-categories inside a store (e.g. "evening dresses" inside Clothing).
 * Reading is public; creating and assigning products is admin only.
 * Assigning only changes products' category_id — nothing else on the product.
 */
@RestController
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public CategoryController(CategoryRepository categoryRepository, ProductRepository productRepository,
                              StoreRepository storeRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/stores/id/{storeId}/categories")
    public List<Map<String, Object>> list(@PathVariable Long storeId) {
        return categoryRepository.findByStoreId(storeId).stream()
                .sorted(Comparator.comparing(Category::getId))
                .map(c -> Map.<String, Object>of("id", c.getId(), "name", c.getName(), "storeId", c.getStoreId()))
                .toList();
    }

    public record CreateCategoryRequest(String name) {}

    @PostMapping("/api/admin/stores/{storeId}/categories")
    public ResponseEntity<?> create(@PathVariable Long storeId, @RequestBody CreateCategoryRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin only"));
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 100) return ResponseEntity.badRequest().body(Map.of("error", "Invalid name"));
        if (!storeRepository.existsById(storeId)) return ResponseEntity.badRequest().body(Map.of("error", "Store not found"));
        // Idempotent: return the existing category with the same name.
        Category existing = categoryRepository.findByStoreId(storeId).stream()
                .filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(Map.of("id", existing.getId(), "name", existing.getName(), "storeId", storeId, "created", false));
        }
        Category saved = categoryRepository.save(Category.builder().name(name).storeId(storeId).build());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "name", saved.getName(), "storeId", storeId, "created", true));
    }

    public record AssignRequest(Long categoryId, List<Long> productIds) {}

    @PostMapping("/api/admin/categories/assign")
    @Transactional
    public ResponseEntity<?> assign(@RequestBody AssignRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin only"));
        if (request == null || request.categoryId() == null || request.productIds() == null || request.productIds().isEmpty()
                || request.productIds().size() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "categoryId and 1-500 productIds are required"));
        }
        Category category = categoryRepository.findById(request.categoryId()).orElse(null);
        if (category == null) return ResponseEntity.badRequest().body(Map.of("error", "Category not found"));
        List<Product> products = productRepository.findByIdIn(request.productIds());
        if (products.size() != request.productIds().stream().distinct().count()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Some products were not found"));
        }
        // All-or-nothing: refuse if any product belongs to a different store.
        for (Product p : products) {
            if (!category.getStoreId().equals(p.getStoreId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Product " + p.getId() + " is not in this category's store"));
            }
        }
        products.forEach(p -> p.setCategoryId(category.getId()));
        productRepository.saveAll(products);
        return ResponseEntity.ok(Map.of("categoryId", category.getId(), "updated", products.size()));
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) return false;
        User user = userDetails instanceof User u ? u : userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}
