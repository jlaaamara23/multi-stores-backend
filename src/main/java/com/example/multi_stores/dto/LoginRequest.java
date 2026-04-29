package com.example.multi_stores.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

    // Explicit getters for compatibility when Lombok annotation processing is disabled
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
