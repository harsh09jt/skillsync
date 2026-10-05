package com.skillsync.backend.service;

import com.skillsync.backend.dto.LoginRequest;
import com.skillsync.backend.dto.LoginResponse;
import com.skillsync.backend.dto.RegisterRequest;
import com.skillsync.backend.entity.Role;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.UserRepository;
import com.skillsync.backend.security.JwtService;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // =========================
    // LOGIN
    // =========================

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        String storedPassword = user.getPassword();
        boolean passwordMatches;
        if (storedPassword.startsWith("$2")) {
            passwordMatches = passwordEncoder.matches(
                    request.getPassword(),
                    storedPassword
            );
        } else {
            passwordMatches = request.getPassword().equals(storedPassword);
            if (passwordMatches) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                userRepository.save(user);
            }
        }

        if (!passwordMatches) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }


    // =========================
    // REGISTER
    // =========================

    public User register(RegisterRequest request) {

        // Check whether email already exists
        if (userRepository.findByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        // Create new user
        User user = new User();

        user.setName(request.getName());

        user.setEmail(request.getEmail().trim().toLowerCase());

        // Hash password before saving
        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Convert String role to Enum
        user.setRole(Role.EMPLOYEE);

        // Save user in database
        return userRepository.save(user);
    }
}