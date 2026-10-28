package com.example.multi_stores.controller;

import com.example.multi_stores.dto.CloudinaryImportRequest;
import com.example.multi_stores.entity.User;
import com.example.multi_stores.entity.UserRole;
import com.example.multi_stores.repository.UserRepository;
import com.example.multi_stores.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminImportController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public AdminImportController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @PostMapping("/import-cloudinary")
    public ResponseEntity<?> importCloudinary(
            @Valid @RequestBody CloudinaryImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthUser(userDetails);
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin only"));
        }
        try {
            return ResponseEntity.ok(productService.importFromCloudinary(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Cloudinary import failed: " + e.getMessage()));
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
