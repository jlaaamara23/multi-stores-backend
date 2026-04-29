package com.example.multi_stores.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(length = 20)
    private String phone;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token", length = 64)
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private Instant verificationTokenExpiry;

    /** No-arg constructor required by JPA (when Lombok does not generate @NoArgsConstructor) */
    public User() {}

    /** Explicit constructor for when Lombok does not generate @AllArgsConstructor */
    public User(Long id, String email, String password, UserRole role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = null;
    }

    public User(Long id, String email, String password, UserRole role, String phone) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.phone = phone;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    /** Admins and owners can log in without email verification; others must be verified. */
    public boolean isEnabled() {
        return emailVerified || role == UserRole.ADMIN || role == UserRole.OWNER;
    }

    @Override
    public String getPassword() {
        return password;
    }

    // Explicit getters/setters for verification and compatibility
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    /** True if explicitly verified or legacy user (no token set). */
    public boolean isEmailVerified() { return emailVerified || verificationToken == null; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public Instant getVerificationTokenExpiry() { return verificationTokenExpiry; }
    public void setVerificationTokenExpiry(Instant verificationTokenExpiry) { this.verificationTokenExpiry = verificationTokenExpiry; }
}
