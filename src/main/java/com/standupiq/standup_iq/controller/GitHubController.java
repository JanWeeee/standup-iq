package com.standupiq.standup_iq.controller;

import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    @Autowired
    private GitHubService gitHubService;

    @GetMapping("/activity/{username}")
    public ResponseEntity<GitHubActivityResponse> getActivity(
            @PathVariable String username,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "1") int days
    ) {
        return ResponseEntity.ok(gitHubService.getActivity(username, owner, repo, branch, days));
    }

    @GetMapping("/debug/{username}")
    public Map<String, Object> debug(@PathVariable String username, @RequestParam(defaultValue = "1") int days) {
        if (days < 1 || days > 30) {
            throw new IllegalArgumentException("days must be between 1 and 30");
        }

        Instant currentTime = Instant.now();
        String since = currentTime.minus(days, ChronoUnit.DAYS).toString();

        return Map.of(
                "sinceDate", since,
                "currentTimeUtc", currentTime.toString(),
                "days", days,
                "username", username
        );
    }

}
