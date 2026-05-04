package com.standupiq.standup_iq.controller;

import com.standupiq.standup_iq.dto.HealthResponse;
import com.standupiq.standup_iq.entity.User;
import com.standupiq.standup_iq.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Health", description = "Application health and local database smoke checks")
@RestController
@RequestMapping("/api")
public class HealthController {

    private final UserRepository userRepository;

    public HealthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Create a sample user", description = "Development-only endpoint that verifies PostgreSQL writes.")
    @GetMapping("/users/test")
    public String createTestUser() {
        User user = new User();
        user.setEmail("janhavi@test.com");
        user.setName("Janhavi");
        user.setGithubToken("test-token-123");

        userRepository.save(user);
        return "User saved successfully! Check your database.";
    }

    @Operation(summary = "Check application health", description = "Returns a lightweight JSON health response.")
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                "StandupIQ",
                "Welcome to StandupIQ - Your AI Standup Generator"
        );
    }
}
