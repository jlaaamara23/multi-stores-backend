package com.example.multi_stores.controller;

import com.example.multi_stores.dto.StoreRequest;
import com.example.multi_stores.dto.StoreResponse;
import com.example.multi_stores.entity.User;
import com.example.multi_stores.repository.UserRepository;
import com.example.multi_stores.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserRepository userRepository;

    public StoreController(StoreService storeService, UserRepository userRepository) {
        this.storeService = storeService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<StoreResponse> listStores() {
        return storeService.findAll();
    }

    @GetMapping("/{slug}")
    public ResponseEntity<StoreResponse> getBySlug(@PathVariable String slug) {
        try {
            return ResponseEntity.ok(storeService.findBySlug(slug));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<StoreResponse> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(storeService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<StoreResponse> create(
            @Valid @RequestBody StoreRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long ownerId = getUserId(userDetails);
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request, ownerId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/check-name")
    public ResponseEntity<Boolean> isNameAvailable(@RequestParam String name) {
        return ResponseEntity.ok(storeService.isStoreNameAvailable(name));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StoreRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(storeService.updateStore(id, request, user.getId(), user.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            storeService.deleteStore(id, user.getId(), user.getRole());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private Long getUserId(UserDetails userDetails) {
        User user = getAuthUser(userDetails);
        return user != null ? user.getId() : null;
    }

    private User getAuthUser(UserDetails userDetails) {
        if (userDetails == null) return null;
        if (userDetails instanceof User) return (User) userDetails;
        String email = userDetails.getUsername();
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmail(email).orElse(null);
    }
}
