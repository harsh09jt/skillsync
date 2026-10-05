package com.skillsync.backend.Controller;

import com.skillsync.backend.dto.LoginRequest;
import com.skillsync.backend.dto.LoginResponse;
import com.skillsync.backend.dto.RegisterRequest;
import com.skillsync.backend.dto.UserResponse;
import com.skillsync.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        var user = authService.register(request);
        return ResponseEntity.status(201).body(new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getJobTitle(),
                user.getDepartment(),
                user.getCareerGoal(),
                user.getBio()
        ));
    }
}