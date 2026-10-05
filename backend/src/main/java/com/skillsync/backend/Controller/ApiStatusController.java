package com.skillsync.backend.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiStatusController {

    @GetMapping({"/", "/health"})
    public Map<String, String> status() {
        return Map.of(
                "service", "SkillSync API",
                "status", "UP"
        );
    }
}
