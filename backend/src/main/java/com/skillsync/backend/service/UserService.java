package com.skillsync.backend.service;

import com.skillsync.backend.dto.UserRequest;
import com.skillsync.backend.dto.UserResponse;
import com.skillsync.backend.dto.ProfileUpdateRequest;
import com.skillsync.backend.entity.User;
import com.skillsync.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    // ==========================================
    // GET ALL USERS
    // ==========================================

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }


    // ==========================================
    // GET USER BY ID
    // ==========================================

    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        return convertToResponse(user);
    }

    public UserResponse updateUser(Long id, ProfileUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        String normalizedEmail = request.email().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Email already registered"
                );
            }
        });
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setJobTitle(request.jobTitle());
        user.setDepartment(request.department());
        user.setCareerGoal(request.careerGoal());
        user.setBio(request.bio());
        return convertToResponse(userRepository.save(user));
    }


    // ==========================================
    // CREATE USER
    // ==========================================

    public UserResponse createUser(UserRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // IMPORTANT:
        // Password is hashed before saving
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        return convertToResponse(savedUser);
    }


    // ==========================================
    // DELETE USER
    // ==========================================

    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {

            throw new RuntimeException(
                    "User not found"
            );
        }

        userRepository.deleteById(id);
    }


    // ==========================================
    // ENTITY → RESPONSE DTO
    // ==========================================

    private UserResponse convertToResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getJobTitle(),
                user.getDepartment(),
                user.getCareerGoal(),
                user.getBio()
        );
    }

    public UserResponse changeRole(Long id, com.skillsync.backend.entity.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User not found"
                ));
        user.setRole(role);
        return convertToResponse(userRepository.save(user));
    }
}