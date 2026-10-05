package com.skillsync.backend.dto;

public class LoginResponse {

    private String token;
    private Long userId;
    private String name;
    private String email;
    private String role;

    public LoginResponse(
            String token,
            Long userId,
            String name,
            String email,
            String role
    ) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}