package com.standupiq.standup_iq.controller;

import com.standupiq.standup_iq.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    @Autowired
    private GitHubService gitHubService;

    @GetMapping("/activity/{username}")
    public Map<String, Object> getActivity(
            @PathVariable String username,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) String branch
    ) {
        boolean hasRepoScope = hasText(owner) && hasText(repo);
        List<Map> commits = hasRepoScope
                ? gitHubService.getUserCommits(owner, repo, branch, username)
                : gitHubService.getUserCommits(username);
        List<Map> prs = hasRepoScope
                ? gitHubService.getUserPullRequests(owner, repo, username)
                : gitHubService.getUserPullRequests(username);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("username", username);
        response.put("totalCommits", commits.size());
        response.put("totalPRs", prs.size());
        response.put("commits", commits.stream().limit(5).toList());
        response.put("pullRequests", prs.stream().limit(5).toList());
        response.put("message", "GitHub activity fetched successfully");

        if (hasRepoScope) {
            response.put("owner", owner);
            response.put("repo", repo);
            response.put("branch", hasText(branch) ? branch : "default");
        }

        return response;
    }

    @GetMapping("/debug/{username}")
    public Map<String, Object> debug(@PathVariable String username) {
        Instant currentTime = Instant.now();
        String since = currentTime.minus(72, ChronoUnit.HOURS).toString();

        return Map.of(
                "sinceDate", since,
                "currentTimeUtc", currentTime.toString(),
                "username", username
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
