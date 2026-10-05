package com.skillsync.backend.dto;

import com.skillsync.backend.entity.Role;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String jobTitle;
    private String department;
    private String careerGoal;
    private String bio;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String name,
            String email,
            Role role,
            String jobTitle,
            String department,
            String careerGoal,
            String bio
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.jobTitle = jobTitle;
        this.department = department;
        this.careerGoal = careerGoal;
        this.bio = bio;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public String getCareerGoal() {
        return careerGoal;
    }

    public String getBio() {
        return bio;
    }
}