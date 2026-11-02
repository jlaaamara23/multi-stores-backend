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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Categories inside a store (department), two levels deep:
 * a top-level category (e.g. "Dresses") with sub-categories (e.g. "Evening dresses").
 * Reading is public; creating, moving and assigning products is admin only.
 * Assigning only changes products' category_id; moving a category to another store also changes its products' store_id.
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
                .map(CategoryController::toMap)
                .toList();
    }

    public record CreateCategoryRequest(String name, Long parentId) {}

    @PostMapping("/api/admin/stores/{storeId}/categories")
    public ResponseEntity<?> create(@PathVariable Long storeId, @RequestBody CreateCategoryRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin only"));
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 100) return ResponseEntity.badRequest().body(Map.of("error", "Invalid name"));
        if (!storeRepository.existsById(storeId)) return ResponseEntity.badRequest().body(Map.of("error", "Store not found"));
        Long parentId = request.parentId();
        if (parentId != null && !isValidParent(parentId, storeId, null)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Parent must be a top-level category in the same store"));
        }
        // Idempotent: return the existing category with the same name.
        Category existing = categoryRepository.findByStoreId(storeId).stream()
                .filter(c -> c.getName().equals(name)).findFirst().orElse(null);
        if (existing != null) {
            Map<String, Object> body = toMap(existing);
            body.put("created", false);
            return ResponseEntity.ok(body);
        }
        Category saved = categoryRepository.save(Category.builder().name(name).storeId(storeId).parentId(parentId).build());
        Map<String, Object> body = toMap(saved);
        body.put("created", true);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public record MoveCategoryRequest(Long storeId, Long parentId) {}

    /**
     * Places a category under a parent (or top level) and optionally in another store.
     * When the store changes, the category's products move with it (only their store_id changes).
     */
    @PutMapping("/api/admin/categories/{id}")
    @Transactional
    public ResponseEntity<?> move(@PathVariable Long id, @RequestBody MoveCategoryRequest request,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin only"));
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) return ResponseEntity.notFound().build();
        Long storeId = request == null || request.storeId() == null ? category.getStoreId() : request.storeId();
        Long parentId = request == null ? null : request.parentId();
        if (!storeRepository.existsById(storeId)) return ResponseEntity.badRequest().body(Map.of("error", "Store not found"));
        boolean hasChildren = categoryRepository.findByStoreId(category.getStoreId()).stream()
                .anyMatch(c -> id.equals(c.getParentId()));
        if (hasChildren && (parentId != null || !storeId.equals(category.getStoreId()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "A category with sub-categories must stay top level in its store"));
        }
        if (parentId != null && !isValidParent(parentId, storeId, id)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Parent must be a top-level category in the target store"));
        }
        int movedProducts = 0;
        if (!storeId.equals(category.getStoreId())) {
            List<Product> products = productRepository.findByStoreIdAndCategoryId(category.getStoreId(), id);
            for (Product p : products) p.setStoreId(storeId);
            productRepository.saveAll(products);
            movedProducts = products.size();
        }
        category.setStoreId(storeId);
        category.setParentId(parentId);
        categoryRepository.save(category);
        Map<String, Object> body = toMap(category);
        body.put("movedProducts", movedProducts);
        return ResponseEntity.ok(body);
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

    private boolean isValidParent(Long parentId, Long storeId, Long selfId) {
        if (parentId.equals(selfId)) return false;
        Category parent = categoryRepository.findById(parentId).orElse(null);
        return parent != null && storeId.equals(parent.getStoreId()) && parent.getParentId() == null;
    }

    private static Map<String, Object> toMap(Category c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("storeId", c.getStoreId());
        m.put("parentId", c.getParentId());
        return m;
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) return false;
        User user = userDetails instanceof User u ? u : userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}
