package com.example.multi_stores.controller;

import com.example.multi_stores.dto.ProductRequest;
import com.example.multi_stores.dto.ProductResponse;
import com.example.multi_stores.entity.ProductStatus;
import com.example.multi_stores.entity.User;
import com.example.multi_stores.entity.UserRole;
import com.example.multi_stores.repository.UserRepository;
import com.example.multi_stores.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    /** Public list. Drafts are included only for admins. */
    @GetMapping("/api/stores/{storeId}/products")
    public List<ProductResponse> getByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return productService.findByStoreIdAndCategory(storeId, categoryId, isAdmin(userDetails));
    }

    @PostMapping("/api/products")
    public ResponseEntity<?> create(@Valid @RequestBody ProductRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(productService.getById(id, isAdmin(userDetails)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/products/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return changeStatus(id, ProductStatus.PUBLISHED, userDetails);
    }

    @PostMapping("/api/products/{id}/unpublish")
    public ResponseEntity<?> unpublish(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return changeStatus(id, ProductStatus.DRAFT, userDetails);
    }

    /** Body: {"ids": [1, 2, 3]}. Publishes those with a price; returns {published: [...], failed: [{id, error}]}. */
    @PostMapping("/api/products/publish")
    public ResponseEntity<?> publishMany(@RequestBody Map<String, List<Long>> body, @AuthenticationPrincipal UserDetails userDetails) {
        if (!isAdmin(userDetails)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(productService.publishAll(body.get("ids")));
    }

    private ResponseEntity<?> changeStatus(Long id, ProductStatus status, UserDetails userDetails) {
        if (!isAdmin(userDetails)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        try {
            return ResponseEntity.ok(productService.setStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isAdmin(UserDetails userDetails) {
        User user = getAuthUser(userDetails);
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    @PutMapping("/api/products/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(productService.updateProduct(id, request, user.getId(), user.getRole()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && (e.getMessage().startsWith("Price") || e.getMessage().startsWith("Invalid status"))) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/api/products/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            productService.deleteProduct(id, user.getId(), user.getRole());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    private User getAuthUser(UserDetails userDetails) {
        if (userDetails == null) return null;
        if (userDetails instanceof User) return (User) userDetails;
        String email = userDetails.getUsername();
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmail(email).orElse(null);
    }
}
