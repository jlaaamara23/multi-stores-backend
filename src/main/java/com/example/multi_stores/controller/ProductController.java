package com.example.multi_stores.controller;

import com.example.multi_stores.dto.ProductRequest;
import com.example.multi_stores.dto.ProductResponse;
import com.example.multi_stores.entity.User;
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

    @GetMapping("/api/stores/{storeId}/products")
    public List<ProductResponse> getByStore(
            @PathVariable Long storeId,
            @RequestParam(required = false) Long categoryId
    ) {
        return productService.findByStoreIdAndCategory(storeId, categoryId);
    }

    @PostMapping("/api/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/api/products/{id}")
    public ResponseEntity<ProductResponse> update(
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
