package com.example.multi_stores.controller;

import com.example.multi_stores.dto.AuthResponse;
import com.example.multi_stores.dto.LoginRequest;
import com.example.multi_stores.entity.User;
import com.example.multi_stores.entity.UserRole;
import com.example.multi_stores.repository.UserRepository;
import com.example.multi_stores.security.JwtUtil;
import com.example.multi_stores.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final long VERIFICATION_EXPIRY_HOURS = 2;
    private static final ConcurrentHashMap<String, Long> resendLastSent = new ConcurrentHashMap<>();
    private static final long RESET_EXPIRY_MINUTES = 60;
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final ConcurrentHashMap<String, Long> resetLastSent = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         JwtUtil jwtUtil,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("email", user.getEmail());
        body.put("phone", user.getPhone() != null ? user.getPhone() : "");
        body.put("id", user.getId());
        body.put("role", user.getRole().name());
        body.put("emailVerified", user.isEmailVerified());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body(Map.of("error", "Please verify your email before logging in. Check your inbox for the verification link."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            boolean pendingExpired = !existing.isEmailVerified()
                    && existing.getVerificationTokenExpiry() != null
                    && existing.getVerificationTokenExpiry().isBefore(Instant.now());
            if (pendingExpired) {
                userRepository.delete(existing);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
            }
        }
        String phone = body.get("phone");
        if (phone != null) phone = phone.trim();
        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone is required"));
        }
        UserRole role = UserRole.CUSTOMER;
        User user = new User(null, email, passwordEncoder.encode(password), role, phone);
        String verificationToken = UUID.randomUUID().toString().replace("-", "");
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(VERIFICATION_EXPIRY_HOURS * 3600));
        boolean sent = emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        if (sent) {
            user.setEmailVerified(false);
            user = userRepository.save(user);
            resendLastSent.put(user.getEmail().toLowerCase(), System.currentTimeMillis());
            return ResponseEntity.status(201).body(Map.of(
                    "message", "Registration successful. Please check your email to verify your account.",
                    "email", user.getEmail()
            ));
        }
        // Brevo/email not configured or send failed: still create account so deploy/bootstrap can continue.
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user = userRepository.save(user);
        return ResponseEntity.status(201).body(Map.of(
                "message", "Registration successful. Verification email could not be sent, so the account was activated automatically.",
                "email", user.getEmail()
        ));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing token"));
        }
        User user = userRepository.findByVerificationToken(token).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired verification link"));
        }
        if (user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(Instant.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Verification link has expired"));
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        String emailLower = email.trim().toLowerCase();
        long now = System.currentTimeMillis();
        Long lastSent = resendLastSent.get(emailLower);
        if (lastSent != null && (now - lastSent) < RESEND_COOLDOWN_SECONDS * 1000L) {
            return ResponseEntity.status(429).body(Map.of("error", "Please wait 1 minute before requesting another verification email."));
        }
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No account found with this email."));
        }
        if (user.isEmailVerified()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already verified. You can log in."));
        }
        if (user.getVerificationToken() == null || user.getVerificationTokenExpiry() == null || user.getVerificationTokenExpiry().isBefore(Instant.now())) {
            String newToken = UUID.randomUUID().toString().replace("-", "");
            user.setVerificationToken(newToken);
            user.setVerificationTokenExpiry(Instant.now().plusSeconds(VERIFICATION_EXPIRY_HOURS * 3600));
            userRepository.save(user);
            final String userEmail = user.getEmail();
            final boolean sent = emailService.sendVerificationEmail(userEmail, newToken);
            if (!sent) {
                return ResponseEntity.status(503).body(Map.of("error", "Failed to send verification email. Please try again later."));
            }
        } else {
            final String userEmail = user.getEmail();
            final String verificationToken = user.getVerificationToken();
            final boolean sent = emailService.sendVerificationEmail(userEmail, verificationToken);
            if (!sent) {
                return ResponseEntity.status(503).body(Map.of("error", "Failed to send verification email. Please try again later."));
            }
        }
        resendLastSent.put(emailLower, now);
        return ResponseEntity.ok(Map.of("message", "Verification email sent. Please check your inbox."));
    }

    /**
     * Emails a one-hour password-reset link. Answers the same way whether or not the email exists.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        Map<String, String> ok = Map.of("message", "If an account exists for this email, a reset link was sent.");
        String emailLower = email.trim().toLowerCase();
        long now = System.currentTimeMillis();
        Long lastSent = resetLastSent.get(emailLower);
        if (lastSent != null && (now - lastSent) < RESEND_COOLDOWN_SECONDS * 1000L) {
            return ResponseEntity.ok(ok);
        }
        resetLastSent.put(emailLower, now);
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(ok);
        }
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        user.setResetTokenHash(sha256(token));
        user.setResetTokenExpiry(Instant.now().plusSeconds(RESET_EXPIRY_MINUTES * 60));
        userRepository.save(user);
        if (!emailService.sendPasswordResetEmail(user.getEmail(), token)) {
            return ResponseEntity.status(503).body(Map.of("error", "Failed to send the email. Please try again later."));
        }
        return ResponseEntity.ok(ok);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        String token = body != null ? body.get("token") : null;
        String password = body != null ? body.get("password") : null;
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired reset link"));
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least " + MIN_PASSWORD_LENGTH + " characters"));
        }
        User user = userRepository.findByResetTokenHash(sha256(token.trim())).orElse(null);
        if (user == null || user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired reset link"));
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setResetTokenHash(null);
        user.setResetTokenExpiry(null);
        // Opening the emailed link proves the address is theirs.
        user.setEmailVerified(true);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed. You can now log in."));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
