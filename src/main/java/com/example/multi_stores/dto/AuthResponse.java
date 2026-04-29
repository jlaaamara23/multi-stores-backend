package com.example.multi_stores.dto;

import lombok.*;

@Data
public class AuthResponse {

    private String token;
    private String email;
    private String phone;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String token, String email, String role) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.phone = null;
    }

    public AuthResponse(String token, String email, String phone, String role) {
        this.token = token;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }

    public static AuthResponseBuilder builder() { return new AuthResponseBuilder(); }

    public static final class AuthResponseBuilder {
        private String token;
        private String email;
        private String phone;
        private String role;

        private AuthResponseBuilder() {}

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder email(String email) { this.email = email; return this; }
        public AuthResponseBuilder phone(String phone) { this.phone = phone; return this; }
        public AuthResponseBuilder role(String role) { this.role = role; return this; }
        public AuthResponse build() { return new AuthResponse(token, email, phone, role); }
    }
}
