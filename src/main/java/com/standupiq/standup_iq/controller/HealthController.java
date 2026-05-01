package com.standupiq.standup_iq.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.standupiq.standup_iq.entity.User;
import com.standupiq.standup_iq.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users/test")
    public String createTestUser() {
        User user = new User();
        user.setEmail("janhavi@test.com");
        user.setName("Janhavi");
        user.setGithubToken("test-token-123");

        userRepository.save(user);
        return "User saved successfully! Check your database.";
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "app", "StandupIQ",
                "message", "Welcome to StandupIQ - Your AI Standup Generator"
        );
    }
}