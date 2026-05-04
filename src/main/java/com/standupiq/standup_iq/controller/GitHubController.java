package com.standupiq.standup_iq.controller;

import com.standupiq.standup_iq.dto.DebugTimeResponse;
import com.standupiq.standup_iq.dto.GitHubActivityResponse;
import com.standupiq.standup_iq.service.GitHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Tag(name = "GitHub Activity", description = "Fetches commits and pull requests from GitHub")
@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @Operation(
            summary = "Fetch GitHub activity",
            description = "Returns commits and pull requests for a developer. Optional owner, repo, and branch parameters limit the lookup to a specific repository."
    )
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

    @Operation(summary = "Preview time window", description = "Shows the UTC cutoff date used for a GitHub activity lookup.")
    @GetMapping("/debug/{username}")
    public DebugTimeResponse debug(@PathVariable String username, @RequestParam(defaultValue = "1") int days) {
        if (days < 1 || days > 30) {
            throw new IllegalArgumentException("days must be between 1 and 30");
        }

        Instant currentTime = Instant.now();
        String since = currentTime.minus(days, ChronoUnit.DAYS).toString();

        return new DebugTimeResponse(
                currentTime.toString(),
                days,
                since,
                username
        );
    }

}
